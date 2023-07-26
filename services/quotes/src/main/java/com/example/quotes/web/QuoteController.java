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
package com.example.quotes.web;

import com.example.quotes.actuator.StartupCheck;
import com.example.quotes.domain.Quote;
import com.example.quotes.domain.QuoteService;
import jakarta.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle requests for Quote CRUD operations
 */
@RestController
public class QuoteController {
    private static final Logger logger = LoggerFactory.getLogger(QuoteController.class);

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostConstruct
    public void init() {
        logger.info("QuotesApplication: QuoteController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
        logger.info("QuotesApplication: QuoteController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    @GetMapping("start")
    String start() {
        logger.info("QuotesApplication: QuotesController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
        return "QuoteController started";
    }

    @GetMapping("/random-quote")
    public Quote randomQuote()
    {
        return quoteService.findRandomQuote();
    }

    @GetMapping("/quotes") 
    public ResponseEntity<List<Quote>> allQuotes()
    {
        try {
            List<Quote> quotes = quoteService.getAllQuotes();

            if (quotes.isEmpty())
                return new ResponseEntity<List<Quote>>(HttpStatus.NO_CONTENT);
                
            return new ResponseEntity<List<Quote>>(quotes, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<List<Quote>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }        
    }

    @GetMapping("/quotes/author/{author}")
    public ResponseEntity<List<Quote>>quoteByAuthor(@PathVariable("author") String author) {
        try {
            List<Quote> quotes = quoteService.getByAuthor(author);

            if(!quotes.isEmpty()){
                return new  ResponseEntity<List<Quote>>(quotes, HttpStatus.OK);
            } else {
                return new  ResponseEntity<List<Quote>>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new  ResponseEntity<List<Quote>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/quotes")
    public ResponseEntity<Quote> createQuote(@RequestBody Quote quote) {
        try {
            Quote saved = quoteService.createQuote(quote);
            return new ResponseEntity<Quote>(saved, HttpStatus.CREATED);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<Quote>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }     

    @PutMapping("/quotes/{id}")
    public ResponseEntity<Quote> updateQuote(@PathVariable("id") Long id, @RequestBody Quote quote) {
        try {
            Optional<Quote> existingQuote = quoteService.findById(id);
            
            if(existingQuote.isPresent()){
                Quote updatedQuote = existingQuote.get();
                updatedQuote.setAuthor(quote.getAuthor());
                updatedQuote.setQuote(quote.getQuote());
                quoteService.updateQuote(updatedQuote);

                return new ResponseEntity<Quote>(updatedQuote, HttpStatus.OK);
            } else {
                return new ResponseEntity<Quote>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<Quote>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }     

    @DeleteMapping("/quotes/{id}")
    public ResponseEntity<HttpStatus> deleteQuote(@PathVariable("id") Long id) {
        try {
            quoteService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch(EmptyResultDataAccessException e){
            return new ResponseEntity<HttpStatus>(HttpStatus.NOT_FOUND);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }    
}
