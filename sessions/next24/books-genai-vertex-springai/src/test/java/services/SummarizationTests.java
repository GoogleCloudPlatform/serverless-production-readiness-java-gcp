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

import java.util.List;
import java.util.Map;

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
    @Value("classpath:/prompts/initial-message.st")
    private Resource initialResource;
    @Value("classpath:/prompts/refine-message.st")
    private Resource refineResource;

    @Value("classpath:/books/The_Wasteland-TSEliot-public.txt")
    private Resource resource;

    @Test
    public void stuffTest(){
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
