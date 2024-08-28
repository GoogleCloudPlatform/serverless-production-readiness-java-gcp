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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import services.ai.VertexAIClient;
import services.domain.dao.DataAccess;
import services.domain.util.ScopeType;
import services.utility.FileUtility;
import services.utility.PromptUtility;
import services.utility.SqlUtility;
import services.web.data.BookRequest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BooksService {
    @Autowired
    DataAccess dao;

    @Autowired
    VertexAIClient vertexAIClient;

    @Value("${prompts.promptSubSummary}")
    private String promptSubSummary;


    @Value("${prompts.promptSummary}")
    private String promptSummary;

    @Value("${workflows.summary.chunk.characters}")
    private Integer summaryChunkCharacters;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    String model;

    private static final Logger logger = LoggerFactory.getLogger(BooksService.class);

    public List<Map<String, Object>>  prompt(String prompt) {
        return dao.promptForBooks(prompt, 0);
    }

    public List<Map<String, Object>>  prompt(String prompt, Integer characterLimit) {
        return dao.promptForBooks(prompt, characterLimit);
    }

    public List<Map<String, Object>>  prompt(BookRequest bookRequest, Integer characterLimit) {
        String prompt = PromptUtility.formatPromptBookKeywords(bookRequest.keyWords());
        return dao.promptForBooks(prompt, bookRequest.book(), bookRequest.author(), characterLimit);
    }

    public Integer insertBook(String fileName) {
        String author = FileUtility.getAuthor(fileName);
        author = SqlUtility.replaceUnderscoresWithSpaces(author);
        String title = FileUtility.getTitle(fileName);
        title = SqlUtility.replaceUnderscoresWithSpaces(title);
        String year = FileUtility.getYear(fileName);
        String publicPrivate = FileUtility.getPublicPrivate(fileName);
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

    public String createBookSummary(BufferedReader reader, String fileName) {
        String summary = "";
        try {
            String bookTitle = FileUtility.getTitle(fileName);
            bookTitle = SqlUtility.replaceUnderscoresWithSpaces(bookTitle);
            summary = getBookSummary(bookTitle);
            if (!summary.isEmpty()) {
                return summary;
            }
            summary = "";
            Map<String, Object> book = dao.findBook(bookTitle);
            Integer bookId = (Integer) book.get("book_id");
            String content="";
            Integer page = 1;
            char[] cbuf = new char[summaryChunkCharacters];
            int charsRead;
            String context = "";
            logger.info("The prompt build summary: " +promptSubSummary.formatted(context, content));
            while ((charsRead = reader.read(cbuf)) != -1) {
                content = new String(cbuf, 0, charsRead);
                try {
                    context = vertexAIClient.promptModel(promptSubSummary.formatted(context, content), model);
                } catch (io.grpc.StatusRuntimeException statusRuntimeException) {
                    logger.warn("vertexAIClient.promptModel(promptSubSummary.formatted(context, content)) statusRuntimeException: {}", statusRuntimeException.getMessage());
                    continue;
                } catch (RuntimeException e) {
                    logger.warn("Failed to interact with Vertex AI model: "+e.getMessage(), e);
                    continue;
                }
                summary += "\n"+context;
                if(page%10==0)
                    logger.info("The prompt build summary: " +summary);
                page++;
            }
            reader.close();
            logger.info("The book {} has pages: {}", bookTitle, page);
            logger.info("The summary for book {} is: {}", bookTitle, summary);
            logger.info("The prompt summary: {}", promptSummary.formatted(summary));

            summary = vertexAIClient.promptModel(promptSummary.formatted(summary), model);

            // insert summary in table
            dao.insertSummaries(bookId, summary);
        } catch (FileNotFoundException e) {
            logger.error("File {} not found. Failed with exception {}", fileName, e.getMessage());
        } catch (IOException e) {
            logger.error("Reading from file %s failure. Failed with exception {}", fileName, e.getMessage());
        }
        return summary;
    }

    public String createBookSummary(String bucketName, String fileName, boolean overwriteIfSummaryExists) {
        String summary = "";
        // Create a Storage client.
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // Get the blob.
        Blob blob = storage.get(BlobId.of(bucketName, fileName));

        // read the book content
        String bookText = new String(blob.getContent(), StandardCharsets.UTF_8);

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

    public Integer insertPagesBook(String filePath, String bookTitle) {
        //0 = failure, 1 = success
        Integer success = 0;
        if( filePath == null || filePath.equals("") || bookTitle==null || bookTitle.equals("")) {
            return success;
        }

        Map<String, Object> book = dao.findBook(bookTitle);

        if(book.isEmpty()){
            return success;
        }

        BufferedReader reader = null;
        Integer bookId = (Integer) book.get("book_id");
        logger.info(filePath+" "+bookTitle+" bookId:"+bookId);
        try {
            //replace with cloud storage eventually
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            InputStream inputStream = classPathResource.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            success = insertPagesBook(reader, bookTitle);
        }catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return success;
    }

    public Integer insertPagesBook(BufferedReader reader, Integer bookId) {
        Integer success = 0;
        try {
            String content;
            Integer page = 1;
            List<Map<String, Object>> pages = dao.findPages(bookId);
            if(!pages.isEmpty()) {
                return success;
            }
            char[] cbuf = new char[6000];
            int charsRead;
            while ((charsRead = reader.read(cbuf)) != -1) {
                content = new String(cbuf, 0, charsRead);
                dao.insertPages( bookId,content,page );
                page++;
            }
            reader.close();
            success=1;
        }catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return success;

    }

    public Integer insertPagesBook(BufferedReader reader, String bookTitle) {
        Integer success = 0;
        Map<String, Object> book = dao.findBook(bookTitle);

        if(book.isEmpty()){
            return success;
        }
        Integer bookId = (Integer) book.get("book_id");
        logger.info("bookId:"+bookId);
        success = insertPagesBook(reader, bookId);
        return success;

    }


}
