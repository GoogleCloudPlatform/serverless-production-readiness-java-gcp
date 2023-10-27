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
package com.example.quotes.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quote Domain Service to access quotes in the repository
 */
@Service
@Transactional //(readOnly = true)
public class QuoteService {
  private final QuoteRepository quoteRepository;

  public QuoteService(QuoteRepository quoteRepository) {
    this.quoteRepository = quoteRepository;
  }

  public Quote findRandomQuote() {
    return quoteRepository.findRandomQuote();
  }

  public List<Quote> getAllQuotes() {
    return quoteRepository.findAll();
  }

  @Transactional
  public Quote createQuote(Quote quote){
    return quoteRepository.save(quote);
  }

  @Transactional
  public Quote updateQuote(Quote quote){ return quoteRepository.save(quote); }

  public Optional<Quote> findById(Long id){
    return quoteRepository.findById(id);
  }

  public List<Quote> getByAuthor(String author) {
    return quoteRepository.findByAuthor(author);
  }

  public void deleteById(Long id){
    quoteRepository.deleteById(id);
  }
}
