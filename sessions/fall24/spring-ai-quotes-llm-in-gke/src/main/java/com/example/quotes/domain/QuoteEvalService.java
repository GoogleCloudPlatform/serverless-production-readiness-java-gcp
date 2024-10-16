package com.example.quotes.domain;

import java.util.List;
import java.util.Map;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.messages.Message;

@Service
public class QuoteEvalService {

    private final AnthropicChatModel anthropicChatModel;

    public QuoteEvalService(AnthropicChatModel anthropicChatModel) {
        this.anthropicChatModel = anthropicChatModel;
    }
    
    public boolean evaluate(String quote, String book){
        SystemMessage systemMessage = new SystemMessage("""
            You are a helpful AI assistant. 
            You are an AI assistant that helps people find information.
            """);
            
        PromptTemplate userMessageTemplate = new PromptTemplate("""
            You are an experienced literary critic. 
            Please validate that the quote {quote} originates from the book {book}. 
            Return strictly with true or false""");
        Message userMessage = userMessageTemplate.createMessage(Map.of("quote", quote, "book", book));
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage),
                                    AnthropicChatOptions.builder()
                                        .withTemperature(0.2).build());

        // test against Anthropic SONNET (3.5)
        long start = System.currentTimeMillis();
        String response =  anthropicChatModel.call(prompt).getResult().getOutput().getContent();

        System.out.println("ANTHROPIC_SONNET: " + response);
        System.out.println("Anthropic SONNET call took " + (System.currentTimeMillis() - start) + " ms");

        return Boolean.parseBoolean(response);
    }
}
