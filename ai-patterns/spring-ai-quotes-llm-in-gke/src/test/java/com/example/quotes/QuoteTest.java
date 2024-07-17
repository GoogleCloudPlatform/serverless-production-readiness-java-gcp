package com.example.quotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import com.example.quotes.domain.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.MapOutputConverter;

class QuoteTest {

  @Test
  void testParseQuoteFromJson_withValidJson() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withValidJsonWithoutCodeBlocks() {
    String input =
            "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withInvalidJson() {
    String input = "This is not valid JSON";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals(input, quote.getQuote());
    assertEquals(0L, quote.getId());
  }

  @Test
  void testParseQuoteFromJson_withPartialJson() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\"\n"
                    + "}```";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withEmptyJson() {
    String input = "```json\n{}\n```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("", quote.getAuthor());
    assertEquals("", quote.getQuote());
    assertEquals("", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocks() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson2() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson3() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson4() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson5() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson6() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

  @Test
  void testParseQuoteFromJson_withJsonContainingMultipleCodeBlocksAndInvalidJson7() {
    String input =
            "```json\n"
                    + "{\n"
                    + "  \"author\": \"Albert Einstein\",\n"
                    + "  \"quote\": \"The important thing is not to stop questioning. Curiosity has its own reason for existing.\",\n"
                    + "  \"book\": \"The World As I See It\"\n"
                    + "}\n"
                    + "```json\n"
                    + "This is not valid JSON\n"
                    + "```json\n"
                    + "{\n"
                    + "  \"author\": \"Isaac Newton\",\n"
                    + "  \"quote\": \"If I have seen further it is by standing on the shoulders of giants.\",\n"
                    + "  \"book\": \"Letter to Robert Hooke\"\n"
                    + "}\n"
                    + "```json";

    Quote quote = Quote.parseQuoteFromJson(input);

    assertNotNull(quote);
    assertEquals("Albert Einstein", quote.getAuthor());
    assertEquals(
            "The important thing is not to stop questioning. Curiosity has its own reason for existing.",
            quote.getQuote());
    assertEquals("The World As I See It", quote.getBook());
  }

}