/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services;

import com.google.cloud.vertexai.Transport;
import com.google.cloud.vertexai.VertexAI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Disabled;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

@Disabled
@SpringBootTest
@ActiveProfiles(value = "test")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_PROJECT_ID", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_LOCATION", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_MODEL", matches = ".*")
public class TerraformTransformationTests {

    @Autowired
    private VertexAiGeminiChatModel chatClient;

    @Value("classpath:/prompts/prompt-tf-transform-message.st")
    private Resource promptTerraformTransform;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    private String model;

    @Value("classpath:/bashscripts/provision-cloud-infra.sh")
    private Resource bashscript;

    private static final int CHUNK_SIZE = 10000;  // Number of words in each window
    private static final int OVERLAP_SIZE = 2000;

    @Test
    public void terraformTransformTest(){
        TextReader textReader = new TextReader(bashscript);
        String script = textReader.get().getFirst().getContent();


        PromptTemplate userPromptTemplate = new PromptTemplate(promptTerraformTransform, Map.of("script", script));
        Message userMessage = userPromptTemplate.createMessage();
        System.out.println(userMessage.getContent());
        long start = System.currentTimeMillis();
        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage),
            VertexAiGeminiChatOptions.builder()
                .withTemperature(0.4f)
                .build()));

        System.out.println(response.getResult().getOutput().getContent());
        System.out.print("Transformation took " + (System.currentTimeMillis() - start) + " milliseconds");
    }


     @SpringBootConfiguration
    public static class TestConfiguration {

        @Bean
        public VertexAI vertexAiApi() {
            String projectId = System.getenv("VERTEX_AI_GEMINI_PROJECT_ID");
            String location = System.getenv("VERTEX_AI_GEMINI_LOCATION");
            return new VertexAI.Builder().setProjectId(projectId)
                .setLocation(location)
                .setTransport(Transport.REST)
                .build();
        }

        @Bean
        public VertexAiGeminiChatModel vertexAiGeminiChatModel(VertexAI vertexAi) {
            String model = System.getenv("VERTEX_AI_GEMINI_MODEL");

            return new VertexAiGeminiChatModel(vertexAi,
                VertexAiGeminiChatOptions.builder()
                    .withModel(model)
                    .build()
            );
        }
    }
}
