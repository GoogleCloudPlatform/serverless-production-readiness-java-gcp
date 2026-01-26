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
import com.example.quotes.domain.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.jspecify.annotations.Nullable;
import com.vaadin.hilla.crud.CrudService;
import com.vaadin.hilla.crud.JpaFilterConverter;
import com.vaadin.hilla.crud.filter.Filter;
import jakarta.annotation.PostConstruct;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;

/**
 * Endpoint for the UI
 */
@BrowserCallable
@AnonymousAllowed
public class QuoteEndpoint implements CrudService<Quote, Long> {
    private static final Logger logger = LoggerFactory.getLogger(QuoteEndpoint.class);

    private final QuoteService quoteService;
    private final QuoteLLMService quoteLLMService;
    private final QuoteLLMInVertexService quoteLLMInVertexService;

    public QuoteEndpoint(QuoteService quoteService,
                         QuoteLLMService quoteLLMService,
                         QuoteLLMInVertexService quoteLLMInVertexService) {
        this.quoteService = quoteService;
        this.quoteLLMService = quoteLLMService;
        this.quoteLLMInVertexService = quoteLLMInVertexService;
    }

    @PostConstruct
    public void init() {
        logger.info("QuotesApplication: QuoteController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
        logger.info("QuotesApplication: QuoteController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    public Quote randomQuote() {
        return quoteService.findRandomQuote();
    }

    public Quote randomLLMQuote() { return quoteLLMService.findRandomQuote(); }

    public Quote randomLLMInVertexQuote() { return quoteLLMInVertexService.findRandomQuote(); }

    public List<Quote> allQuotes() {
        return quoteService.getAllQuotes();
    }

    public List<Quote> quoteByAuthor(String author) {
        return quoteService.getByAuthor(author);
    }

    public List<Quote> quoteByBook(String book) {
        return quoteService.getByBook(book);
    }

    @Override
    public Quote save(Quote quote) {
        if (quote.getId() == null) {
            return quoteService.createQuote(quote);
        } else {
            return quoteService.updateQuote(quote);
        }
    }

    @Override
    public void delete(Long id) {
        quoteService.deleteById(id);
    }

    @Override
    public List<Quote> list(Pageable pageable, @Nullable Filter filter) {
        return quoteService.list(pageable, JpaFilterConverter.toSpec(filter, Quote.class));
    }
}
