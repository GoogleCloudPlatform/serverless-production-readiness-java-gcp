
Commands for the demo
```shell
./mvnw package spring-boot:run
```

```Please provide a brief explanation of each Java file in the folder quotes-llm
```

```
I want to get details about the QuotesApplication; please provide a detailed overview of the QuotesApplication
```
```
Please perform a detailed code review of the QuoteEndpoint
```
```
Please recommend code improvements to the QuoteEndpoint class
```

```
Which types of test should I be writing for the QuoteEndpoint
```

```
Please answer briefly whether I should add tests for network failures?
```

```
Answer as a Software Engineer with expertise in Java. Create a test for the QuoteEndpoint for a method quoteByBook which responds to the UI and retrieves a quote from the book The Road
```

```
// generate a getByBook method which retrieves a quote by book name
```

```
// generate a unit test for the getByBook method in the QuoteService; create a Quote in the QuoteService first then test the getByBook method against the new Quote
```

```
// generate a getByBook method which retrieves a quote by book name
```

```
// generate a findByBook method which retrieves a quote by book name; use the nativeQuery syntax
```

```
@QuoteEndpoint refine the quoteByBook method in QuoteEndpoint, handle the error codes from the QuotesService and return the error codes to the UI
```

```
Answer as a Software Engineer with expertise in Java. Create an endpoint for the QuoteEndpoint for a method quoteByBookandAuthor which responds to the UI and retrieves a quote using the book name and book author; implement the endpoint, and downstream service and repository methods
```




---

Snippets of sample code
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


// generate a findByBook method which retrieves a quote by book name; use the nativeQuery syntax
@Transactional(readOnly = true)
@Cacheable(value = "quoteCache", key = "book")
@Query(nativeQuery = true, value = "SELECT id, quote, author, book FROM quotes WHERE book = :book")
List<Quote> findByBook(@Param("book") String book);


// generate a getByBook method which retrieves a quote by book name
public List<Quote> getByBook(String book) {
  return quoteRepository.findByBook(book);
}

// generate a unit test for the getByBook method in the QuoteService; create a Quote in the QuoteService first then test the getByBook method against the new Quote
@Test
@DisplayName("Get a quote by book")
void testGetByBook() {
  var quote = new Quote();
  quote.setAuthor("Tennessee Williams");
  quote.setQuote("Time is the longest distance between two places.");
  quote.setBook("The Glass Menagerie");

  var result = this.quoteService.createQuote(quote);
  assertThat(result.getAuthor()).isEqualTo("Tennessee Williams");

  var quotes = this.quoteService.getByBook("The Glass Menagerie");
  assertThat(quotes).isNotEmpty();
  assertThat(quotes.get(0).getBook()).isEqualTo("The Glass Menagerie");
}
```