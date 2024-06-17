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

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.service.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(value = "test")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_PROJECT_ID", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_LOCATION", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_MODEL", matches = ".*")
public class SummarizationTests {

    @Autowired
    private VertexAiGeminiChatModel chatModel;

    @Value("classpath:/books/The_Wasteland-TSEliot-public.txt")
    private Path resource;

    private static final int CHUNK_SIZE = 10000;  // Number of words in each window
    private static final int OVERLAP_SIZE = 200;

    public interface StuffingSummarizationAssistant {
        @SystemMessage("""
        You are a helpful AI assistant.
        You are an AI assistant that helps people summarize information.
        Your name is Gemini
        You should reply to the user's request with your name and also in the style of a literary critic
        Strictly ignore Project Gutenberg & ignore copyright notice in summary output.
        """)
        @UserMessage("""
        Please provide a concise summary covering the key points of the following text.
                  TEXT: {{content}}
        """)
        String summarize(@V("content") String content);
    }

    // --- Summarization using Stuffing ---
    @Test
    public void summarizationStuffTest() {
        Document document = loadDocument(resource, new TextDocumentParser());

        long start = System.currentTimeMillis();

        StuffingSummarizationAssistant assistant = AiServices.create(StuffingSummarizationAssistant.class, chatModel);
        String response = assistant.summarize(document.text());

        System.out.println(response);
        System.out.printf("Summarization (stuffing test) took %d milliseconds", (System.currentTimeMillis() - start));
    }

    // --- Summarization using Refine with NO overlapping chunks ---
    @Test
    public void summarizationChunkRefineTest(){
        Document document = loadDocument(resource, new TextDocumentParser());

        // Overlap set to 0
        DocumentSplitter splitter = new DocumentByParagraphSplitter(CHUNK_SIZE, 0);
        List<TextSegment> segments = splitter.split(document);

        long start = System.currentTimeMillis();

        StringBuilder context = new StringBuilder();
        segments.forEach(segment -> processChunk(context, segment.text()));

        String output = processSummary(context.toString());

        System.out.println(output);
        System.out.printf("\nChunks: %d, Chunk size: %d, Overlap size: %d", segments.size(), CHUNK_SIZE, OVERLAP_SIZE);
        System.out.printf("\nSummarization (refine, with NO overlapping chunking test) took %d milliseconds", (System.currentTimeMillis() - start));
    }

    // --- Summarization using Refine with overlapping chunks ---
    @Test
    public void summarizationChunkRefineOverlappingWindowsTest(){
        Document document = loadDocument(resource, new TextDocumentParser());

        DocumentSplitter splitter = new DocumentByParagraphSplitter(CHUNK_SIZE, OVERLAP_SIZE);
        List<TextSegment> segments = splitter.split(document);

        long start = System.currentTimeMillis();

        StringBuilder context = new StringBuilder();
        segments.forEach(segment -> processChunk(context, segment.text()));

        String output = processSummary(context.toString());

        System.out.println(output);
        System.out.printf("\nChunks: %d, Chunk size: %d, Overlap size: %d", segments.size(), CHUNK_SIZE, OVERLAP_SIZE);
        System.out.printf("\nSummarization (refine, with overlapping chunking test) took %d milliseconds", (System.currentTimeMillis() - start));
    }

    @Test
    public void summarizationChunkMapReduceTest() throws Exception {
        Document document = loadDocument(resource, new TextDocumentParser());

        DocumentSplitter splitter = new DocumentByParagraphSplitter(CHUNK_SIZE, OVERLAP_SIZE);
        List<TextSegment> segments = splitter.split(document);

        long start = System.currentTimeMillis();

        List<CompletableFuture<Map<Integer, String>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Map<Integer, String> resultMap = new TreeMap<>(); // TreeMap to automatically sort by key

        StringBuilder context = new StringBuilder();
        // segments.forEach(segment -> processChunk(context, segment.text()));

        for(int i = 0; i < segments.size(); i++) {
            int finalI = i;
            CompletableFuture<Map<Integer, String>> future = CompletableFuture
                .supplyAsync(() -> processChunk(finalI, segments.get(finalI).text()), executor);
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

        String content = contextBuilder.toString();
        String output = processSummary(content);

        System.out.println(output);
        System.out.printf("\nChunks: %d, Chunk size: %d, Overlap size: %d", segments.size(), CHUNK_SIZE, OVERLAP_SIZE);
        System.out.printf("\nSummarization (map-reduce) took %d milliseconds", (System.currentTimeMillis() - start));

        executor.shutdown(); // Shutdown the executor
    }

    public interface FinalSummarizationAssistant {
        @SystemMessage("""
        You are a helpful AI assistant.
        You are an AI assistant that helps people summarize information.
        Your name is Gemini
        You should reply to the user's request with your name and also in the style of a literary critic
        Strictly ignore Project Gutenberg & ignore copyright notice in summary output.
        """)
        @UserMessage("""
        Strictly please give me a summary with an introduction, in no more than 10 one sentence bullet points, and a conclusion from the following text delimited by triple backquotes.
      
        ```Text:{{content}}```
      
        Output starts with SUMMARY:
        """)
        String summarize(@V("content") String content);
    }

    private String processSummary(String content) {
        long start = System.currentTimeMillis();

        FinalSummarizationAssistant assistant = AiServices.create(FinalSummarizationAssistant.class, chatModel);
        String response = assistant.summarize(content);

        System.out.println(content+"\n\n");
        System.out.println("Summarization (final summary) took " + (System.currentTimeMillis() - start) + " milliseconds");
        return response;
    }

    public interface SegmentSummarizationAssistant {
        @SystemMessage("""
        You are a helpful AI assistant.
        You are an AI assistant that helps people summarize information.
        Your name is Gemini
        You should reply to the user's request with your name and also in the style of a literary critic
        Strictly ignore Project Gutenberg & ignore copyright notice in summary output.
        """)
        @UserMessage("""
        Taking the following context delimited by triple backquotes into consideration

        ```{{context}}```

        Write a concise summary of the following text delimited by triple backquotes.

        ```{{content}}```

        Output starts with CONCISE SUB-SUMMARY:

        """)
        String summarize(@V("context") String context, @V("content") String content);
    }

    private void processChunk(StringBuilder context, String segment) {
        long start = System.currentTimeMillis();

        SegmentSummarizationAssistant assistant = AiServices.create(SegmentSummarizationAssistant.class, chatModel);
        String response = assistant.summarize(context.toString(), segment);

        System.out.println(response);
        System.out.println("Summarization (single chunk) took " + (System.currentTimeMillis() - start) + " milliseconds");
        context.append("\n").append(response);
    }

    private Map<Integer, String> processChunk(Integer index, String chunk) {
        Map<Integer, String> outputWithIndex = new HashMap<>();
        StringBuilder content = new StringBuilder();

        processChunk(content, chunk);

        outputWithIndex.put(index, content.toString());
        return outputWithIndex;
    }

    // --- set the test configuration up ---
    @SpringBootConfiguration
    public static class TestConfiguration {
        @Bean
        public VertexAiGeminiChatModel vertexAiEmbedding() {
            return VertexAiGeminiChatModel.builder()
                .project(System.getenv("VERTEX_AI_GEMINI_PROJECT_ID"))
                .location(System.getenv("VERTEX_AI_GEMINI_LOCATION"))
                .modelName(System.getenv("VERTEX_AI_GEMINI_MODEL"))
                .temperature(0.2f)
                .maxOutputTokens(8192)
                .build();
        }
    }
}
