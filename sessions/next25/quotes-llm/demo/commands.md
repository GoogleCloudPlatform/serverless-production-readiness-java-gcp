
Commands for the demo
```shell
./mvnw package spring-boot:run

I want to get details about the QuotesApplication; please provide a detailed overview of the QuotesApplication

Please perform a detailed code review of the QuoteEndpoint

Please recommend code improvements to the QuoteEndpoint class

Which types of test should I be writing for the QuoteEndpoint

Please answer briefly whether I should add tests for network failures?

Answer as a Software Engineer with expertise in Java. Create a test for the QuoteEndpoint for a method quoteByBook which responds to the UI and retrieves a quote from the book The Road

// generate a unit test for the getByBook method in the QuoteService; create a Quote in the QuoteService first then test the getByBook method against the new Quote

// generate a getByBook method which retrieves a quote by book name

@QuoteEndpoint refine the quoteByBook method in QuoteEndpoint, handle the error codes from the QuotesService and return the error codes to the UI

```

Snippets of code
```java
    public List<Quote> quoteByBook(String book) {
        return quoteService.getByAuthor("Truman Capote");
    }
    
    @Test
    void quoteByBook() {
        String book = "Truman Capote";
        List<Quote> expectedQuotes = new ArrayList<>();
        when(quoteService.getByAuthor(book)).thenReturn(expectedQuotes);
        List<Quote> result = quoteEndpoint.quoteByBook(book);
        assertEquals(expectedQuotes, result);
        verify(quoteService, times(1)).getByAuthor(book);
    }    
```