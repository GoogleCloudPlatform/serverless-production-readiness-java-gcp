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
package services;

import com.google.cloud.vertexai.Transport;
import com.google.cloud.vertexai.VertexAI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles(value = "test")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_PROJECT_ID", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_LOCATION", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_MODEL", matches = ".*")
public class SummarizationTests {

    @Autowired
    private VertexAiGeminiChatModel chatModel;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @Value("classpath:/prompts/system-summary-message.st")
    private Resource systemSummaryResource;

    @Value("classpath:/prompts/initial-message.st")
    private Resource initialResource;

    @Value("classpath:/prompts/refine-message.st")
    private Resource resourceResource;

    @Value("classpath:/books/The_Wasteland-TSEliot-public.txt")
    private Resource resource;

    @Value("classpath:/prompts/subsummary-message.st")
    private Resource subsummaryResource;

    @Value("classpath:/prompts/subsummary-overlap-message.st")
    private Resource subsummaryOverlapResource;

    @Value("classpath:/prompts/summary-message.st")
    private Resource summaryResource;

    private static final int CHUNK_SIZE = 10000;  // Number of words in each window
    private static final int OVERLAP_SIZE = 2000;

    @Test
    public void summarizationStuffTest(){
        TextReader textReader = new TextReader(resource);
        String bookTest = textReader.get().getFirst().getContent();

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "Gemini", "voice", "literary critic"));

        PromptTemplate userPromptTemplate = new PromptTemplate(initialResource,Map.of("content", bookTest));
        Message userMessage = userPromptTemplate.createMessage();

        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(new Prompt(List.of(userMessage, systemMessage),
            VertexAiGeminiChatOptions.builder()
                .withTemperature(0.2f)
                .build()));

        System.out.println(response.getResult().getOutput().getContent());
        System.out.print("Summarization (stuffing test) took " + (System.currentTimeMillis() - start) + " milliseconds");
    }

    @Test
    public void summarizationChunkRefineTest(){
        TextReader textReader = new TextReader(resource);
        String bookTest = textReader.get().getFirst().getContent();

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemSummaryResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        long start = System.currentTimeMillis();
        int length = bookTest.length();
        String subcontext;
        String context = "";
        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, length);
            String chunk = bookTest.substring(i, end);

            // Process the chunk here
            subcontext = processChunk(context, chunk, systemMessage);
            context += "\n"+subcontext;
        }

        String output = processSummary(context, systemMessage);

        System.out.println(output);
        System.out.print("Summarization (refine, with chunking test) took " + (System.currentTimeMillis() - start) + " milliseconds");
    }


    @Test
    public void summarizationChunkRefineOverlappingWindowsTest(){
        TextReader textReader = new TextReader(resource);
        String bookTest = textReader.get().getFirst().getContent();

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemSummaryResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        long start = System.currentTimeMillis();
        int length = bookTest.length();
        String subcontext;
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < length; i += CHUNK_SIZE-OVERLAP_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, length);
            String chunk = bookTest.substring(i, end);

            // Process the chunk here
            subcontext = processChunk("", chunk, systemMessage);
            context.append("\n").append(subcontext);
        }

        String output = processSummary(context.toString(), systemMessage);

        System.out.println(output);
        System.out.print("Summarization (refine, with overlapping chunking test) took " + (System.currentTimeMillis() - start) + " milliseconds");
    }

    @Test
    public void summarizationChunkMapReduceTest() throws Exception {
        TextReader textReader = new TextReader(resource);
        String bookText = textReader.get().getFirst().getContent();
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemSummaryResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        long startTime = System.currentTimeMillis();
        int length = bookText.length();
        List<CompletableFuture<Map<Integer, String>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Map<Integer, String> resultMap = new TreeMap<>(); // TreeMap to automatically sort by key

        for (int i = 0; i < length; i += (CHUNK_SIZE - OVERLAP_SIZE)) {
            final int index = i / (CHUNK_SIZE - OVERLAP_SIZE); // Calculate chunk index
            int end = Math.min(i + CHUNK_SIZE, length);
            String chunk = bookText.substring(i, end);

            CompletableFuture<Map<Integer, String>> future = CompletableFuture.supplyAsync(() -> processChunk(index,
                chunk, systemMessage), executor);
            futures.add(future);
        }

        // Wait for all futures to complete and collect the results in resultMap
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> futures.forEach(f -> f.thenAccept(resultMap::putAll)));

        allDone.get(); // Wait for all processing to complete

        // Build the final context string from the sorted entries in the resultMap
        StringBuilder contextBuilder = new StringBuilder();
        for (Map.Entry<Integer, String> entry : resultMap.entrySet()) {
//            System.out.println("Index " + entry.getKey() + ": " + entry.getValue());
            contextBuilder.append(entry.getValue()).append("\n");
        }

        String context = contextBuilder.toString();
        String output = processSummary(context, systemMessage);
        System.out.println(output);
        System.out.print("Summarization (map-reduce) took " + (System.currentTimeMillis() - startTime) + " milliseconds");

        executor.shutdown(); // Shutdown the executor
    }

    private String processSummary(String context, Message systemMessage) {
        long start = System.currentTimeMillis();
        System.out.println(context+"\n\n");
        PromptTemplate userPromptTemplate = new PromptTemplate(summaryResource,Map.of("content", context));
        Message userMessage = userPromptTemplate.createMessage();

        ChatResponse response = chatModel.call(new Prompt(List.of(userMessage, systemMessage),
            VertexAiGeminiChatOptions.builder()
                .withTemperature(0.2f)
                .build()));
        System.out.println("Summarization (final summary) took " + (System.currentTimeMillis() - start) + " milliseconds");
        return response.getResult().getOutput().getContent();
    }


    private Map<Integer, String> processChunk(Integer index, String chunk, Message systemMessage) {

        Map<Integer, String> outputWithIndex = new HashMap<>();
        String output = processChunk("", chunk, systemMessage);
        outputWithIndex.put(index, output);
        return outputWithIndex;
    }

    private String processChunk(String context, String chunk, Message systemMessage) {
        long start = System.currentTimeMillis();
        PromptTemplate userPromptTemplate;
        if(context.trim().isEmpty()) {
            userPromptTemplate = new PromptTemplate(subsummaryOverlapResource, Map.of("content", chunk));
        } else {
            userPromptTemplate = new PromptTemplate(subsummaryResource, Map.of("context", context, "content", chunk));
        }
        Message userMessage = userPromptTemplate.createMessage();

        ChatResponse response = chatModel.call(new Prompt(List.of(userMessage, systemMessage),
            VertexAiGeminiChatOptions.builder()
                .withTemperature(0.2f)
                .build()));
        System.out.println("Summarization (single chunk) took " + (System.currentTimeMillis() - start) + " milliseconds");
        String output = response.getResult().getOutput().getContent();
        System.out.println(output+"\n\n");
        return output;
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
        public VertexAiGeminiChatModel vertexAiEmbedding(VertexAI vertexAi) {
            String model = System.getenv("VERTEX_AI_GEMINI_MODEL");
            return new VertexAiGeminiChatModel(vertexAi,
                VertexAiGeminiChatOptions.builder()
                    .withModel(model)
                    .build());
        }
    }
}
