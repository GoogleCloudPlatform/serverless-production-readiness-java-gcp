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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;

import services.actuator.StartupCheck;
import services.ai.VertexAIClient;
import services.domain.BooksService;
import services.utility.PromptUtility;
import services.web.data.BookRequest;

/**
 * Controller for the Book Analysis service.
 * This controller is responsible for processing the user request for book analysis.
 * The controller will prompt AlloyDB for the embeddings for the book in the request.
 * The controller will build a prompt to query LLM with the augmented context.
 * Returns the book anaylsis response to the caller.
 */
@RestController
@RequestMapping("/analysis")
public class BookAnalysisController {
  private static final Logger logger = LoggerFactory.getLogger(BookAnalysisController.class);

  private final BooksService booksService;
  private final VertexAIClient vertexAIClient;
  public BookAnalysisController(BooksService booksService, VertexAIClient vertexAIClient) {
    this.booksService = booksService;
    this.vertexAIClient = vertexAIClient;
  }

  @Value("${spring.cloud.config.modelAnalysisName}")
  private String model;

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: BookAnalysisController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: BookAnalysisController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @CrossOrigin
  @PostMapping("")
  public ResponseEntity<String> processUserRequest(@RequestBody BookRequest bookRequest, @RequestParam(name = "contentCharactersLimit", defaultValue = "6000") Integer contentCharactersLimit){

    long start = System.currentTimeMillis();
    logger.info("Book analysis flow : start");

    // Prompt AlloyDB for the embeddings for the book in the request
    List<Map<String, Object>> responseBook = booksService.prompt(bookRequest, contentCharactersLimit);
    logger.info("Book analysis flow: retrieve embeddings from AlloyDB AI: " + (System.currentTimeMillis() - start) + "ms");

    // build prompt to query LLM with the augmented context
    String promptWithContext = PromptUtility.formatPromptBookAnalysis(bookRequest, responseBook, bookRequest.keyWords());

    logger.info("Book analysis flow - Model: " + model);
    start = System.currentTimeMillis();

    // submit prompt to the LLM via LLM orchestration framework
    String response = vertexAIClient.promptModel(promptWithContext);
    logger.info("Book analysis flow: prompt LLM: " + (System.currentTimeMillis() - start) + "ms");

    // return the response to the caller
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
