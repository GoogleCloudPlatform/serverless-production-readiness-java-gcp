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
package services.web;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import services.actuator.StartupCheck;
import services.ai.VertexAIClient;
import services.config.CloudConfig;
import services.domain.BooksService;
import services.domain.CloudStorageService;
import services.domain.FirestoreService;
import services.utility.JsonUtility;

@RestController
@RequestMapping("/images")
public class ImageProcessingController {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingController.class);

    private final FirestoreService eventService;

    private BooksService booksService;

    private CloudStorageService cloudStorageService;

    @Value("${prompts.promptImage}")
    private String promptImage;

    public ImageProcessingController(FirestoreService eventService, BooksService booksService, VertexAIClient vertexAIClient, CloudStorageService cloudStorageService) {
        this.eventService = eventService;
        this.booksService = booksService;
        this.vertexAIClient = vertexAIClient;
        this.cloudStorageService = cloudStorageService;
    }

    VertexAIClient vertexAIClient;

    @PostConstruct
    public void init() {
        logger.info("BookImagesApplication: ImageProcessingController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
        logger.info("BookImagesApplication: ImageProcessingController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    @GetMapping("start")
    String start(){
        logger.info("BookImagesApplication: ImageProcessingController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
        return "ImageProcessingController started";
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<String> receiveMessage(
        @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) throws IOException, InterruptedException, ExecutionException {
        System.out.println("Header elements");
        for (String field : CloudConfig.requiredFields) {
            if (headers.get(field) == null) {
                String msg = String.format("Missing expected header: %s.", field);
                System.out.println(msg);
                return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
            } else {
                System.out.println(field + " : " + headers.get(field));
            }
        }

        System.out.println("Body elements");
        for (String bodyField : body.keySet()) {
            System.out.println(bodyField + " : " + body.get(bodyField));
        }

        if (headers.get("ce-subject") == null) {
            String msg = "Missing expected header: ce-subject.";
            System.out.println(msg);
            return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
        }

        String ceSubject = headers.get("ce-subject");
        String msg = "Detected change in Cloud Storage bucket: (ce-subject) : " + ceSubject;
        System.out.println(msg);

        String fileName = (String)body.get("name");
        String bucketName = (String)body.get("bucket");

        logger.info("New picture uploaded " + fileName);

        if(fileName == null){
            msg = "Missing expected body element: file name";
            System.out.println(msg);
            return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
        }

        // multi-modal call to retrieve text from the uploaded image
        String response = vertexAIClient.promptOnImage(promptImage, bucketName, fileName);

        // parse the response and extract the data
        Map<String, Object> jsonMap = JsonUtility.parseJsonToMap(response);

        String title = (String) jsonMap.get("title");
        String author = (String) jsonMap.get("author");

        logger.info(String.format("Result: Author %s, Title %s", author, title));

        // retrieve the book summary from the database
        String summary = booksService.getBookSummary(title);
        logger.info("The summary of the book:"+ title+ " is: " + summary);

        // Function calling BookStoreService
        UserMessage systemMessage = new UserMessage("""
                Use Multi-turn function calling.
                Answer with precision.
                If the information was not fetched call the function again. Repeat at most 3 times.
                """);

        // UserMessage userMessage = new UserMessage(
        //     String.format("Please find out if the book with the title %s by author %s is available in the University bookstore.",
        //                     title, author));
        UserMessage userMessage = new UserMessage(
            String.format("Write a nice note including book author, book title and availability. Find out if the book with the title %s by author %s is available in the University bookstore.Please add also this book summary to the response, with the text available after the column, prefix it with My Book Summary:  %s",
            title, author, summary));

        String bookStoreResponse = vertexAIClient.promptModelwithFunctionCalls(systemMessage, 
                                                                               userMessage,
                                                                               new BookStoreService());

        // Saving result to Firestore
        if (bookStoreResponse != null) {
            ApiFuture<WriteResult> writeResult = eventService.storeBookInfo(fileName, title, author, summary, bookStoreResponse);
            logger.info("Picture metadata saved in Firestore at " + writeResult.get().getUpdateTime());
        }

        return new ResponseEntity<String>(msg, HttpStatus.OK);
    }
    static class BookStoreService {
        @Tool("Find book availability in bookstore based on the book title and book author")
        BookStoreResponse getBookAvailability(@P("The title of the book") String title,
            @P("The author of the book") String author) {
            System.out.printf("Called getBookAvailability(%s, %s)%n", title, author);
            return new BookStoreResponse(title, author, "The book is available for purchase in the book store in paperback format.");
        }

        public record BookStoreRequest(
            @P("The title of the book") String title,
            @P("The author of the book") String author) {
        }
        public record BookStoreResponse(String title, String author, String availability) {}
    }

    // @Bean
    // @Description("Get availability of book in the book store")
    // public Function<BookStoreService.Request, BookStoreService.Response> bookStoreAvailability() {
    //     return new BookStoreService();
    // }
    // public static class BookStoreService
    //     implements Function<BookStoreService.Request, BookStoreService.Response> {
    //
    //     @JsonInclude(Include.NON_NULL)
    //     @JsonClassDescription("BookStore API Request")
    //     public record Request(
    //         @JsonProperty(required = true, value = "title") @JsonPropertyDescription("The title of the book") String title,
    //         @JsonProperty(required = true, value = "author") @JsonPropertyDescription("The author of the book") String author) {
    //     }
    //     @JsonInclude(Include.NON_NULL)
    //     public record Response(String title, String author, String availability) {
    //     }
    //
    //     @Override
    //     public Response apply(Request request) {
    //         System.out.println("BookStore availability request: " + request);
    //         return new Response(request.title(), request.author(), "The book is available for purchase in the book store in paperback format.");
    //     }
    // }
    //
    // public static class BookStoreServiceRuntimeHints implements RuntimeHintsRegistrar {
    //     @Override
    //     public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    //         // Register method for reflection
    //         var mcs = MemberCategory.values();
    //         hints.reflection().registerType(BookStoreService.Request.class, mcs);
    //         hints.reflection().registerType(BookStoreService.Response.class, mcs);
    //     }
    //
    // }
}
