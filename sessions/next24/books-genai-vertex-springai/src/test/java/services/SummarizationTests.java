package services;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration;
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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;

@SpringBootTest
@SpringJUnitConfig
@ActiveProfiles(value = "test")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_PROJECT_ID", matches = ".*")
@EnabledIfEnvironmentVariable(named = "VERTEX_AI_GEMINI_LOCATION", matches = ".*")
@TestPropertySource(properties = {"spring.ai.vertex.ai.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}",
        "spring.ai.vertex.ai.gemini.location=${VERTEX_AI_GEMINI_LOCATION}",
        "spring.ai.vertex.ai.gemini.transport=rest"})
public class SummarizationTests {

    @Autowired
    private VertexAiGeminiChatClient chatClient;

    private Resource systemResource = new ClassPathResource("/prompts/system-message.st");
    private Resource initialResource = new ClassPathResource("/prompts/initial-message.st");
    private Resource resourceResource = new ClassPathResource("/prompts/refine-message.st");

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    String model;

    private Resource resource = new DefaultResourceLoader().getResource("The_Jungle_Book-Rudyard_Kipling-1894-public.txt");

//    @Configuration(proxyBeanMethods = false)
//    @ImportAutoConfiguration(VertexAiGeminiAutoConfiguration.class)
//    static class Config {
//    }

    @Ignore
    @Test
    public void stuffTest(){
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", "The_Jungle_Book-Rudyard_Kipling-1894-public.txt");

        String bookTest = textReader.get().getFirst().getContent();
        System.out.println(bookTest);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "Gemini", "voice", "literary critic"));


        PromptTemplate userPromptTemplate = new PromptTemplate(initialResource,Map.of("content", bookTest));
        Message userMessage = userPromptTemplate.createMessage();

        ChatResponse response = chatClient.call(new Prompt(List.of(userMessage, systemMessage),
                        VertexAiGeminiChatOptions.builder()
                                .withTemperature(0.4f)
                                .withModel(model)
                                .build()));

        System.out.println(response.getResult().getOutput().getContent());
    }
}
