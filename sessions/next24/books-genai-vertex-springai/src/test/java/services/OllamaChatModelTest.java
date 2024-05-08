
package services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaApi.GenerateRequest;
import org.springframework.ai.ollama.api.OllamaApi.GenerateResponse;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

@Disabled 
@Testcontainers
@SpringBootTest
public class OllamaChatModelTest {
    static String MODEL_NAME = "gemma:7b";
    static String imageName = "tc-ollama-gemma-7b";

    static OllamaApi ollamaApi;

    @ServiceConnection
    @Container
    static OllamaContainer ollama = new OllamaContainer(
            DockerImageName.parse(imageName).asCompatibleSubstituteFor("ollama/ollama")
    );

    @BeforeAll
    public static void beforeAll(){
        ollama.start();
        ollamaApi = new OllamaApi(baseUrl());
    }

    @Test
    public void simplexample() {
		var request = GenerateRequest
			.builder("What is the Gemma model?")
			.withModel(MODEL_NAME)
			.withStream(false)
            .withOptions(OllamaOptions.create()
                    .withTemperature(0.4f))
			.build();

        long start = System.currentTimeMillis();            
		GenerateResponse chatResponse = ollamaApi.generate(request);
        
        System.out.print("Generation test took " + (System.currentTimeMillis() - start) + " milliseconds");
        System.out.println("Response: " + chatResponse.response());
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ RestClientAutoConfiguration.class, OllamaAutoConfiguration.class, OllamaChatClient.class})
    static class Config {

    }

    static String baseUrl() {
        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
    }
}
