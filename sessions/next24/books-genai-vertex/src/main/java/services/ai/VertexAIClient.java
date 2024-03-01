package services.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import services.config.CloudConfig;
import services.web.ImageProcessingController;

@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);
    public String prompt(String prompt, String modelName) {
        String output =null;
        logger.info("The prompt & model name are: " + prompt.substring(0,100) +" | "+modelName);
        if (modelName.contains("chat")) {
            VertexAiChatModel vertexAiChatModel = VertexAiChatModel.builder()
                    .endpoint("us-central1-aiplatform.googleapis.com:443")
                    .project(CloudConfig.projectID)
                    .location(CloudConfig.zone)
                    .publisher("google")
                    .modelName(modelName)
                    .temperature(0.1)
                    .maxOutputTokens(500)
                    .topK(0)
                    .topP(0.0)
                    .maxRetries(3)
                    .build();
            Response<AiMessage> modelResponse = vertexAiChatModel.generate(UserMessage.from(prompt));
            output = modelResponse.content().text();
        } else {
            VertexAiLanguageModel vertexAiTextModel = VertexAiLanguageModel.builder()
                    .endpoint("us-central1-aiplatform.googleapis.com:443")
                    .project(CloudConfig.projectID)
                    .location(CloudConfig.zone)
                    .publisher("google")
                    .modelName(modelName)
                    .temperature(0.1)
                    .maxOutputTokens(50)
                    .topK(0)
                    .topP(0.0)
                    .maxRetries(3)
                    .build();
            Response<String> textResponse = vertexAiTextModel.generate(prompt);
            output = textResponse.content();
        }

        logger.info(output);
        return output;
    }

}
