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
package services.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
    VertexAIClient is a service class that interacts with the Vertex AI Chat Client.

    This implementation leverages Spring AI's Vertex AI Chat Client to interact with the Vertex AI Chat API.
    Support for function calling is also demonstrated in this class.
 */
@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);

    private final VertexAiGeminiChatModel chatClient;

    public VertexAIClient(VertexAiGeminiChatModel chatClient){
        this.chatClient = chatClient;
    }

    public record ImageDetails(String title, String author){
    }

    // Multimedia prompt to retrieve information from an image
    // use entities to map responses to Objects
    public ImageDetails promptOnImage(Message systemMessage,
                                      Message userMessage,
                                      String model) {
        long start = System.currentTimeMillis();

        ChatClient client = ChatClient.create(chatClient);
        ImageDetails imageData = client.prompt()
                .advisors(new LoggingAdvisor(),
                          new GuardrailsAdvisor())
                .messages(List.of(systemMessage, userMessage))
                .options(VertexAiGeminiChatOptions.builder()
                        .withModel(model)
                        .build())
                .call()
                .entity(ImageDetails.class);
        logger.info("Multi-modal response: {}, {}", imageData.author, imageData.title);
        logger.info("Elapsed time ({}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));
        return imageData;
    }

    // prompt model with System and User Messages
    // return response as String
    public String promptModel(Message systemMessage,
                              Message userMessage,
                              String model) {
        return promptModelGrounded(systemMessage, userMessage, model, false);
    }

    // prompt model with System and User Messages
    // use grounding with Google web search | not
    // return response as String
    public String promptModelGrounded(Message systemMessage,
                                      Message userMessage,
                                      String model,
                                      boolean useGoogleWebSearch) {
        long start = System.currentTimeMillis();

        ChatClient client = ChatClient.create(chatClient);
        ChatResponse chatResponse = client.prompt()
                .advisors(new LoggingAdvisor(),
                          new GuardrailsAdvisor())
                .messages(List.of(systemMessage, userMessage))
                .options(VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .withMaxOutputTokens(8192)
                        .withTopK(3f)
                        .withTopP(0.5f)
                        .withModel(model)
                        .withGoogleSearchRetrieval(useGoogleWebSearch)
                        .build())
                .call()
                .chatResponse();

        logger.info("Elapsed time ( {}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));

        String output = "No response from model";
        if (chatResponse != null && chatResponse.getResult() != null) {  // Ensure chatResponse is not null
            output = chatResponse.getResult().getOutput().getContent();
        }
        logger.info("Chat model output: {} ...", output.substring(0, Math.min(1000, output.length())));
        return output;
    }

    // prompt model with Tool support
    public String promptModelWithFunctionCalls(Message systemMessage,
                                               Message userMessage,
                                               String functionName,
                                               String model) {
        long start = System.currentTimeMillis();

        ChatClient client = ChatClient.create(chatClient);
        ChatResponse chatResponse = client.prompt()
                .advisors(new LoggingAdvisor(),
                          new GuardrailsAdvisor())
                .messages(List.of(systemMessage, userMessage))
                .functions(functionName)
                .options(VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .withModel(model)
                        .build())
                .call()
                .chatResponse();

        logger.info("Elapsed time ({}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));

        String output = chatResponse.getResult().getOutput().getContent();
        logger.info("Chat Model output with Function Call: {}", output);
        return output;
    }

    public String promptModelWithMemory(Message systemMessage,
                                        Message userMessage,
                                        String model,
                                        ChatMemory chatMemory) {
        long start = System.currentTimeMillis();

        ChatClient client = ChatClient.create(chatClient);
        ChatResponse chatResponse = client.prompt()
                .advisors(new LoggingAdvisor(),
                          new MessageChatMemoryAdvisor(chatMemory),
                          new GuardrailsAdvisor())
                .messages(List.of(systemMessage, userMessage))
                .options(VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .withModel(model)
                        .build())
                .call()
                .chatResponse();

        logger.info("Elapsed time ( {}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));

        String output = "No response from model";
        if (chatResponse != null && chatResponse.getResult() != null) {  // Ensure chatResponse is not null
            output = chatResponse.getResult().getOutput().getContent();
        }
        logger.info("Chat model output: {} ...", output.substring(0, Math.min(1000, output.length())));
        return output;
    }

    private static class LoggingAdvisor implements RequestResponseAdvisor {
        private final Logger logger = LoggerFactory.getLogger(LoggingAdvisor.class);

        @Override
        public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
            logger.info("System text: \n{}", request.systemText());
            logger.info("System params: {}", request.systemParams());
            logger.info("User text: \n{}", request.userText());
            logger.info("User params:{}", request.userParams());
            logger.info("Function names: {}", request.functionNames());
            logger.info("Messages {}", request.messages());

            logger.info("Options: {}", request.chatOptions().toString());

            return request;
        }

        @Override
        public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
            logger.info("Response: {}", response);
            return response;
        }
    }

    private static class GuardrailsAdvisor implements RequestResponseAdvisor {
        private final Logger logger = LoggerFactory.getLogger(GuardrailsAdvisor.class);

        @Override
        public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
            // configure safety filters
            logger.info("Perform prompt safety check");

            // filter PII data
            logger.info("Mask PII data from prompt");

            return request;
        }

        @Override
        public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
            // filter inappropriate response
            // send notification in specific cases
            logger.info("Filter inappropriate response");

            return response;
        }
    }
}