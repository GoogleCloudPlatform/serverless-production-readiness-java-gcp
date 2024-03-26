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
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.MimeTypeDetector;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

/*
    VertexAIClient is a service class that interacts with the Vertex AI Chat Client.

    This implementation leverages Spring AI's Vertex AI Chat Client to interact with the Vertex AI Chat API.
    Support for function calling is also demonstrated in this class.
 */
@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);

    private VertexAiGeminiChatClient chatClient;

    public VertexAIClient(VertexAiGeminiChatClient chatClient){
        this.chatClient = chatClient;
    }

    public String promptOnImage(String prompt,
        String bucketName,
        String fileName) throws IOException {
        long start = System.currentTimeMillis();

        // bucket where image has been uploaded
        String imageURL = String.format("gs://%s/%s",bucketName, fileName);

        // create User Message for AI framework
        var multiModalUserMessage = new UserMessage(prompt,
            List.of(new Media(MimeTypeDetector.getMimeType(imageURL), imageURL)));

        // call the model of choice
        ChatResponse multiModalResponse = chatClient.call(new Prompt(List.of(multiModalUserMessage),
            VertexAiGeminiChatOptions.builder()
                .withModel(VertexModels.GEMINI_PRO_VISION).build()));
        String response = multiModalResponse.getResult().getOutput().getContent();
        logger.info("Multi-modal response: " + response);

        // response from Vertex is in Markdown, remove annotations
        response = response.replaceAll("```json", "").replaceAll("```", "").replace("'", "\"");

        logger.info("Elapsed time (chat model): " + (System.currentTimeMillis() - start) + "ms");

        // return the response in String format, extract values in caller
        return response;
    }

    public String promptModel(String prompt) {
        long start = System.currentTimeMillis();

        ChatResponse chatResponse = chatClient.call(new Prompt(prompt,
            VertexAiGeminiChatOptions.builder()
                .withTemperature(0.4f)
                .withModel(VertexModels.GEMINI_PRO)
                .build())
        );
        logger.info("Elapsed time (chat model, with SpringAI): " + (System.currentTimeMillis() - start) + "ms");
        String output = "Failed to fetch.. Please try again!";
        if(chatResponse.getResult()!=null) {
            output = chatResponse.getResult().getOutput().getContent();
        }
        logger.info("Chat Model output: " + output);

        // return model response in String format
        return output;
    }

    public String promptModelwithFunctionCalls(SystemMessage systemMessage,
                                               UserMessage userMessage,
                                               String functionName) {
        long start = System.currentTimeMillis();

        ChatResponse chatResponse = chatClient.call(new Prompt(List.of(systemMessage, userMessage),
                                                    VertexAiGeminiChatOptions.builder()
                                                        .withModel("gemini-pro")
                                                        .withFunction(functionName).build()));
        
        logger.info("Elapsed time (chat model, with SpringAI): " + (System.currentTimeMillis() - start) + "ms");

        String output = chatResponse.getResult().getOutput().getContent();
        logger.info("Chat Model output with Function Call: " + output);

        // return model response in String format
        return output;
    }

}
