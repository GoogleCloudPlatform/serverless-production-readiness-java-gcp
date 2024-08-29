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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.Media;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.util.MimeTypeUtils;

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

    public ImageDetails promptOnImage(String prompt,
                                      String imageURL,
                                      String model) {
        long start = System.currentTimeMillis();

        // create User Message for AI framework
        var multiModalUserMessage = new UserMessage(prompt,
                List.of(new Media(MimeTypeUtils.parseMimeType("image/*"), imageURL)));

        ChatClient client = ChatClient.create(chatClient);
        ImageDetails imageData = client.prompt()
                .advisors(new LoggingAdvisor())
                .messages(multiModalUserMessage)
                .call()
                .entity(ImageDetails.class);
        logger.info("Multi-modal response: {}, {}", imageData.author, imageData.title);
        logger.info("Elapsed time ({}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));
        return imageData;
    }
    public String promptModel(String prompt, String model) {
        long start = System.currentTimeMillis();
        logger.info("Chat model prompt: {} ...", prompt.substring(0, Math.min(500, prompt.length())));

        int maxRetries = 3;  // Set the maximum number of retry attempts
        int retryCount = 0;
        ChatResponse chatResponse = null;

        while (retryCount < maxRetries) {
            try {
                chatResponse = chatClient.call(new Prompt(prompt,
                        VertexAiGeminiChatOptions.builder()
                                .withTemperature(0.4f)
                                .withModel(model)
                                .build())
                );

                // Check if the response is valid before exiting the loop
                if (chatResponse.getResult() != null) {
                    break; // Exit the loop if we have a valid response
                } else {
                    logger.warn("Received invalid response from model. Retrying...");
                }

            } catch (Exception exception) {
                logger.error("Error calling chat model: ", exception);
            }

            retryCount++;
        }

        logger.info("Elapsed time ( {}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));

        String output = VertexModels.RETRY_MSG;
        if (chatResponse != null && chatResponse.getResult() != null) {  // Ensure chatResponse is not null
            output = chatResponse.getResult().getOutput().getContent();
        }

        logger.info("Chat model output: {} ...", output.substring(0, Math.min(1000, output.length())));
        return output;
    }

    public String promptModel(Message systemMessage,
                              Message userMessage,
                              String model) {
        long start = System.currentTimeMillis();
        ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
                VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .withModel(model)
                        .build()));
        logger.info("Elapsed time ( {}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));

        String output = "No response from model";
        if (chatResponse != null && chatResponse.getResult() != null) {  // Ensure chatResponse is not null
            output = chatResponse.getResult().getOutput().getContent();
        }
        logger.info("Chat model output: {} ...", output.substring(0, Math.min(1000, output.length())));
        return output;
    }

    public String promptModelWithFunctionCalls(Message systemMessage,
                                               Message userMessage,
                                               String functionName,
                                               String model) {
        long start = System.currentTimeMillis();

        ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
                VertexAiGeminiChatOptions.builder()
                        .withModel(model)
                        .withFunction(functionName)
                        .build()));

        logger.info("Elapsed time ({}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));

        String output = chatResponse.getResult().getOutput().getContent();
        logger.info("Chat Model output with Function Call: {}", output);
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

            logger.info("Options: {}", request.chatOptions().toString());

            return request;
        }

        @Override
        public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
            logger.info("Response: {}", response);
            return response;
        }
    }
}