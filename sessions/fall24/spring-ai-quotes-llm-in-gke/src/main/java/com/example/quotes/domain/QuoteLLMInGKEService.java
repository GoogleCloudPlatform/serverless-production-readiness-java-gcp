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
  private QuoteEvalService evalService;
  private Environment env;

  public QuoteLLMInGKEService(OpenAiChatModel chatClient, 
                              QuoteEvalService evalService,
                              Environment env) {
    this.chatClient = chatClient;
    this.evalService = evalService;
    this.env = env;
  }

  public Quote findRandomQuote() {
    SystemMessage systemMessage = new SystemMessage("""
        You are a helpful AI assistant. 
        You are an AI assistant that helps people find information.
        """);
    UserMessage userMessage = new UserMessage("""
          Answer succinctly;
          please provide a quote from a random book, including book, quote and author; 
          do not repeat quotes from the same book; 
          return the answer wrapped in triple backquotes ```json```strictly in JSON format  
          return only 3 values, the book, the quote and the author, strictly in JSON format, wrapped in tripe backquotes ```json``` while eliminating every other text      
          """);

    ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
        OpenAiChatOptions.builder()
            .withTemperature(0.4f)
            .build())
    );

    Generation generation = chatResponse.getResult();
    String input = generation.getOutput().getContent();


    System.out.printf("\nLLM Model in GKE provided response: \n%s\n", input);

    // clear extraneous characters from LLM response
    Quote sanitizedQuote = Quote.parseQuoteFromJson(input);

    // validate against another LLM for veracity
    evalService.evaluate(sanitizedQuote.getQuote(), sanitizedQuote.getBook());

    return sanitizedQuote;
  }
}
