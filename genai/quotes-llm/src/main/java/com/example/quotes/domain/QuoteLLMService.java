package com.example.quotes.domain;

import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class QuoteLLMService {
  private GoogleGenAiChatModel chatClient;
  private Environment env;

  public QuoteLLMService(GoogleGenAiChatModel chatClient, Environment env) {
    this.chatClient = chatClient;
    this.env = env;
  }

  public Quote findRandomQuote() {
    SystemMessage systemMessage = new SystemMessage("""
        You are a helpful AI assistant. 
        You are an AI assistant that helps people find information.
        You should reply to the user's request with your name and also in the style of a literary professor.
        """);
    UserMessage userMessage = new UserMessage("""
        Answer precisely; please provide a quote from a random book, 
        including only book, quote and author; do not repeat quotes from the same book
        return only 3 values, the book, the quote and the author, strictly in JSON format, wrapped in tripe backquotes ```json```, while eliminating every other text
        """);

    ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
        GoogleGenAiChatOptions.builder()
            .build())
    );
    Generation generation = chatResponse.getResult();
    String input = generation.getOutput().getText();


    System.out.printf("\nGemini Model provided response: \n%s\n", input);

    return Quote.parseQuoteFromJson(input);
  }
}
