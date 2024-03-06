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
import java.util.*;
import javax.annotation.PostConstruct;

import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import kotlin.collections.ArrayDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Blob;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.protobuf.ByteString;
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
import services.domain.CloudStorageService;
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

  private CloudStorageService cloudStorageService;

  public BookAnalysisGeminiController(FirestoreService eventService, BooksService booksService, VertexAIClient vertexAIClient, Environment environment, CloudStorageService cloudStorageService) {
    this.eventService = eventService;
    this.booksService = booksService;
    this.vertexAIClient = vertexAIClient;
    this.environment = environment;
    this.cloudStorageService = cloudStorageService;
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
    byte[] image = cloudStorageService.readFileAsByteString("library_next24_images", "TheJungleBook.jpg");
    GenerateContentResponse response  = vertexAIClient.promptOnImage(image);
    return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
  }
}
