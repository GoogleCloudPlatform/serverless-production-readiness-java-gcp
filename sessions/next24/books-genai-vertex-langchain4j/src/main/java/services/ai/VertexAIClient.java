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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
    VertexAIClient is a service class that interacts with the Vertex AI Chat Client.

    This implementation leverages Langchain4J's Vertex AI Chat Client to interact with the Vertex AI Chat API.
    Support for function calling is also demonstrated in this class.
 */
@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String project;

    @Value("${spring.ai.vertex.ai.gemini.location}")
    private String location;

    public String promptOnImage(String prompt,
                                String bucketName,
                                String fileName) throws IOException {
        long start = System.currentTimeMillis();

        // bucket where image has been uploaded
        String imageURL = String.format("gs://%s/%s",bucketName, fileName);

        UserMessage userMessage = UserMessage.from(
            ImageContent.from(imageURL),
            TextContent.from(prompt)
        );

        ChatLanguageModel visionModel = VertexAiGeminiChatModel.builder()
            .project(project)
            .location(location)
            .modelName(VertexModels.GEMINI_PRO_VISION)
            .build();

        Response<AiMessage> multiModalResponse = visionModel.generate(userMessage);
        String response = multiModalResponse.content().text();
        logger.info("Multi-modal response: " + response);

        // response from Vertex is in Markdown, remove annotations
        response = response.replaceAll("```json", "").replaceAll("```", "").replace("'", "\"");

        logger.info("Elapsed time (gemini-pro-vision, with Langchain4J): " + (System.currentTimeMillis() - start) + "ms");

        // return the response in String format, extract values in caller
        return response;
    }

    public String promptModel(String prompt) {
        long start = System.currentTimeMillis();
        logger.info("Chat model prompt: {} ...",  prompt.substring(0, Math.min(500, prompt.length())));

        ChatLanguageModel model = VertexAiGeminiChatModel.builder()
            .project(project)
            .location(location)
            .modelName(VertexModels.GEMINI_PRO)
            .build();
            
        // prompt Chat model
        String output = model.generate(prompt);

        logger.info("Elapsed time (gemini-pro, with Langchain4J): " + (System.currentTimeMillis() - start) + "ms");
        logger.info("Chat model output: {} ...", output.substring(0, Math.min(1000, prompt.length())));
        // return model response in String format
        return output;
    }

    interface Assistant {
        @SystemMessage("""
            Use Multi-turn function calling.
            Answer with precision.
            If the information was not fetched call the function again. Repeat at most 3 times.
            """)
        String chat(UserMessage userMessage);
    }
    public String promptModelwithFunctionCalls(UserMessage userMessage,
                                               Object function) {
        long start = System.currentTimeMillis();

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        ChatLanguageModel model = VertexAiGeminiChatModel.builder()
            .project(project)
            .location(location)
            .modelName(VertexModels.GEMINI_PRO)
            .build();

        Assistant assistant = AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .chatMemory(chatMemory)
            .tools(function)
            .build();

        String output = assistant.chat(userMessage);

        logger.info("Elapsed time (gemini-pro, with Langchain4J): " + (System.currentTimeMillis() - start) + "ms");
        logger.info("Chat Model output with Function Call: " + output);

        // return model response in String format
        return output;
    }

}
