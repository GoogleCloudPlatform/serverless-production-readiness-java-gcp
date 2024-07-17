package com.example.quotes.domain;

import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class QuoteLLMInGKEService {
  private OpenAiChatModel chatClient;
  private Environment env;

  public static final String VERTEX_AI_GEMINI_PROJECT_ID = "VERTEX_AI_GEMINI_PROJECT_ID";
  public static final String VERTEX_AI_GEMINI_LOCATION = "VERTEX_AI_GEMINI_LOCATION";
  public static final String VERTEX_AI_GEMINI_MODEL = "VERTEX_AI_GEMINI_MODEL";

  public QuoteLLMInGKEService(OpenAiChatModel chatClient, Environment env) {
    this.chatClient = chatClient;
    this.env = env;
  }

  public Quote findRandomQuote() {
    SystemMessage systemMessage = new SystemMessage("""
        You are a helpful AI assistant. 
        You are an AI assistant that helps people find information.
        """);
    UserMessage userMessage = new UserMessage("""
          Answer succintly;
          please provide a quote from a random book, including book, quote and author; 
          do not repeat quotes from the same book; 
          return the answer wrapped in triple backquotes ```json```strictly in JSON format        
          """);

    ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
        OpenAiChatOptions.builder()
            .withTemperature(0.4f)
            .build())
    );

    // MapOutputConverter converter = new MapOutputConverter();
    // Generation generation = chatResponse.getResult();
    // Map<String, Object> result = converter.convert(generation.getOutput().getContent());
    MapOutputConverter converter = new MapOutputConverter();
    Generation generation = chatResponse.getResult();
    String input = generation.getOutput().getContent();

    String startDelimiter = "{";
    String endDelimiter = "}";

    int startIndex = input.indexOf(startDelimiter)-1;
    startIndex += startDelimiter.length();
    int endIndex = input.indexOf(endDelimiter+1, startIndex);

    // MapOutputConverter converter = new MapOutputConverter();
    // Generation generation = chatResponse.getResult();
    // Map<String, Object> result = converter.convert(generation.getOutput().getContent());

    Map<String, Object> result = converter.convert(input.substring(startIndex, endIndex).trim());

    Quote quote = new Quote();
    quote.setId(0l);
    quote.setAuthor(result.get("author").toString());
    quote.setQuote(result.get("quote").toString());
    quote.setBook(result.get("book").toString());

    return quote;
  }
}
