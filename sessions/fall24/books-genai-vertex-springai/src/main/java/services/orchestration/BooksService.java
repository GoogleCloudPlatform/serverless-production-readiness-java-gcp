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
package services.orchestration;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
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

    @Value("classpath:/prompts/text-system-message.st")
    Resource textSystemMessage;

    @Value("classpath:/prompts/summary-user-message.st")
    Resource summaryUserMessage;

    @Value("classpath:/prompts/summary-grounded-user-message.st")
    Resource summaryGroundedUserMessage;

    @Value("classpath:/prompts/analysis-user-message.st")
    Resource analysisUserMessage;

    @Value("classpath:/prompts/image-system-message.st")
    Resource imageSystemMessage;

    @Value("classpath:/prompts/image-user-message.st")
    Resource imageUserMessage;

    @Value("classpath:/prompts/text-functions-system-message.st")
    Resource textFunctionsSystemMessage;

    @Value("classpath:/prompts/bookstore-user-message.st")
    Resource bookStoreUserMessage;

    @Value("classpath:/prompts/sentiment-analysis-system-message.st")
    Resource sentimentAnalysisSystemMessage;

    @Value("classpath:/prompts/sentiment-analysis-user-message.st")
    Resource sentimentAnalysisUserMessage;

    public BooksService(VertexAIClient vertexAIClient,
                        CloudStorageService cloudStorageService,
                        BooksDataService booksDataService,
                        FirestoreService eventService) {
        this.vertexAIClient = vertexAIClient;
        this.cloudStorageService = cloudStorageService;
        this.booksDataService = booksDataService;
        this.eventService = eventService;
    }

    // Create book summary from an existing file in  Cloud Storage and persist in the database
    public String createBookSummary(String bucketName, String fileName) {
        // read the book content from Cloud Storage
        String bookText = cloudStorageService.readFileAsString(bucketName, fileName);

        // extract the book title
        String bookTitle = SqlUtility.replaceUnderscoresWithSpaces(FileUtility.getTitle(fileName));

        // lookup book summary in the database
        String summary = booksDataService.getBookSummary(bookTitle);
        if (!summary.isEmpty())
            return summary;

        // find the book in the book table
        // extract the book id
        Integer bookId = booksDataService.findBookByTitle(bookTitle);

        // create a SystemMessage
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(textSystemMessage);
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("name", "Gemini", "voice", "literary critic"));

        // book content has been found in Cloud Storage; summarize the content
        // create a UserMessage
        PromptTemplate userPromptTemplate = new PromptTemplate(summaryUserMessage);
        Message userMessage = userPromptTemplate.createMessage(Map.of("content", bookText));

        // prompt the model for a summary
        summary = vertexAIClient.promptModel(systemMessage, userMessage, model);
        logger.info("The summary for book {} is: {}", bookTitle, summary);

        // insert summary in table
        booksDataService.insertBookSummary(bookId, summary);

        return summary;
    }

    // Create book summary from a file which does not exist in Cloud Storage
    public String createBookSummaryWebGrounded(String title, String author, String publicationYear, String bucketName) {
        // lookup book summary in the database
        String summary = booksDataService.getBookSummary(title);
        if (!summary.isEmpty())
            return summary;

        // find the book in the book table
        // extract the book id
        Integer bookId = booksDataService.findBookByTitle(title);

        // create a SystemMessage
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(textSystemMessage);
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("name", "Gemini", "voice", "literary critic"));

        PromptTemplate userPromptTemplate;
        Message userMessage;
        if(bookId == null) {
            // insert the book data in the books and authors tables
            String defaultFormattedFileNameInCloudStorage = String.format("%s-%s-%s-public.txt", title, author, publicationYear);
            bookId = booksDataService.insertBookAndAuthorData(defaultFormattedFileNameInCloudStorage);
        }

        // book content has not been found in Cloud Storage
        // use grounding with Web search to get a book summary
        userPromptTemplate = new PromptTemplate(summaryGroundedUserMessage);
        userMessage = userPromptTemplate.createMessage(Map.of(
                "title", title,
                "author", author));


        // prompt the model for a summary
        summary = vertexAIClient.promptModelGrounded(systemMessage, userMessage, model, true);
        logger.info("The summary for book {} is: {}", title, summary);

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
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(textSystemMessage);
        Message systemMessage = systemPromptTemplate.createMessage(
                Map.of("name", "Gemini", "voice", "literary critic"));

        // build user prompt to query LLM with the augmented context
        Message userMessage = PromptUtility.formatPromptBookAnalysis(analysisUserMessage,
                bookRequest, responseBook, bookRequest.keyWords());

        logger.info("Book analysis flow - Model: {}", model);

        // submit prompt to the LLM via LLM orchestration framework
        return vertexAIClient.promptModel(systemMessage, userMessage, model);
    }

    // Analyze book, start from book cover image
    public void analyzeImage(String bucketName, String fileName) {
        // multi-modal call to retrieve text from the uploaded image
        SystemPromptTemplate imageSystemPromptTemplate = new SystemPromptTemplate(imageSystemMessage);
        Message imageSystemMessage = imageSystemPromptTemplate.createMessage(
                Map.of("name", "Gemini", "voice", "multimedia analyst"));

        // bucket where image has been uploaded
        String imageURL = String.format("gs://%s/%s",bucketName, fileName);

        // create User Message for AI framework
        PromptTemplate userPromptTemplate = new PromptTemplate(imageUserMessage);
        Message imageAnalysisUserMessage = userPromptTemplate.createMessage(List.of(new Media(MimeTypeUtils.parseMimeType("image/*"), imageURL)));

        VertexAIClient.ImageDetails imageData = vertexAIClient.promptOnImage(
                imageSystemMessage,
                imageAnalysisUserMessage,
                model);
        logger.info("Image Analysis Result: Author {}, Title {}", imageData.author(), imageData.title());

        // retrieve the book summary from the database
        String summary = booksDataService.getBookSummary(imageData.title());
        logger.info("The summary of the book {},as retrieved from the database, is: {}", imageData.title(), summary);
        logger.info("End of summary of the book {},as retrieved from the database", imageData.title());

        // Function calling BookStoreService
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(textFunctionsSystemMessage);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "Gemini", "voice", "literary critic"));

        PromptTemplate userMessageTemplate = new PromptTemplate(bookStoreUserMessage);
        Message userMessage = userMessageTemplate.createMessage(Map.of("title", imageData.title(), "author", imageData.author(), "summary", summary));

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

    // Build Few-shot history
    private List<Message> messages = List.of(
            new UserMessage("Lord of the Rings by J.R.R. Tolkien"),
            new AssistantMessage("Fantasy"),
            new UserMessage("Dune by Frank Herbert"),
            new AssistantMessage("Science Fiction"),
            new UserMessage("Murder on the Orient Express by Agatha Christie"),
            new AssistantMessage("Mistery"),
            new UserMessage("Pride and Prejudice by Jane Austen"),
            new AssistantMessage("Romance"),
            new UserMessage("Dracula by Bram Stoker"),
            new AssistantMessage("Horror"),
            new UserMessage("Gone With the Wind by Margaret Mitchell"),
            new AssistantMessage("Historical Fiction"),
            new UserMessage("1984 by George Orwell"),
            new AssistantMessage("Dystopian"),
            new UserMessage("Catch-22 by Joseph Heller"),
            new AssistantMessage("Comedy"),
            new UserMessage("The Autobiography of Benjamin Franklin"),
            new AssistantMessage("Biography/Autobiography"),
            new UserMessage("Sapiens: A Brief History of Humankind by Yuval Noah Harari"),
            new AssistantMessage("History"),
            new UserMessage("A Brief History of Time by Stephen Hawking"),
            new AssistantMessage("Science"),
            new UserMessage("How to Win Friends and Influence People by Dale Carnegie"),
            new AssistantMessage("Self-Help"),
            new UserMessage("The Wealth of Nations by Adam Smith\n"),
            new AssistantMessage("Business/Economics"),
            new UserMessage("Joy of Cooking"),
            new AssistantMessage("Cookbook"),
            new UserMessage("Eat, Pray, Love by Elizabeth Gilbert"),
            new AssistantMessage("Travel")
    );

    // classify book and get sentiment analysis of main character
    public String sentimentAnalysis(String title, String author) {
        // create the system prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(sentimentAnalysisSystemMessage);
        Message systemMessage = systemPromptTemplate.createMessage();

        // create the memory for the few-shot history
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatMemory.add("examples", messages);

        PromptTemplate userMessageTemplate = new PromptTemplate(sentimentAnalysisUserMessage);
        Message userMessage = userMessageTemplate.createMessage(Map.of("title", title, "author", author));

        return vertexAIClient.promptModelWithMemory(systemMessage,
                                                    userMessage,
                                                    model,
                                                    chatMemory);
    }
}
