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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import kotlin.collections.ArrayDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import services.actuator.StartupCheck;
import services.ai.VertexAIClient;
import services.config.CloudConfig;
import services.domain.BooksService;
import services.domain.FirestoreService;
import services.utility.PromptUtility;
import services.web.data.BookInquiryResponse;
import services.web.data.BookRequest;

@RestController
@RequestMapping("/geminianalysis")
public class BookAnalysisGeminiController {
  private static final Logger logger = LoggerFactory.getLogger(BookAnalysisGeminiController.class);

  private final FirestoreService eventService;
  private BooksService booksService;
  private VertexAIClient vertexAIClient;
  private Environment environment;

  public BookAnalysisGeminiController(FirestoreService eventService, BooksService booksService, VertexAIClient vertexAIClient, Environment environment) {
    this.eventService = eventService;
    this.booksService = booksService;
    this.vertexAIClient = vertexAIClient;
    this.environment = environment;
  }

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: BookAnalysisController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: BookAnalysisController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @PostMapping("")
  public ResponseEntity<String> processUserRequest(@RequestBody BookRequest bookRequest, 
                                                   @RequestParam(name = "contentCharactersLimit", defaultValue = "6000") Integer contentCharactersLimit) throws IOException{
    // given
    // ChatLanguageModel visionModel = VertexAiGeminiChatModel.builder()
    //         .endpoint("us-central1-aiplatform.googleapis.com:443")
    //         .project("next24-genai-app")
    //         .location("us-central1")
    //         .modelName("gemini-pro-vision")
    //         .publisher("google")
    //         .build();

    // VertexAiChatModel visionModel = VertexAiChatModel.builder()
    //     .endpoint("us-central1-aiplatform.googleapis.com:443")
    //     .project(CloudConfig.projectID)
    //     .location(CloudConfig.zone)
    //     .publisher("google")
    //     .modelName("gemini-1.0-pro-vision")
    //     .temperature(0.1)
    //     .maxOutputTokens(1000)
    //     .topK(0)
    //     .topP(0.0)
    //     .maxRetries(3)
    //     .build();

    String projectId = CloudConfig.projectID;
    String location = "us-central1";
    String modelName = "gemini-1.0-pro-vision";

    GenerateContentResponse response = null;
    try (VertexAI vertexAI = new VertexAI(projectId, location)) {
      String imageUri = "gs://library_next24_images/TheJungleBook.jpg";
      GenerativeModel model = new GenerativeModel(modelName, vertexAI);
      
      response = model.generateContent(ContentMaker.fromMultiModalData(
          PartMaker.fromMimeTypeAndData("image/jpg", imageUri),
          "Extract the author and book name from this image"
      ));
        
      System.out.println(response.toString());
    }
    return new ResponseEntity<String>(HttpStatus.OK);
  }
}
