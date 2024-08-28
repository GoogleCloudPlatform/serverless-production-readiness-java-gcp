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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.Media;
import org.springframework.ai.vertexai.gemini.MimeTypeDetector;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import org.springframework.util.MimeTypeUtils;

/*
    VertexAIClient is a service class that interacts with the Vertex AI Chat Client.

    This implementation leverages Spring AI's Vertex AI Chat Client to interact with the Vertex AI Chat API.
    Support for function calling is also demonstrated in this class.
 */
@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);

    private VertexAiGeminiChatModel chatClient;
    private Environment env;

    public VertexAIClient(VertexAiGeminiChatModel chatClient, Environment env){
        this.chatClient = chatClient;
        this.env = env;
    }

    public String promptOnImage(String prompt,
                                String bucketName,
                                String fileName,
                                String model) throws IOException {
        long start = System.currentTimeMillis();

        // bucket where image has been uploaded
        String imageURL = String.format("gs://%s/%s",bucketName, fileName);

        // create User Message for AI framework
        var multiModalUserMessage = new UserMessage(prompt,
                List.of(new Media(MimeTypeUtils.parseMimeType("image/*"), imageURL)));

        // call the model of choice
        ChatResponse multiModalResponse = chatClient.call(new Prompt(List.of(multiModalUserMessage),
                VertexAiGeminiChatOptions.builder()
                        .withModel(model)
                        .build()));
        String response = multiModalResponse.getResult().getOutput().getContent();
        logger.info("Multi-modal response: " + response);

        // response from Vertex is in Markdown, remove annotations
        response = response.replaceAll("```json", "").replaceAll("```", "").replace("'", "\"");

        logger.info("Elapsed time ({}, with SpringAI): {} ms", model, (System.currentTimeMillis() - start));
        return response;
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

    public String promptModelwithFunctionCalls(SystemMessage systemMessage,
                                               UserMessage userMessage,
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
        logger.info("Chat Model output with Function Call: " + output);
        return output;
    }

}