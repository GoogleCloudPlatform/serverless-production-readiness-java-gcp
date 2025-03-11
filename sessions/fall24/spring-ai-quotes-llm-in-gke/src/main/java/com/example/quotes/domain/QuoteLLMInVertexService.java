package com.example.quotes.domain;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Date;
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
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class QuoteLLMInVertexService {
  private Environment env;

  public QuoteLLMInVertexService(Environment env) {
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

    // ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
    //     VertexAiGeminiChatOptions.builder()
    //         .withTemperature(0.4f)
    //         .withModel(env.getProperty("spring.ai.openai.vertex.ai.chat.options.model"))
    //         .build())
    // );
    // Generation generation = chatResponse.getResult();
    // String input = generation.getOutput().getContent();
    String baseURL = env.getProperty("spring.ai.openai.vertex.ai.chat.base-url");
    String completionsPath = env.getProperty("spring.ai.openai.vertex.ai.chat.completions-path");
    String model = env.getProperty("spring.ai.openai.vertex.ai.chat.options.model");
    String token = null;
    try {
      token = getOauth2Token(baseURL + completionsPath);
    } catch (IOException e) {
      Quote quote = new Quote();
      quote.setQuote("Quote generationm failure; please retry");
      quote.setAuthor("N/A");
      quote.setBook("N/A");
      return quote;
    }

    OpenAiApi openAiApi = new OpenAiApi(baseURL, token, completionsPath,
        "/v1/embeddings",
        RestClient.builder(),
        WebClient.builder(),
        RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

    OpenAiChatModel openAIGemini = new OpenAiChatModel(openAiApi);
    OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
        .temperature(0.2)
        .model(model)
        .build();

    long start = System.currentTimeMillis();
    String input = openAIGemini.call(
            new Prompt(List.of(systemMessage,userMessage), openAiChatOptions))
            .getResult().getOutput().getText();

    System.out.printf("\nLLM Model in VertexAI provided response: \n%s\n", input);
    System.out.printf("Call took %s ms", (System.currentTimeMillis() - start));

    return Quote.parseQuoteFromJson(input);
  }

  private static String getOauth2Token(String target) throws IOException {
    // Load credentials from the environment (default)
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

    // Refresh if necessary
    if (credentials.getAccessToken() == null || credentials.getAccessToken().getExpirationTime().before(new Date())) {
      credentials.refresh();
    }

    // Get the access token
    AccessToken accessToken = credentials.getAccessToken();
    System.out.println("Generated short-lived Access Token: " + accessToken.getTokenValue());

    return accessToken.getTokenValue();
  }
}
