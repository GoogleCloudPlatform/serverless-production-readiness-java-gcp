/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.MediaData;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.ai.vertexai.gemini.MimeTypeDetector;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import reactor.core.publisher.Flux;

import services.actuator.StartupCheck;
import services.ai.VertexAIClient;
import services.ai.VertexModels;
import services.domain.BooksService;
import services.domain.CloudStorageService;
import services.domain.FirestoreService;
import services.web.data.BookRequest;

@RestController
@RequestMapping("/geminianalysis")
public class BookAnalysisGeminiController {
  private static final Logger logger = LoggerFactory.getLogger(BookAnalysisGeminiController.class);

  private final FirestoreService eventService;
  private BooksService booksService;
  private VertexAIClient vertexAIClient;
  private Environment environment;
  private CloudStorageService cloudStorageService;
  private VertexAiGeminiChatClient chatSpringClient;

  @Value("${prompts.bookanalysis}")
  private String prompt;

  public BookAnalysisGeminiController(FirestoreService eventService, 
                                      BooksService booksService, 
                                      VertexAIClient vertexAIClient, 
                                      Environment environment, 
                                      CloudStorageService cloudStorageService,
                                      VertexAiGeminiChatClient chatSpringClient) {
    this.eventService = eventService;
    this.booksService = booksService;
    this.vertexAIClient = vertexAIClient;
    this.environment = environment;
    this.cloudStorageService = cloudStorageService;
    this.chatSpringClient = chatSpringClient;
  }

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: BookAnalysisController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: BookAnalysisController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @PostMapping("")
  public ResponseEntity<String> processUserRequest(@RequestBody BookRequest bookRequest, 
                                                   @RequestParam(name = "contentCharactersLimit", defaultValue = "6000") Integer contentCharactersLimit) throws IOException{
    long start = System.currentTimeMillis();
    prompt = "Extract the main ideas from the book The Jungle Book by Rudyard Kipling";
    ChatResponse chatResponse = chatSpringClient.call(new Prompt(prompt,
                                                      VertexAiGeminiChatOptions.builder()
                                                          .withTemperature(0.4f)
                                                          .withModel(VertexModels.GEMINI_PRO)
                                                          .build())
            );
    System.out.println("Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
    System.out.println("SYNC RESPONSE: " + chatResponse.getResult().getOutput().getContent());

    System.out.println("Starting ASYNC call...");
    Flux<ChatResponse> flux = chatSpringClient.stream(new Prompt(prompt));
    String fluxResponse = flux.collectList().block().stream().map(r -> r.getResult().getOutput().getContent())
            .collect(Collectors.joining());
    System.out.println("ASYNC RESPONSE: " + fluxResponse);


    String imageURL = "gs://library_next24_images/TheJungleBook.jpg";
    var multiModalUserMessage = new UserMessage("Extract the author and title from the book cover. Return the response as a Map, remove the markdown annotations",
        List.of(new MediaData(MimeTypeDetector.getMimeType(imageURL), imageURL)));

    ChatResponse multiModalResponse = chatSpringClient.call(new Prompt(List.of(multiModalUserMessage),
            VertexAiGeminiChatOptions.builder().withModel(VertexModels.GEMINI_PRO_VISION).build()));

    System.out.println("MULTI-MODAL RESPONSE: " + multiModalResponse.getResult().getOutput().getContent());

    String response = removeMarkdownTags(multiModalResponse.getResult().getOutput().getContent());
    System.out.println("Response without Markdown: " + response);
    var outputParser = new BeanOutputParser<>(BookData.class);
    BookData bookData = outputParser.parse(response);
    System.out.println("Book title: " + bookData.title());
    System.out.println("Book author: " + bookData.author());

    //-----------------------
    // Functiop calling BookStoreService
    // var systemMessage = new SystemMessage("""
    //         Use Multi-turn function calling.
    //         Answer with precision.
    //         If the information was not fetched call the function again. Repeat at most 3 times.
    //         """);
    // UserMessage userMessage = new UserMessage(
    //     String.format("Please find out if the book with the title %s by author %s is available in the University bookstore.",
    //                     bookData.title(), bookData.author()));

    // ChatResponse bookStoreResponse = chatSpringClient.call(new Prompt(List.of(systemMessage, userMessage),
    //     VertexAiGeminiChatOptions.builder().withFunction("bookStoreAvailability").build()));
    // System.out.println("Book availability: " + bookStoreResponse.getResult().getOutput().getContent());
    //-----------------------

    // Function calling
    var systemMessage = new SystemMessage("""
      Use Multi-turn function calling.
      Answer for all listed locations.
      If the information was not fetched call the function again. Repeat at most 3 times.
      """);
    UserMessage userMessage = new UserMessage(
            "What's the weather like in San Francisco, in Paris and in Tokyo?");

    ChatResponse weatherResponse = chatSpringClient.call(new Prompt(List.of(systemMessage, userMessage),
            VertexAiGeminiChatOptions.builder().withFunction("weatherInfo").build()));
    System.out.println("WEATHER RESPONSE: " + weatherResponse.getResult().getOutput().getContent());

    return new ResponseEntity<String>(chatResponse.getResult().getOutput().getContent(), HttpStatus.OK);
  }


  public static String removeMarkdownTags(String text) {
    // String response = text.replaceAll("```json", " ");
    // return response = response.replaceAll("```", " ");
    return text.replaceAll("```json", "").replaceAll("```", "").replace("'", "\"");
  }

  public record BookData(String author, String title) {

  }

  @Bean
  @Description("Get the weather in location")
  public Function<MockWeatherService.Request, MockWeatherService.Response> weatherInfo() {
      return new MockWeatherService();
  }

      public static class MockWeatherService
            implements Function<MockWeatherService.Request, MockWeatherService.Response> {

        @JsonInclude(Include.NON_NULL)
        @JsonClassDescription("Weather API request")
        public record Request(
                @JsonProperty(required = true, value = "location") @JsonPropertyDescription("The city and state e.g. San Francisco, CA") String location,
                @JsonProperty(required = true, value = "unit") @JsonPropertyDescription("Temperature unit") Unit unit) {
        }

        public enum Unit {
            C, F;
        }

        @JsonInclude(Include.NON_NULL)
        public record Response(double temperature, double feels_like, double temp_min, double temp_max, int pressure,
                int humidity, Unit unit) {
        }

        @Override
        public Response apply(Request request) {
            System.out.println("Weather request: " + request);
            return new Response(11, 15, 20, 2, 53, 45, Unit.C);
        }

    }

    // //-----------------------
    // @Bean
    // @Description("Get availability of book in the book store")
    // public Function<BookStoreService.Request, BookStoreService.Response> bookStoreAvailability() {
    //     return new BookStoreService();
    // }
    // public static class BookStoreService
    //     implements Function<BookStoreService.Request, BookStoreService.Response> {

    //     @JsonInclude(Include.NON_NULL)
    //     @JsonClassDescription("BookStore API Request")
    //     public record Request(
    //         @JsonProperty(required = true, value = "title") @JsonPropertyDescription("The title of the book") String title,
    //         @JsonProperty(required = true, value = "author") @JsonPropertyDescription("The author of the book") String author) {
    //     }
    //     @JsonInclude(Include.NON_NULL)
    //     public record Response(String title, String author, String availability) {
    //     }

    //     @Override
    //     public Response apply(Request request) {
    //         System.out.println("BookStore availability request: " + request);
    //         return new Response(request.title(), request.author(), "The book is available for purchase in the book store in hard copy");
    //     }
    // }

    // public static class BookStoreServiceRuntimeHints implements RuntimeHintsRegistrar {
    //     @Override
    //     public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    //         // Register method for reflection
    //         var mcs = MemberCategory.values();
    //         hints.reflection().registerType(BookStoreService.Request.class, mcs);
    //         hints.reflection().registerType(BookStoreService.Response.class, mcs);
    //     }

    // }
    // //-----------------------    

}
