package services;

import com.google.cloud.vertexai.Transport;
import com.google.cloud.vertexai.VertexAI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles(value = "test")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_PROJECT_ID", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_LOCATION", matches = ".*")
@EnabledIfEnvironmentVariable(named = "MODEL", matches = ".*")
public class SummarizationTests {

    @Autowired
    private VertexAiGeminiChatClient chatClient;

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
    public void summarizationTest(){
        TextReader textReader = new TextReader(resource);
        String bookTest = textReader.get().getFirst().getContent();
        // System.out.println(bookTest);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "Gemini", "voice", "literary critic"));


        PromptTemplate userPromptTemplate = new PromptTemplate(initialResource,Map.of("content", bookTest));
        Message userMessage = userPromptTemplate.createMessage();

        long start = System.currentTimeMillis();
        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage, systemMessage),
            VertexAiGeminiChatOptions.builder()
                .withTemperature(0.4f)
                .build()));

        System.out.println(response.getResult().getOutput().getContent());
        System.out.print("Summarization took " + (System.currentTimeMillis() - start) + " milliseconds");
    }

    @Test
    public void summarizationChunkTest(){
        TextReader textReader = new TextReader(resource);
        String bookTest = textReader.get().getFirst().getContent();

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemSummaryResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        int length = bookTest.length();
        String subcontext = "";
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
    }


    @Test
    public void summarizationOverlappingWindowsTest(){
        TextReader textReader = new TextReader(resource);
        String bookTest = textReader.get().getFirst().getContent();

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemSummaryResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        int length = bookTest.length();
        String subcontext = "";
        String context = "";
        for (int i = 0; i < length; i += CHUNK_SIZE-OVERLAP_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, length);
            String chunk = bookTest.substring(i, end);

            // Process the chunk here
            subcontext = processChunk("", chunk, systemMessage);
            context += "\n"+subcontext;
        }

        String output = processSummary(context, systemMessage);

        System.out.println(output);
    }

    private String processSummary(String context, Message systemMessage) {
        long start = System.currentTimeMillis();
        System.out.println(context+"\n\n\n\n\n");
        PromptTemplate userPromptTemplate = new PromptTemplate(summaryResource,Map.of("content", context));
        Message userMessage = userPromptTemplate.createMessage();

        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage, systemMessage),
                VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .build()));
        System.out.print("Summarization took " + (System.currentTimeMillis() - start) + " milliseconds");
        return response.getResult().getOutput().getContent();
    }

    private String processChunk(String context, String chunk, Message systemMessage) {
        long start = System.currentTimeMillis();
        PromptTemplate userPromptTemplate = null;
        if(context.trim().equals("")) {
            userPromptTemplate = new PromptTemplate(subsummaryOverlapResource, Map.of("content", chunk));
        } else {
            userPromptTemplate = new PromptTemplate(subsummaryResource, Map.of("context", context, "content", chunk));
        }
        Message userMessage = userPromptTemplate.createMessage();

        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage, systemMessage),
                VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .build()));
        System.out.print("Summarization took " + (System.currentTimeMillis() - start) + " milliseconds");
        String output = response.getResult().getOutput().getContent();
        System.out.println(output+"\n\n\n\n\n");
        return output;
    }

    @Test
    public void summarizationChunkParallelTest() throws Exception {
        TextReader textReader = new TextReader(resource);
        String bookText = textReader.get().getFirst().getContent();
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemSummaryResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        int length = bookText.length();
        List<CompletableFuture<String>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        String previousChunk = "";
        for (int i = 0; i < length; i += (CHUNK_SIZE - OVERLAP_SIZE)) {
            int start = i;
            int end = Math.min(start + CHUNK_SIZE, length);
            String chunk = bookText.substring(start, end);
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> processChunk("", chunk, systemMessage), executor);
            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        CompletableFuture<List<String>> allPageContentsFuture = allFutures.thenApply(v -> {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        });

        List<String> results = allPageContentsFuture.get();  // Block and get all results
        String context = String.join("\n", results);  // Join all results into a single context

        String output = processSummary(context, systemMessage);
        System.out.println(output);

        executor.shutdown();  // Shutdown the executor
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
        public VertexAiGeminiChatClient vertexAiEmbedding(VertexAI vertexAi) {
            String model = System.getenv("MODEL");
            return new VertexAiGeminiChatClient(vertexAi,
                VertexAiGeminiChatOptions.builder()
                    .withModel(model)
                    .build());
        }
    }
}
