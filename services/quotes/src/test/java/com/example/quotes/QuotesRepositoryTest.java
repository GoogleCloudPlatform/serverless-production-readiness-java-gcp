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
package com.example.quotes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.quotes.domain.Quote;
import com.example.quotes.domain.QuoteRepository;
import com.example.quotes.domain.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test persistence for the Quotes service using Postgres Testcontainer
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuotesRepositoryTest {
  @Container
  @ServiceConnection
  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3-alpine");

  @Autowired
  private QuoteRepository quoteRepository;
  private QuoteService quoteService;

  @BeforeEach
  void setUp() {
    quoteService = new QuoteService(quoteRepository);
  }

  @Test
  @DisplayName("A random quote is returned")
  void testRandomQuotes() {
    // var quote = this.quoteRepository.findRandomQuote();
    var quote = this.quoteService.findRandomQuote();
    assertThat(quote).isNotNull();
  }

  @Test
  @DisplayName("All quotes are returned")
  void testAllQuotes() {
    var quotes = this.quoteService.getAllQuotes();
    assertThat(quotes).isNotNull();
  }

  @Test
  @DisplayName("Create a quote")
  void testCreateQuote(){
    var quote = new Quote();
    quote.setAuthor("Truman Capote");
    quote.setQuote("Anyone who ever gave you confidence, you owe them a lot.");
    quote.setBook("Breakfast at Tiffany''s");

    var result = this.quoteService.createQuote(quote);
    assertThat(result.getAuthor()).isEqualTo("Truman Capote");
  }

  @Test
  @DisplayName("Delete a quote - failed")
  void testDeleteQuote(){
    assertFalse(quoteRepository.existsById(1000L));

    assertDoesNotThrow(() -> quoteService.deleteById(1000L));
  }

  @Test
  @DisplayName("Delete a quote - good")
  void testDeleteQuoteGood(){
    var quote = new Quote();
    quote.setAuthor("Tennessee Williams");
    quote.setQuote("Time is the longest distance between two places.");
    quote.setBook("The Glass Menagerie");

    var result = this.quoteService.createQuote(quote);
    assertThat(result.getAuthor()).isEqualTo("Tennessee Williams");

    assertDoesNotThrow(() -> quoteRepository.deleteById(result.getId()));
  }
}
