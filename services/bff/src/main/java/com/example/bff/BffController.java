/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.bff;

import com.example.bff.actuator.StartupCheck;
import com.example.bff.data.Data;
import com.example.bff.data.Quote;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BffController {
  // logger
  private static final Log logger = LogFactory.getLog(BffController.class);

  @Value("${quotes_url}")
  String quotesURL;

  @Value("${reference_url}")
  String referenceURL;

  @Value("${faulty_url}")
  String faultyUrl;

  static long read;
  static long write;

  @Value("${read_timeout}")
  public void setRead(Long readTimeout) {
    read = readTimeout;
  }

  @Value("${write_timeout}")
  public void setWrite(Long writeTimeout){
    write = writeTimeout;
  }

  // Instantiate OkHttpClient
  private static OkHttpClient ok = null;

  // Metadata
  private static String metadata = null;

  @PostConstruct
  public void init() {
      logger.info("BffController: Post Construct Initializer");
      if(referenceURL==null){
        logger.error("Reference Service URL has not been configured. Please set the QUOTES_URL env variable");
        StartupCheck.down();
      }

      if(quotesURL==null){
        logger.error("Quotes Service URL has not been configured. Please set the QUOTES_URL env variable");
        StartupCheck.down();
      }

      // build the HTTP client
      ok = new OkHttpClient.Builder()
            .readTimeout(read, TimeUnit.MILLISECONDS)
            .writeTimeout(write, TimeUnit.MILLISECONDS)
            .build();

      // retrieve metadata at startup
      if(metadata == null) {
        try {
          ResponseBody data = ServiceRequests.makeAuthenticatedRequest(ok, referenceURL,
              "metadata");
          metadata = data.string();

          logger.info("Metadata:" + metadata);
        } catch (IOException e) {
          logger.error("Unable to get Reference service data", e);

          // fail the startup actuator
          StartupCheck.down();
          return;
        }
      }
      // enable the startup actuator
      StartupCheck.up();
  }

  @GetMapping("start")
  String start() {
    logger.info("BFFApplication: BFFController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    return (metadata != null ? "BFFController started" : "BFFController started however Reference Data service could not be accessed");
  }

  @GetMapping("/quotes") 
  public ResponseEntity<String> allQuotes(){
    // retrieve all quotes
    try {
      ResponseBody quotes = ServiceRequests.makeAuthenticatedRequest(ok, quotesURL, "quotes");

      return new ResponseEntity<String>(quotes.string(), HttpStatus.OK);
    } catch (IOException e) {
      logger.error("Failed to retrieve data from the Quotes service:", e);
      return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/faulty")
  public ResponseEntity<String> getFromFaultyService() {
    // retrieve all quotes
    try {
      ResponseBody faultyResponse = ServiceRequests.makeAuthenticatedRequest(ok, faultyUrl, "");

      return new ResponseEntity<String>(faultyResponse.string(), HttpStatus.OK);
    } catch (IOException e) {
      logger.error("Failed to retrieve data from the Faulty service:", e);
      return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/quotes")
  public ResponseEntity<String> createQuote(@RequestBody Quote data) {
    logger.info("Quote: " + data);

    // build a Quote
    Quote quote = new Quote();
    quote.setAuthor(data.getAuthor());
    quote.setQuote(data.getQuote());
    quote.setBook(data.getBook());

    ObjectMapper mapper = new ObjectMapper();
    String quoteString;
    try {
      quoteString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(quote);

      ResponseBody quotes = ServiceRequests.makeAuthenticatedPostRequest(ok, quotesURL, "quotes", quoteString);
      return new ResponseEntity<String>(quotes.string(), HttpStatus.OK);
    } catch (IOException e) {
      logger.error("Failed to post data to the Quotes service:" + e.getMessage());
      return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }    
  }

  @DeleteMapping("/quotes/{id}")
  public ResponseEntity<HttpStatus> deleteQuote(@PathVariable("id") Integer id) {
    logger.info("Delete by ID: " + id);

    String path = String.format("%s/%s", "quotes", id.toString());
    try {
      Response status = ServiceRequests.makeAuthenticatedDeleteRequest(ok, quotesURL, path);
      return new ResponseEntity<HttpStatus>(HttpStatus.valueOf(status.code()));
    } catch (IOException e) {
      logger.error("Failed to delete data in the Quotes service:", e);
      return new ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
