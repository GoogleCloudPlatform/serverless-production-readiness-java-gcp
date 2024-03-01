package services.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import services.config.CloudConfig;
import services.web.ImageProcessingController;

@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);
    public String prompt(String prompt, String modelName) {
        VertexAiChatModel vertexAiChatModel = VertexAiChatModel.builder()
                .endpoint("us-central1-aiplatform.googleapis.com:443")
                .project(CloudConfig.projectID)
                .location(CloudConfig.zone)
                .publisher("google")
                .modelName(modelName)
                .temperature(0.1)
                .maxOutputTokens(1000)
                .topK(0)
                .topP(0.0)
                .maxRetries(3)
                .build();

        logger.info("The prompt: " + prompt);


        Response<AiMessage> modelResponse = vertexAiChatModel.generate(UserMessage.from(prompt));
        return modelResponse.content().text();
    }

}
