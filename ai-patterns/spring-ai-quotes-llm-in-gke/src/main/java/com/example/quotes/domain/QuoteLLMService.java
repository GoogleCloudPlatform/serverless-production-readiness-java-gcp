package com.example.quotes.domain;

import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class QuoteLLMService {
  private VertexAiGeminiChatModel chatClient;
  private Environment env;

  public static final String VERTEX_AI_GEMINI_PROJECT_ID = "VERTEX_AI_GEMINI_PROJECT_ID";
  public static final String VERTEX_AI_GEMINI_LOCATION = "VERTEX_AI_GEMINI_LOCATION";
  public static final String VERTEX_AI_GEMINI_MODEL = "VERTEX_AI_GEMINI_MODEL";

  public QuoteLLMService(VertexAiGeminiChatModel chatClient, Environment env) {
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
        VertexAiGeminiChatOptions.builder()
            .withTemperature(0.4)
            .withModel(env.getProperty(VERTEX_AI_GEMINI_MODEL))
            .build())
    );
    Generation generation = chatResponse.getResult();
    String input = generation.getOutput().getContent();


    System.out.printf("\nGemini Model provided response: \n%s\n", input);

    return Quote.parseQuoteFromJson(input);
  }
}
