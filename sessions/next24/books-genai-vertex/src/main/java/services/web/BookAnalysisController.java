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

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import services.actuator.StartupCheck;
import services.data.FirestoreService;
import services.web.data.BookInquiryResponse;
import services.web.data.BookRequest;

@RestController
@RequestMapping("/analysis")
public class BookAnalysisController {
  private static final Logger logger = LoggerFactory.getLogger(BookAnalysisController.class);

  private final FirestoreService eventService;

  public BookAnalysisController(FirestoreService eventService) {
    this.eventService = eventService;
  }

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: BookAnalysisController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: BookAnalysisController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  /*
 # cURL request example:
     curl -X POST \
    -H "Content-Type: application/json" \
    -d '{
        "book": "The Jungle Book",
        "author": "Rudyard Kipling",
        "topics": ["adventure", "animals", "coming-of-age"]
    }' \
    http://localhost:8080/analysis

   */
  @PostMapping("/")
  public ResponseEntity<BookInquiryResponse>processUserRequest(@RequestBody BookRequest bookRequest){
    String analysis = "";

    BookInquiryResponse response = new BookInquiryResponse(
        bookRequest.book(),
        bookRequest.author(),
        bookRequest.topics(),
        analysis);

    return new ResponseEntity<BookInquiryResponse>(response, HttpStatus.OK);
  }
}
