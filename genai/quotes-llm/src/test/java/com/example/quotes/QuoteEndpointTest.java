package com.example.quotes;

import com.example.quotes.domain.Quote;
import com.example.quotes.domain.QuoteLLMInVertexService;
import com.example.quotes.domain.QuoteLLMService;
import com.example.quotes.domain.QuoteService;
import com.example.quotes.web.QuoteEndpoint;
import com.vaadin.hilla.crud.JpaFilterConverter;
import com.vaadin.hilla.crud.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteEndpointTest {

    @Mock
    private QuoteService quoteService;

    @Mock
    private QuoteLLMService quoteLLMService;

    @Mock
    private QuoteLLMInVertexService quoteLLMInVertexService;

    @Mock
    private JpaFilterConverter jpaFilterConverter;

    @InjectMocks
    private QuoteEndpoint quoteEndpoint;

    private Quote quote1;
    private Quote quote2;
    private List<Quote> quotes;

    @BeforeEach
    void setUp() {
        quote1 = new Quote();
        quote1.setId(1L);
        quote1.setAuthor("Author 1");
        quote1.setBook("Book 1");

        quote2 = new Quote();
        quote2.setId(2L);
        quote2.setAuthor("Author 2");
        quote2.setBook("Book 2");

        quotes = new ArrayList<>();
        quotes.add(quote1);
        quotes.add(quote2);
    }

    @Test
    void init() {
        quoteEndpoint.init();
        // No assertions needed here, just checking that it runs without errors and logs are generated.
    }

    @Test
    void randomQuote() {
        when(quoteService.findRandomQuote()).thenReturn(quote1);
        Quote result = quoteEndpoint.randomQuote();
        assertEquals(quote1, result);
        verify(quoteService, times(1)).findRandomQuote();
    }

    @Test
    void randomLLMQuote() {
        when(quoteLLMService.findRandomQuote()).thenReturn(quote1);
        Quote result = quoteEndpoint.randomLLMQuote();
        assertEquals(quote1, result);
        verify(quoteLLMService, times(1)).findRandomQuote();
    }

    @Test
    void randomLLMInVertexQuote() {
        when(quoteLLMInVertexService.findRandomQuote()).thenReturn(quote1);
        Quote result = quoteEndpoint.randomLLMInVertexQuote();
        assertEquals(quote1, result);
        verify(quoteLLMInVertexService, times(1)).findRandomQuote();
    }

    @Test
    void allQuotes() {
        when(quoteService.getAllQuotes()).thenReturn(quotes);
        List<Quote> result = quoteEndpoint.allQuotes();
        assertEquals(quotes, result);
        verify(quoteService, times(1)).getAllQuotes();
    }

    @Test
    void quoteByAuthor() {
        String author = "Author 1";
        List<Quote> expectedQuotes = List.of(quote1);
        when(quoteService.getByAuthor(author)).thenReturn(expectedQuotes);
        List<Quote> result = quoteEndpoint.quoteByAuthor(author);
        assertEquals(expectedQuotes, result);
        verify(quoteService, times(1)).getByAuthor(author);
    }

    @Test
    void save_create() {
        Quote newQuote = new Quote();
        newQuote.setAuthor("New Author");
        newQuote.setBook("New Book");

        Quote savedQuote = new Quote();
        savedQuote.setId(3L);
        savedQuote.setAuthor("New Author");
        savedQuote.setBook("New Book");

        when(quoteService.createQuote(newQuote)).thenReturn(savedQuote);
        Quote result = quoteEndpoint.save(newQuote);
        assertEquals(savedQuote, result);
        verify(quoteService, times(1)).createQuote(newQuote);
        verify(quoteService, never()).updateQuote(any());
    }

    @Test
    void save_update() {
        when(quoteService.updateQuote(quote1)).thenReturn(quote1);
        Quote result = quoteEndpoint.save(quote1);
        assertEquals(quote1, result);
        verify(quoteService, times(1)).updateQuote(quote1);
        verify(quoteService, never()).createQuote(any());
    }

    @Test
    void delete() {
        Long id = 1L;
        quoteEndpoint.delete(id);
        verify(quoteService, times(1)).deleteById(id);
    }

    @Test
    void list() {
        Pageable pageable = Pageable.unpaged();
        Filter filter = new Filter();
        Specification<Quote> spec = Specification.where(null);

        when(jpaFilterConverter.toSpec(filter, Quote.class)).thenReturn(spec);
        when(quoteService.list(pageable, spec)).thenReturn(quotes);

        List<Quote> result = quoteEndpoint.list(pageable, filter);
        assertEquals(quotes, result);
        verify(jpaFilterConverter, times(1)).toSpec(filter, Quote.class);
        verify(quoteService, times(1)).list(pageable, spec);
    }

}

