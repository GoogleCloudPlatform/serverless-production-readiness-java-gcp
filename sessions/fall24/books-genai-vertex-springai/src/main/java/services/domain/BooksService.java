/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import services.ai.VertexAIClient;
import services.domain.dao.DataAccess;
import services.domain.util.ScopeType;
import services.utility.FileUtility;
import services.utility.PromptUtility;
import services.utility.SqlUtility;
import services.web.data.BookRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BooksService {
    private static final Logger logger = LoggerFactory.getLogger(BooksService.class);

    final DataAccess dao;

    final VertexAIClient vertexAIClient;

    final CloudStorageService cloudStorageService;

    @Value("${prompts.promptSubSummary}")
    private String promptSubSummary;


    @Value("${prompts.promptSummary}")
    private String promptSummary;

    @Value("${workflows.summary.chunk.characters}")
    private Integer summaryChunkCharacters;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    String model;

    public BooksService(DataAccess dao, VertexAIClient vertexAIClient, CloudStorageService cloudStorageService) {
        this.dao = dao;
        this.vertexAIClient = vertexAIClient;
        this.cloudStorageService = cloudStorageService;
    }

    @Deprecated
    public List<Map<String, Object>>  prompt(String prompt) {
        return dao.promptForBooks(prompt, 0);
    }

    @Deprecated
    public List<Map<String, Object>>  prompt(String prompt, Integer characterLimit) {
        return dao.promptForBooks(prompt, characterLimit);
    }

    public List<Map<String, Object>>  prompt(BookRequest bookRequest, Integer characterLimit) {
        String prompt = PromptUtility.formatPromptBookKeywords(bookRequest.keyWords());
        return dao.promptForBooks(prompt, bookRequest.book(), bookRequest.author(), characterLimit);
    }

    // Insert book data in the database
    // books, authors and vector embeddings
    public String insertBookAndAuthorData(String bucketName, String fileName, boolean overwriteIfBookExists){
        String message;
        // insert book data in the book data
        // insert author info in the authors table
        Integer bookId = insertBookAndAuthorData(fileName);

        // read the document from Cloud Storage
        List<Document> bookContent = cloudStorageService.readFileAsDocument(bucketName, fileName);

        // split the document using a TokenTextSplitter
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(5000, 100, 5, 100000, true);
        List<Document> chunks = tokenTextSplitter.apply(bookContent);

        logger.info("Splitting document with Metadata: {}", bookContent.getFirst().getMetadata().toString());
        logger.info("Chunks size: {}", chunks.size());

        // insert the book data with vector embeddings
        insertPagesBook(bookId, chunks);

        return "Success";
    }

    // Persist data into Books and Authors tables
    public Integer insertBookAndAuthorData(String fileName) {
        String author = FileUtility.getAuthor(fileName);
        author = SqlUtility.replaceUnderscoresWithSpaces(author);
        String title = FileUtility.getTitle(fileName);
        title = SqlUtility.replaceUnderscoresWithSpaces(title);
        String year = FileUtility.getYear(fileName);
        String publicPrivate = FileUtility.getPublicPrivate(fileName);

        // lookup existing info for books and authors
        Map<String, Object> book = dao.findBook(title);
        Map<String, Object> authorMap = dao.findAuthor(author);
        Object authorId = authorMap.get("author_id");
        Integer bookId = 0;
        if(!book.isEmpty()){
            bookId = (Integer) book.get("book_id");
        } else {
            if(authorId==null)
                authorId = dao.insertAuthor("famous author", author);
            logger.info("publicPrivate:{}", publicPrivate);
            bookId = dao.insertBook( (Integer) authorId, title, year, ScopeType.fromValue(publicPrivate));
        }

        return bookId;
    }

    // Create book summary and persist in the database
    public String createBookSummary(String bucketName, String fileName, boolean overwriteIfSummaryExists) {
        String summary = "";

        // read the book content from Cloud Storage
        String bookText = cloudStorageService.readFileAsString(bucketName, fileName);

        // extract the book title
        String bookTitle = FileUtility.getTitle(fileName);
        bookTitle = SqlUtility.replaceUnderscoresWithSpaces(bookTitle);

        // lookup book summary in the database
        summary = getBookSummary(bookTitle);
        if (!summary.isEmpty() && !overwriteIfSummaryExists)
            return summary;

        // find the book in the book table
        // extract the book id
        Map<String, Object> book = dao.findBook(bookTitle);
        Integer bookId = (Integer) book.get("book_id");

        // create a SystemMessage
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate("""
            You are a helpful AI assistant.
            You are an AI assistant that helps people summarize information.
            Your name is {name}
            You should reply to the user's request with your name and also in the style of a {voice}.
            Strictly ignore Project Gutenberg & ignore copyright notice in summary output.
            """
        );
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("name", "Gemini", "voice", "literary critic"));

        // create a UserMessage
        PromptTemplate userPromptTemplate = new PromptTemplate("""
            "Please provide a concise summary covering the key points of the following text.
                              TEXT: {content}
                              SUMMARY:
            """, Map.of("content", bookText));
        Message userMessage = userPromptTemplate.createMessage();

        summary = vertexAIClient.promptModel(systemMessage, userMessage, model);

        logger.info("The summary for book {} is: {}", bookTitle, summary);
        logger.info("The prompt summary: {}", promptSummary.formatted(summary));

        // insert summary in table
        dao.insertSummaries(bookId, summary);

        return summary;
    }

    // Retrieve book summary from booksummaries table, if it exists
    public String getBookSummary(String bookTitle) {
        // find the book in the database by table name
        Map<String, Object> book = dao.findBook(bookTitle);

        Map<String, Object> summary = new HashMap<>();
        if(!book.isEmpty()){
            Integer bookId = (Integer) book.get("book_id");
            summary = dao.findSummaries(bookId);
        }
        return summary.isEmpty() ? "" : (String) summary.get("summary");
    }

    // Persist book pages in the database, with vector embeddings
    public boolean insertPagesBook(Integer bookId, List<Document> chunks) {
        Integer page = 1;
        List<Map<String, Object>> pages = dao.findPages(bookId);
        if (!pages.isEmpty()) {
            return true;
        }

        for (Document chunk : chunks) {
            dao.insertPages(bookId, chunk.getContent(), page++);
        }

        return true;
    }
}
