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

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Blob;
import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Service;
import services.config.CloudConfig;
import services.utility.CloudUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);

    private VertexAiGeminiChatClient chatClient;

    public VertexAIClient(VertexAiGeminiChatClient chatClient){
        this.chatClient = chatClient;
    }

    public GenerateContentResponse promptOnImage(byte[] image) throws IOException {
        return promptOnImage(image, "");
    }

    public GenerateContentResponse promptOnImage(byte[] image, String prompt) throws IOException {
        long start = System.currentTimeMillis();

        GenerateContentResponse response = null;
        if(prompt== null ||prompt.isBlank())
            prompt = "Extract the book name, labels, main color and author strictly in JSON format.";
        String location = CloudUtility.extractRegion(CloudConfig.zone);
        try (VertexAI vertexAI = new VertexAI(CloudConfig.projectID, location)) {
            GenerationConfig generationConfig =
                    GenerationConfig.newBuilder()
                            .setMaxOutputTokens(2048)
                            .setTemperature(0.4F)
                            .setTopK(32)
                            .setTopP(1F)
                            .build();
            GenerativeModel model = new GenerativeModel(VertexModels.GEMINI_PRO_VISION_VERSION, generationConfig, vertexAI);
            List<SafetySetting> safetySettings = Arrays.asList(
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build(),
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build(),
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build(),
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build()
            );
            List<Content> contents = new ArrayList<>();
            contents.add(Content.newBuilder().setRole("user").addParts(Part.newBuilder().setInlineData(Blob.newBuilder().setMimeType("image/png")
                            .setData(ByteString.copyFrom(image))))
                    .addParts(Part.newBuilder().setText(prompt))
                    .build());
            response = model.generateContent(contents, safetySettings);
            logger.info(response.toString());
        }
        logger.info("Elapsed time (chat model): " + (System.currentTimeMillis() - start) + "ms");        
        return response;
    }

    public String promptModel(String prompt, String modelName) {
        long start = System.currentTimeMillis();
        ChatResponse chatResponse = chatClient.call(new Prompt(prompt,
                                                    VertexAiGeminiChatOptions.builder()
                                                        .withTemperature(0.4f)
                                                        .withModel(VertexModels.GEMINI_PRO)
                                                        .build())
                );
        logger.info("Elapsed time (chat model, with SpringAI): " + (System.currentTimeMillis() - start) + "ms");

        String output = chatResponse.getResult().getOutput().getContent();
        logger.info(output);
        return output;
    }

}
