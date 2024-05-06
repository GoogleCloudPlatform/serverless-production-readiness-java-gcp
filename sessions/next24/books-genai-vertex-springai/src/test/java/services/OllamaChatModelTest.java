
package services;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import static java.time.Duration.ofSeconds;

@SpringBootTest
@SpringJUnitConfig
@Testcontainers
@ActiveProfiles(value = "test")
@TestPropertySource(properties = {"spring.ai.vertex.ai.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}",
        "spring.ai.vertex.ai.gemini.location=${VERTEX_AI_GEMINI_LOCATION}",
        "spring.ai.vertex.ai.gemini.transport=rest"})
public class OllamaChatModelTest {
    static String MODEL_NAME = "gemma:7b";
    static String imageName = "tc-ollama-gemma-7b";

    @Autowired
    VertexAiGeminiChatClient chatClient;

    @ServiceConnection
    @Container
    static OllamaContainer ollama = new OllamaContainer(
            DockerImageName.parse(imageName).asCompatibleSubstituteFor("ollama/ollama")
    );

    @Ignore
    @Test
    void simplexample() {

        ChatResponse chatResponse = chatClient.call(new Prompt("tell me a joke",
                VertexAiGeminiChatOptions.builder()
                        .withTemperature(0.4f)
                        .withModel("gemma:7b")
                        .build()));

        System.out.println(chatResponse.getResult().getOutput().getContent());
    }

    @DynamicPropertySource
    static void registerOllamaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.vertex.ai.gemini.api-endpoint",
                () -> String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort()));
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ RestClientAutoConfiguration.class, OllamaAutoConfiguration.class, VertexAiGeminiAutoConfiguration.class})
    static class Config {

    }

    static String baseUrl() {
        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
    }
}
