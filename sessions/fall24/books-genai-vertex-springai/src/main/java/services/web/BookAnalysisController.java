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
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import services.actuator.StartupCheck;
import services.orchestration.BooksService;
import services.web.data.BookRequest;

/**
 * Controller for the Book Analysis service.
 * This controller is responsible for processing the user request for book analysis.
 * The controller will prompt AlloyDB for the embeddings for the book in the request.
 * The controller will build a prompt to query LLM with the augmented context.
 * Returns the book analysis response to the caller.
 */
@RestController
@RequestMapping("/analysis")
public class BookAnalysisController {
  private static final Logger logger = LoggerFactory.getLogger(BookAnalysisController.class);

  private final BooksService booksService;

  public BookAnalysisController(BooksService booksService) {
    this.booksService = booksService;
  }

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: BookAnalysisController Post Construct Initializer {}",
            new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: BookAnalysisController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @CrossOrigin
  @PostMapping("")
  public ResponseEntity<String> bookAnalysis(@RequestBody BookRequest bookRequest,
                                             @RequestParam(name = "contentCharactersLimit",
                                                     defaultValue = "6000") Integer contentCharactersLimit){
    // analyze book by title, author and specific keywords
    long start = System.currentTimeMillis();
    logger.info("Book analysis flow : start");
    String response = booksService.analyzeBookByKeywords(bookRequest, contentCharactersLimit);
    logger.info("Book analysis flow: prompt LLM: {}ms", System.currentTimeMillis() - start);
    logger.info("Book analysis flow: done");

    // return the response to the caller
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
