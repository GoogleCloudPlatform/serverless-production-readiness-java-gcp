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
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageProperties;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.Likelihood;
import com.google.cloud.vision.v1.Property;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import services.data.BooksService;
import services.data.FirestoreService;

@RestController
@RequestMapping("/images")
public class ImageProcessingController {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingController.class);

    private final FirestoreService eventService;

    private BooksService booksService;

    public ImageProcessingController(FirestoreService eventService, BooksService booksService, VertexAIClient vertexAIClient) {
        this.eventService = eventService;
        this.booksService = booksService;
        this.vertexAIClient = vertexAIClient;
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

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
            List<AnnotateImageRequest> requests = new ArrayList<>();

            ImageSource imageSource = ImageSource.newBuilder()
                    .setGcsImageUri("gs://" + bucketName + "/" + fileName)
                    .build();

            Image image = Image.newBuilder()
                    .setSource(imageSource)
                    .build();

            Feature featureLabel = Feature.newBuilder()
                    .setType(Type.LABEL_DETECTION)
                    .build();
            Feature featureImageProps = Feature.newBuilder()
                    .setType(Type.IMAGE_PROPERTIES)
                    .build();
            Feature featureSafeSearch = Feature.newBuilder()
                    .setType(Type.SAFE_SEARCH_DETECTION)
                    .build();

            Feature featureTextDetection = Feature.newBuilder()
                    .setType(Type.TEXT_DETECTION)
                    .build();

            Feature featureLogoDetection = Feature.newBuilder()
                    .setType(Type.LOGO_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(featureLabel)
                    .addFeatures(featureImageProps)
                    .addFeatures(featureSafeSearch)
                    .addFeatures(featureTextDetection)
                    .addFeatures(featureLogoDetection)
                    .setImage(image)
                    .build();

            requests.add(request);

            logger.info("Calling the Vision API...");
            BatchAnnotateImagesResponse result = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = result.getResponsesList();

            if (responses.isEmpty()) {
                logger.info("No response received from Vision API.");
                return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
            }

            AnnotateImageResponse response = responses.getFirst();
            if (response.hasError()) {
                logger.info("Error: " + response.getError().getMessage());
                return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
            }

            List<String> labels = response.getLabelAnnotationsList().stream()
                    .map(annotation -> annotation.getDescription())
                    .collect(Collectors.toList());
            logger.info("Annotations found by Vision API:");
            for (String label: labels) {
                logger.info("- " + label);
            }

            String mainColor = "#FFFFFF";
            ImageProperties imgProps = response.getImagePropertiesAnnotation();
            if (imgProps.hasDominantColors()) {
                DominantColorsAnnotation colorsAnn = imgProps.getDominantColors();
                ColorInfo colorInfo = colorsAnn.getColors(0);

                mainColor = rgbHex(
                        colorInfo.getColor().getRed(),
                        colorInfo.getColor().getGreen(),
                        colorInfo.getColor().getBlue());

                logger.info("Color: " + mainColor);
            }

            boolean isSafe = false;
            if (response.hasSafeSearchAnnotation()) {
                SafeSearchAnnotation safeSearch = response.getSafeSearchAnnotation();

                isSafe = Stream.of(
                                safeSearch.getAdult(), safeSearch.getMedical(), safeSearch.getRacy(),
                                safeSearch.getSpoof(), safeSearch.getViolence())
                        .allMatch( likelihood ->
                                likelihood != Likelihood.LIKELY && likelihood != Likelihood.VERY_LIKELY
                        );

                logger.info("Is Image Safe? " + isSafe);
            }

            logger.info("Logo Annotations:");
            for (EntityAnnotation annotation : response.getLogoAnnotationsList()) {
                logger.info("Logo: " + annotation.getDescription());

                List<Property> properties = annotation.getPropertiesList();
                logger.info("Logo property list:");
                for (Property property : properties) {
                    logger.info("Name: %s, Value: %s", property.getName(), property.getValue());
                }
            }

            String prompt = "Explain the text ";
            var ref = new Object() {
                String textElements;
            };

            logger.info("Text Annotations:");
            //put these items below into a array list
            ArrayList<String> books = new ArrayList<>(Arrays.asList("Ulysses", "Meditations", "The Republic", "The Complete Works of William Shakespeare", "The Jungle Book"));
            String bookTitle = "";

            for (EntityAnnotation annotation : response.getTextAnnotationsList()) {
                ref.textElements = annotation.getDescription();
                prompt += ref.textElements + " ";
                logger.info("Text: " + ref.textElements);
                if(bookTitle.equals(""))
                    bookTitle = books.stream().filter(b-> b.contains(ref.textElements)).findAny().get();
                // if(textElements.matches("^[a-zA-Z0-9]+$"))
                prompt += ref.textElements;
            }

            // use summary in the prompt to the llm
            String summary = booksService.getBookSummary(bookTitle);

            // build alternative prompt using Vertex AI
            //  extractTextFromImage(bucketName, fileName);

            String modelResponse = null;
            if (!prompt.isEmpty()) {
                modelResponse = vertexAIClient.prompt(prompt, "chat-bison");
                logger.info("Result Chat Model: " + vertexAIClient.prompt(prompt, "chat-bison"));
            }

            if (!prompt.isEmpty()) {
                modelResponse = vertexAIClient.prompt(prompt, "text-bison");
                logger.info("Result Chat Model: " + vertexAIClient.prompt(prompt, "text-bison"));
            }

            // Saving result to Firestore
            if (isSafe && modelResponse != null) {
                ApiFuture<WriteResult> writeResult = eventService.storeImage(fileName, labels, mainColor, modelResponse);
                logger.info("Picture metadata saved in Firestore at " + writeResult.get().getUpdateTime());
            }
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