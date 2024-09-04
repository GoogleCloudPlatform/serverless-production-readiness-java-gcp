package services.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import services.ai.VertexAIClient;
import services.domain.BooksDataService;
import services.domain.CloudStorageService;
import services.utility.PromptUtility;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@Service
public class TFService {
    private static final Logger logger = LoggerFactory.getLogger(TFService.class);

    BooksDataService booksDataService;
    CloudStorageService cloudStorageService;
    VertexAIClient vertexAIClient;

    @Value("classpath:/prompts/transform-tf-system-message.st")
    Resource promptTransformTFSystemMessage;

    @Value("classpath:/prompts/transform-tf-user-message.st")
    Resource promptTransformTFUserMessage;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    private String model;

    @Value("classpath:/bashscripts/provision-cloud-infra.sh")
    private Resource bashscript;

    @Value("classpath:/queries/tf-transform-search-query.st")
    Resource tfTransformSearchQuery;
    public TFService(BooksDataService booksDataService,
                                     CloudStorageService cloudStorageService,
                                     VertexAIClient vertexAIClient) {
        this.booksDataService = booksDataService;
        this.cloudStorageService = cloudStorageService;
        this.vertexAIClient = vertexAIClient;
    }

    public String tfTransform(String bucketName, String fileName){
        // read file from Cloud Storage
        String script = cloudStorageService.readFileAsString(bucketName, fileName);
        return tfTransform(script);
    }

    public String tfTransform(String script) {
        logger.info("tf transform flow - Model: {}", model);
        long start = System.currentTimeMillis();

        List<Map<String, Object>> responseDoc;
        try {
            responseDoc = booksDataService.prompt(tfTransformSearchQuery.getContentAsString(Charset.defaultCharset()), 6000);
        } catch (IOException e) {
            return "Could not transform the Terraform script";
        }

        // create a SystemMessage
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptTransformTFSystemMessage);
        Message systemMessage = systemPromptTemplate.createMessage();

        // create a UserMessage
        Message userMessage =  PromptUtility.formatPromptTF(responseDoc, promptTransformTFUserMessage, script);
        logger.info("TF transform: prompt LLM: {}ms", System.currentTimeMillis() - start);
        String response = vertexAIClient.promptModel(systemMessage, userMessage, model);
        logger.info("TF transform flow: {}ms", System.currentTimeMillis() - start);
        return response;
    }
}
