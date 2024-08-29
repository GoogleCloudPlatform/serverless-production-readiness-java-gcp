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
package services.client;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import services.ai.VertexAIClient;
import services.domain.BooksDataService;
import services.domain.CloudStorageService;
import services.domain.FirestoreService;
import services.utility.FileUtility;
import services.utility.PromptUtility;
import services.utility.SqlUtility;
import services.web.data.BookRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class BooksService {
    private static final Logger logger = LoggerFactory.getLogger(BooksService.class);

    private final VertexAIClient vertexAIClient;
    private final CloudStorageService cloudStorageService;
    private final BooksDataService booksDataService;
    private final FirestoreService eventService;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    String model;

    public BooksService(VertexAIClient vertexAIClient,
                        CloudStorageService cloudStorageService,
                        BooksDataService booksDataService,
                        FirestoreService eventService) {
        this.vertexAIClient = vertexAIClient;
        this.cloudStorageService = cloudStorageService;
        this.booksDataService = booksDataService;
        this.eventService = eventService;
    }

    // Create book summary and persist in the database
    public String createBookSummary(String bucketName, String fileName, boolean overwriteIfSummaryExists) {
        // read the book content from Cloud Storage
        String bookText = cloudStorageService.readFileAsString(bucketName, fileName);

        // extract the book title
        String bookTitle = FileUtility.getTitle(fileName);
        bookTitle = SqlUtility.replaceUnderscoresWithSpaces(bookTitle);

        // lookup book summary in the database
        String summary = booksDataService.getBookSummary(bookTitle);
        if (!summary.isEmpty() && !overwriteIfSummaryExists)
            return summary;

        // find the book in the book table
        // extract the book id
        Integer bookId = booksDataService.findBookByTitle(bookTitle);

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

        // insert summary in table
        booksDataService.insertBookSummary(bookId, summary);

        return summary;
    }

    // Analyze book by title, author and specific keywords
    public String analyzeBookByKeywords(BookRequest bookRequest, Integer contentCharactersLimit){
        long start = System.currentTimeMillis();
        // Prompt AlloyDB for embeddings for the book in the request
        List<Map<String, Object>> responseBook = booksDataService.prompt(bookRequest, contentCharactersLimit);
        logger.info("Book analysis flow: retrieve embeddings from AlloyDB AI: {}ms", System.currentTimeMillis() - start);

        // create a SystemMessage
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate("""
            You are a helpful AI assistant.
            You are an experienced AI assistant that helps people extract detailed information from images.
            Your name is {name}
            You should reply to the user's request with your name and also in the style of an {voice}.
            Strictly ignore Project Gutenberg & ignore copyright notice in summary output.
            """
        );
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("name", "Gemini", "voice", "image analyst"));

        // build user prompt to query LLM with the augmented context
        Message userMessage = new UserMessage(PromptUtility.formatPromptBookAnalysis(bookRequest, responseBook, bookRequest.keyWords()));

        logger.info("Book analysis flow - Model: {}", model);

        // submit prompt to the LLM via LLM orchestration framework
        return vertexAIClient.promptModel(systemMessage, userMessage, model);
    }

    // Analyze book, start from book cover image
    public void analyzeImage(String bucketName, String fileName) {
        // multi-modal call to retrieve text from the uploaded image
        // property file ```promptImage: ${PROMPT_IMAGE:Extract the title and author from the image, strictly in JSON format}```
        String imageUserMessage = "Extract the title and author from the image, strictly in JSON format";
        // bucket where image has been uploaded
        String imageURL = String.format("gs://%s/%s",bucketName, fileName);

        VertexAIClient.ImageDetails imageData = vertexAIClient.promptOnImage(imageUserMessage, imageURL, model);
        logger.info("Image Analysis Result: Author {}, Title {}", imageData.author(), imageData.title());

        // retrieve the book summary from the database
        String summary = booksDataService.getBookSummary(imageData.title());
        logger.info("The summary of the book {},as retrieved from the database, is: {}", imageData.title(), summary);
        logger.info("End of summary of the book {},as retrieved from the database", imageData.title());

        // Function calling BookStoreService
        SystemMessage systemMessage = new SystemMessage("""
                Use Multi-turn function calling.
                Answer with precision.
                If the information was not fetched call the function again. Repeat at most 3 times.
                """);

        PromptTemplate userMessageTemplate = new PromptTemplate("""
                Write a nice note including book author, book title and availability.
                Find out if the book with the title {title} by author {author} is available in the University bookstore.
                Please add also this book summary to the response, with the text available after the column, prefix it with The Book Summary:  {summary}
                """,
                Map.of("title", imageData.title(), "author", imageData.author(), "summary", summary));
        Message userMessage = userMessageTemplate.createMessage();

        String bookStoreResponse = vertexAIClient.promptModelWithFunctionCalls(systemMessage,
                userMessage,
                "bookStoreAvailability",
                model);

        // Saving result to Firestore
        if (bookStoreResponse != null) {
            ApiFuture<WriteResult> writeResult = eventService.storeBookInfo(fileName, imageData.title(), imageData.author(), summary, bookStoreResponse);
            try {
                logger.info("Picture metadata saved in Firestore at {}", writeResult.get().getUpdateTime());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Could not save picture metadata in Firestore: {}", e.getMessage());
            }
        }
    }
}
