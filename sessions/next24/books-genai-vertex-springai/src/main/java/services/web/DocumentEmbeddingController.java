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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import services.actuator.StartupCheck;
import services.config.CloudConfig;
import services.domain.BooksService;
import services.domain.CloudStorageService;

@RestController
@RequestMapping("/document")
public class DocumentEmbeddingController {

  private static final Logger logger = LoggerFactory.getLogger(DocumentEmbeddingController.class);

  BooksService booksService;
  CloudStorageService cloudStorageService;

  public DocumentEmbeddingController(BooksService booksService,
      CloudStorageService cloudStorageService) {
    this.booksService = booksService;
    this.cloudStorageService = cloudStorageService;
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

  @RequestMapping(value = "/embeddings", method = RequestMethod.POST)
  public ResponseEntity<String> receiveMessage(
      @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) {
    logger.info("Header elements");
    for (String field : CloudConfig.requiredFields) {
      if (headers.get(field) == null) {
        String msg = String.format("Missing expected header: %s.", field);
        logger.error(msg);
        return new ResponseEntity<>(msg, HttpStatus.BAD_REQUEST);
      } else {
        logger.info(field + " : " + headers.get(field));
      }
    }

    logger.info("Body elements");
    for (String bodyField : body.keySet()) {
      logger.info(bodyField + " : " + body.get(bodyField));
    }

    if (headers.get("ce-subject") == null) {
      String msg = "Missing expected header: ce-subject.";
      logger.error(msg);
      return new ResponseEntity<>(msg, HttpStatus.BAD_REQUEST);
    }

    String ceSubject = headers.get("ce-subject");
    String msg = "Detected change in Cloud Storage bucket: (ce-subject) : " + ceSubject;
    logger.info(msg);

    // get docuemnt name and bucket
    String fileName = (String) body.get("name");
    String bucketName = (String) body.get("bucket");

    logger.info("New book uploaded for embedding:" + fileName);

    if (fileName == null) {
      msg = "Missing expected body element: file name";
      logger.error(msg);
      return new ResponseEntity<>(msg, HttpStatus.BAD_REQUEST);
    }

    // add embedding functionality here
    long start = System.currentTimeMillis();
    BufferedReader br = cloudStorageService.readFile(bucketName, fileName);
    logger.info("Embedding flow - read book: " + (System.currentTimeMillis() - start) + "ms");

    start = System.currentTimeMillis();
    Integer bookId = booksService.insertBook(fileName);
    booksService.insertPagesBook(br, bookId);
    logger.info("Embedding flow - insert book and pages: " + (System.currentTimeMillis() - start) + "ms");

    // success
    return new ResponseEntity<>(msg, HttpStatus.OK);
  }
}
