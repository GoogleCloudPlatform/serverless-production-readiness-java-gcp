/*
 * Copyright 2021 Google LLC
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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.core.env.Environment;
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
import services.ai.VertexModels;
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

    private Environment environment;

    private CloudStorageService cloudStorageService;

    public ImageProcessingController(FirestoreService eventService, BooksService booksService, VertexAIClient vertexAIClient, CloudStorageService cloudStorageService, Environment environment) {
        this.eventService = eventService;
        this.booksService = booksService;
        this.vertexAIClient = vertexAIClient;
        this.environment = environment;
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

        byte[] image = cloudStorageService.readFileAsByteString(bucketName, fileName);
        GenerateContentResponse response  = vertexAIClient.promptOnImageWithVertex(image);

        String prompt = "Explain the text ";

        logger.info("Text Annotations:");
        //put these items below into a array list
//        ArrayList<String> books = new ArrayList<>(Arrays.asList("Ulysses", "Meditations", "The Republic", "The Complete Works of William Shakespeare", "The Jungle Book"));
        String jsonResponse = "";
//        example of the json Map from gemini
//        {
//            "bookName": "The Jungle Book",
//                "mainColor": "green",
//                "author": "Rudyard Kipling",
//                "labels": []
//        }
        Map<String, Object> jsonMap = new HashMap<>();
        String bookTitle = "";
        String mainColor = "";
        String author = "";
        List<String> labels = new ArrayList<>();

        for (Candidate candidate : response.getCandidatesList()) {
                List<Part> parts = candidate.getContent().getPartsList();
                String textElements = "";
                if(parts.size() == 0) {
                    continue;
                }
                for(Part p : parts) {
                    if(p.getText()!=null && p.getText().length() > 0) {
                        jsonResponse = p.getText();
                        try {
                            jsonMap = JsonUtility.parseJsonToMap(jsonResponse);
                        } catch (Exception e) {
                           logger.warn(e.toString());
                        }
                    }
                    textElements += p.getText() + " ";
                }
                prompt += textElements + " ";
                logger.info("Text: " + textElements);
                // if(textElements.matches("^[a-zA-Z0-9]+$"))
                prompt += textElements;
        }

        bookTitle = (String) jsonMap.get("bookName");
        mainColor = (String) jsonMap.get("mainColor");
        author = (String) jsonMap.get("author");
        labels = (List) jsonMap.get("labels");

            // use summary in the prompt to the llm
            // build alternative prompt using Vertex AI
            //  extractTextFromImage(bucketName, fileName);
        logger.info("Result bookTitle: " + bookTitle +" mainColor: "+mainColor+" labels: "+labels);
        String modelResponse = null;
        if (!prompt.isEmpty()) {
            modelResponse = vertexAIClient.promptWithLangchain4J(prompt, VertexModels.CHAT_BISON);
            logger.info("Result Chat Model: " + modelResponse);
        }

        // dead code?
        // if (!prompt.isEmpty()) {
        //     String model = environment.getProperty("spring.cloud.config.modelImageProName");
        //     modelResponse = vertexAIClient.promptWithLangchain4J(prompt, model);
        //     logger.info("Result Chat Model: " + modelResponse);
        // }

        String summary = booksService.getBookSummary(bookTitle);
        logger.info("The summary of the book "+bookTitle+ " is: " + summary);
        // Saving result to Firestore
        if (modelResponse != null) {
            ApiFuture<WriteResult> writeResult = eventService.storeImage(fileName, labels, mainColor, modelResponse);
            logger.info("Picture metadata saved in Firestore at " + writeResult.get().getUpdateTime());
        }

        return new ResponseEntity<String>(msg, HttpStatus.OK);
    }


    // private void extractTextFromImage(String bucketName, String fileName) throws IOException {
    //     try (EndpointServiceClient endpointServiceClient = EndpointServiceClient.create()) {
    //         EndpointName name =
    //                 EndpointName.ofProjectLocationEndpointName("[PROJECT]", "[LOCATION]", "[ENDPOINT]");
    //         Endpoint response = endpointServiceClient.getEndpoint(name);
    //         logger.info("Endpoint description: " +response.getDescription());
    //     }
    // }

    private static String rgbHex(float red, float green, float blue) {
        return String.format("#%02x%02x%02x", (int)red, (int)green, (int)blue);
    }

}
// [END eventarc_audit_storage_handler]