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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.actuator.StartupCheck;
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
  public static final String NAME = "name";
  public static final String BUCKET = "bucket";

  private BooksService booksService;
  private CloudStorageService cloudStorageService;

  public DocumentEmbeddingController(BooksService booksService,
      CloudStorageService cloudStorageService) {
    this.booksService = booksService;
    this.cloudStorageService = cloudStorageService;
  }

  @PostConstruct
  public void init() {
      logger.info("BookImagesApplication: DocumentEmbeddingController Post Construct Initializer {}",
              new SimpleDateFormat("HH:mm:ss.SSS").format(
              new java.util.Date(System.currentTimeMillis())));
      logger.info(
          "BookImagesApplication: DocumentEmbeddingController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @GetMapping("start")
  String start() {
      logger.info("BookImagesApplication: DocumentEmbeddingController - Executed start endpoint request {}",
              new SimpleDateFormat("HH:mm:ss.SSS").format(
              new java.util.Date(System.currentTimeMillis())));
    return "DocumentEmbeddingController started";
  }

  // endpoint triggered when a new file is being uploaded to Cloud  Storage
  @RequestMapping(value = "/embeddings", method = RequestMethod.POST)
  public ResponseEntity<String> receiveMessage(
            @RequestBody Map<String, Object> body,
            @RequestHeader Map<String, String> headers) {
    // validate  headers
    // request received as a CLoudEvent
    String errorMsg = RequestValidationUtility.validateRequest(body,headers);
    if (!errorMsg.isBlank()) {
        logger.error("Document Embedding Request failed: {}", errorMsg);
        return new ResponseEntity<>(errorMsg, HttpStatus.BAD_REQUEST);
    }

    // get document name and bucket
    String fileName = (String) body.get(NAME);
    String bucketName = (String) body.get(BUCKET);

    logger.info("New book uploaded for embedding: {}", fileName);

    // read file from Cloud Storage
    long start = System.currentTimeMillis();
    BufferedReader br = cloudStorageService.readFile(bucketName, fileName);
    logger.info("Embedding flow - read book: {}ms", System.currentTimeMillis() - start);

    // add embedding functionality here
    // persist book info to AlloyDB
    // persist book pages as embeddings in AlloyDB
    start = System.currentTimeMillis();
    Integer bookId = booksService.insertBook(fileName);

    booksService.insertPagesBook(br, bookId);
    logger.info("Embedding flow - insert book and pages: {}ms", System.currentTimeMillis() - start);

    // embedding flows are executed async, latency not the same priority
    // as in real-time request processing
    // create a summary of the document
    //    long start = System.currentTimeMillis();
    //    logger.info("Book summarization flow : start");
    //    logger.info("Summarize book with title {} from Cloud Storage bucket {}", fileName, bucketName);
    //    String summary = booksService.createBookSummary(bucketName, fileName, true);
    //    logger.info("Book summarization flow: end {}ms", System.currentTimeMillis() - start);

    return new ResponseEntity<>(HttpStatus.OK);
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
}
