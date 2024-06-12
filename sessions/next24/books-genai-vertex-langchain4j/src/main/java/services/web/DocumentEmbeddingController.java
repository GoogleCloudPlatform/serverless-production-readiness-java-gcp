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

import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.actuator.StartupCheck;
import services.ai.VertexAIClient;
import services.config.CloudConfig;
import services.domain.BooksService;
import services.domain.CloudStorageService;
import services.utility.RequestValidationUtility;

/**
 * Controller for handling document embedding requests, generating document summaries
 * and persistence to Vector and SQL databases.
 */
@RestController
@RequestMapping("/document")
public class DocumentEmbeddingController {

  private static final Logger logger = LoggerFactory.getLogger(DocumentEmbeddingController.class);

  BooksService booksService;
  CloudStorageService cloudStorageService;

  VertexAIClient vertexAIClient;

  @Value("${prompts.promptTransformTF}")
  private String promptTransformTF;

  @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
  private String model;

  @Value("classpath:/bashscripts/provision-cloud-infra.sh")
  private Resource bashscript;

  public DocumentEmbeddingController(BooksService booksService,
      CloudStorageService cloudStorageService, VertexAIClient vertexAIClient) {
    this.booksService = booksService;
    this.cloudStorageService = cloudStorageService;
    this.vertexAIClient = vertexAIClient;
  }

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: DocumentEmbeddingController Post Construct Initializer "
        + new SimpleDateFormat("HH:mm:ss.SSS").format(
        new java.util.Date(System.currentTimeMillis())));
    logger.info(
        "BookImagesApplication: DocumentEmbeddingController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @GetMapping("start")
  String start() {
    logger.info(
        "BookImagesApplication: DocumentEmbeddingController - Executed start endpoint request "
            + new SimpleDateFormat("HH:mm:ss.SSS").format(
            new java.util.Date(System.currentTimeMillis())));
    return "DocumentEmbeddingController started";
  }

  // used for testing
  @RequestMapping(value = "/category/books", method = RequestMethod.GET)
  public ResponseEntity<List<Map<String, Object>>> getTable(
      @RequestParam(name = "prompt") String prompt,
      @RequestParam(name = "contentCharactersLimit", defaultValue = "2000") String contentCharactersLimit) {
    return new ResponseEntity<>(
        booksService.prompt(prompt, Integer.parseInt(contentCharactersLimit)), HttpStatus.OK);
  }

  // used for testing
  @RequestMapping(value = "/category/books", method = RequestMethod.POST)
  public ResponseEntity<Integer> insertTable(@RequestBody Map<String, Object> body) {
    String fileName = (String) body.get("fileName");
    booksService.insertBook(fileName);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/terraform", method = RequestMethod.POST)
  public ResponseEntity<String> receiveMessageTransform(
          @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) {
    String errorMsg = RequestValidationUtility.validateRequest(body,headers);
    if (!errorMsg.isBlank()) {
      return new ResponseEntity<>(errorMsg, HttpStatus.BAD_REQUEST);
    }

    // get document name and bucket
    String fileName = (String) body.get("name");
    String bucketName = (String) body.get("bucket");

    logger.info("New script to transform:" + fileName);

    // read file from Cloud Storage
    BufferedReader br = cloudStorageService.readFile(bucketName, fileName);

    String response = tfTransformTransform(br.toString());

    // success
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @CrossOrigin
  @RequestMapping(value = "/bash/to-terraform", method = RequestMethod.POST)
  public ResponseEntity<String> tfTransformTransform(
          @RequestBody Map<String, Object> body) {

    String script = (String) body.get("script");
    String response = tfTransformTransform(script);

    // success
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  public String tfTransformTransform(String script) {
    logger.info("tf transform flow");
    long start = System.currentTimeMillis();
    String transformScript = String.format(promptTransformTF, script);
    // submit prompt to the LLM via LLM orchestration framework
    logger.info("TF transform: prompt LLM: " + (System.currentTimeMillis() - start) + "ms");
    String response = vertexAIClient.promptModel(transformScript);
    logger.info("TF transform flow: " + (System.currentTimeMillis() - start) + "ms");

    // success
    return response;
  }

  @RequestMapping(value = "/embeddings", method = RequestMethod.POST)
  public ResponseEntity<String> receiveMessage(
      @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) {
    String errorMsg = RequestValidationUtility.validateRequest(body,headers);
    if (!errorMsg.isBlank()) {
      return new ResponseEntity<>(errorMsg, HttpStatus.BAD_REQUEST);
    }

    // get document name and bucket
    String fileName = (String) body.get("name");
    String bucketName = (String) body.get("bucket");

    logger.info("New book uploaded for embedding:" + fileName);

    // read file from Cloud Storage
    long start = System.currentTimeMillis();
    BufferedReader br = cloudStorageService.readFile(bucketName, fileName);
    logger.info("Embedding flow - read book: " + (System.currentTimeMillis() - start) + "ms");

    // add embedding functionality here
    // persist book and pages to AlloyDB
    start = System.currentTimeMillis();
    Integer bookId = booksService.insertBook(fileName);
    booksService.insertPagesBook(br, bookId);
    logger.info("Embedding flow - insert book and pages: " + (System.currentTimeMillis() - start) + "ms");

    // success
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
