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

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.actuator.StartupCheck;
import services.config.CloudConfig;
import services.data.BooksService;
import services.data.CloudStorageService;
import utility.FileUtility;
@RestController
@RequestMapping("/document")
public class DocumentEmbeddingController {
  private static final Logger logger = LoggerFactory.getLogger(DocumentEmbeddingController.class);

  @Autowired
  BooksService booksService;

  @Autowired
  CloudStorageService cloudStorageService;

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: DocumentEmbeddingController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: DocumentEmbeddingController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @GetMapping("start")
  String start(){
    logger.info("BookImagesApplication: DocumentEmbeddingController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    return "DocumentEmbeddingController started";
  }

  @RequestMapping(value = "/category/books", method = RequestMethod.GET)
  public ResponseEntity<List<Map<String, Object>>> getTable(@RequestParam(name = "prompt") String prompt, @RequestParam(name = "contentCharactersLimit", defaultValue = "2000") String contentCharactersLimit) {
    return new ResponseEntity<List<Map<String, Object>>>(booksService.prompt(prompt, Integer.parseInt(contentCharactersLimit)), HttpStatus.OK);
  }

  @RequestMapping(value = "/category/books", method = RequestMethod.POST)
  public ResponseEntity<Integer> insertTable(@RequestBody Map<String, Object> body) {
//    Integer success = booksService.insertBook((String) body.get("fileName"));
//    Integer success = booksService.insertPagesBook( (String) body.get("filePath"), (String) body.get("bookTitle") );
    String fileName = (String) body.get("fileName");
//    BufferedReader br = cloudStorageService.readFile((String) body.get("bucketName"), fileName);
    booksService.insertBook(fileName);
//    Integer success = booksService.insertPagesBook(br, FileUtility.getTitle(fileName));
    return new ResponseEntity<>(1 > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
  }

  @RequestMapping(value = "/embeddings", method = RequestMethod.POST)
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

    logger.info("New book uploaded for embedding" + fileName);

    if(fileName == null){
      msg = "Missing expected body element: file name";
      System.out.println(msg);
      return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
    }

    // add embedding functionality here
    BufferedReader br = cloudStorageService.readFile(bucketName, fileName);
    booksService.insertBook(fileName);
    booksService.insertPagesBook(br, FileUtility.getTitle(fileName));

    // success
    return new ResponseEntity<String>(msg, HttpStatus.OK);
  }
}
