package services.ai;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Blob;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import com.google.protobuf.ByteString;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import services.config.CloudConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class VertexAIClient {
    private static final Logger logger = LoggerFactory.getLogger(VertexAIClient.class);

    public GenerateContentResponse promptOnImage(byte[] image) throws IOException {
        GenerateContentResponse response = null;
        String modelName = "gemini-1.0-pro-vision";
        String prompt = "Extract the book and author name";
        String location = "us-central1";
        try (VertexAI vertexAI = new VertexAI(CloudConfig.projectID, location)) {
            GenerationConfig generationConfig =
                    GenerationConfig.newBuilder()
                            .setMaxOutputTokens(2048)
                            .setTemperature(0.4F)
                            .setTopK(32)
                            .setTopP(1F)
                            .build();
            GenerativeModel model = new GenerativeModel(modelName, generationConfig, vertexAI);
            List<SafetySetting> safetySettings = Arrays.asList(
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build(),
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build(),
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build(),
                    SafetySetting.newBuilder()
                            .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                            .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                            .build()
            );
            List<Content> contents = new ArrayList<>();
            contents.add(Content.newBuilder().setRole("user").addParts(Part.newBuilder().setInlineData(Blob.newBuilder().setMimeType("image/png")
                            .setData(ByteString.copyFrom(image))))
                    .addParts(Part.newBuilder().setText(prompt))
                    .build());
            ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(contents, safetySettings);
            response = responseStream.iterator().next();
            logger.info(response.toString());
        }
        return response;
    }
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
                    .maxOutputTokens(1000)
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
                    .maxOutputTokens(1000)
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
