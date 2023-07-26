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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test appplication through its web controllers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
public class QuotesControllerTest {
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3-alpine");

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Test returns Quotes")
  void shouldReturnQuotes() throws Exception {
    mockMvc.perform(get("/quotes"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @DisplayName("Test returns Quotes by Author")
  void shouldReturnQuoteByAuthor() throws Exception {
    mockMvc.perform(get("/quotes/author/George Orwell"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].author", Matchers.equalTo("George Orwell")));
  }

  @Test
  @DisplayName("Test saves Book quote in database")
  void shouldSaveProduct() throws Exception {
    mockMvc.perform(
            post("/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                  {
                  "author": "Isabel Allende",
                  "quote": "The longer I live, the more uninformed I feel. Only the young have an explanation for everything.",
                  "book": "City of the Beasts"
                  }
                  """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", Matchers.notNullValue()))
        .andExpect(jsonPath("$.author", Matchers.equalTo("Isabel Allende")))
        .andExpect(jsonPath("$.quote", Matchers.equalTo("The longer I live, the more uninformed I feel. Only the young have an explanation for everything.")))
        .andExpect(jsonPath("$.book", Matchers.equalTo("City of the Beasts")));
  }
}
