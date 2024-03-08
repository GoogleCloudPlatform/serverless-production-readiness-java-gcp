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

        logger.info(String.format("Result: Author %s, Title %s", title, author));

        // String modelResponse = null;
        // if (!prompt.isEmpty()) {
        //     modelResponse = vertexAIClient.promptModel(prompt, VertexModels.CHAT_BISON);
        //     logger.info("Result Chat Model: " + modelResponse);
        // }

        // retrieve the book summary from the database
        String summary = booksService.getBookSummary(title);
        logger.info("The summary of the book:"+ title+ " is: " + summary);

        // WIP - finalize after Function Calling is wired in
        // Saving result to Firestore
        // if (modelResponse != null) {
        // ApiFuture<WriteResult> writeResult = eventService.storeImage(fileName, labels, mainColor, modelResponse);
        // logger.info("Picture metadata saved in Firestore at " + writeResult.get().getUpdateTime());
        // }

        return new ResponseEntity<String>(msg, HttpStatus.OK);
    }

}
