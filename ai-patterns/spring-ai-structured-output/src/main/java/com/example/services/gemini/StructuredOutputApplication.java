package com.example.spring.ai.demostructuredoutput;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.support.DefaultConversionService;

@SpringBootApplication
public class StructuredOutputApplication {

	@Bean
	ApplicationRunner applicationRunner(VertexAiGeminiChatModel chatClient) {
		return args -> {
			// convert response to a map
			mapOutputConverter(chatClient);

			// convert response to a list
			listOutputConverter(chatClient);

			// convert response to a bean
			beanOutputConverter(chatClient);
		};
	}

	public void mapOutputConverter(VertexAiGeminiChatModel chatClient) {
		MapOutputConverter mapOutputConverter = new MapOutputConverter();

		String format = mapOutputConverter.getFormat();
		String template = """
				You are a helpful AI assistant. 
        You are an AI assistant that helps people find information.
        You should reply to the user's request in the style of a literary professor.
				Provide me a quote from a random book, including only {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "book, author and quote", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		Generation generation = chatClient.call(prompt).getResult();

		Map<String, Object> result = mapOutputConverter.convert(generation.getOutput().getContent());

		System.out.println("Prompt for MapConverter test:" + prompt.getContents().toString());
		System.out.println("Format this response to a map: " + generation.getOutput().getContent());
		System.out.println("Formatted response: " + result);

	}

	public void listOutputConverter(VertexAiGeminiChatModel chatClient) {
		ListOutputConverter listOutputConverter = new ListOutputConverter(new DefaultConversionService());

		String format = listOutputConverter.getFormat();
		String template = """
				You are a helpful AI assistant. 
        You are an AI assistant that helps people find information.
        You should reply to the user's request in the style of a literary professor.				
				List the top ten {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "important fiction books I should read in my lifetime, with book and author", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		Generation generation = chatClient.call(prompt).getResult();

		List<String> list = listOutputConverter.convert(generation.getOutput().getContent());

		System.out.println("Prompt for ListConverter test:" + prompt.getContents().toString());
		System.out.println("Format this response to a List: " + generation.getOutput().getContent());
		System.out.println("Formatted response: " + list);
	}

	public void beanOutputConverter(VertexAiGeminiChatModel chatClient) {

		record BooksAuthor(String writer, List<String> books) {}

		BeanOutputConverter<BooksAuthor> beanOutputConverter = new BeanOutputConverter<>(BooksAuthor.class);

		String format = beanOutputConverter.getFormat();
		String writer = "Gabriel Garcia Marquez";

		String template = """
				Generate the bibliography of books written by the writer {writer}.
				{format}
				""";

		Prompt prompt = new Prompt(
				new PromptTemplate(template, Map.of("writer", writer, "format", format)).createMessage());
		Generation generation = chatClient.call(prompt)
				.getResult();

		System.out.println("Prompt for BeanConverter test:" + prompt.getContents().toString());
		String content = generation.getOutput().getContent();
		System.out.println("Format this response to a Bean: " + content);

		BooksAuthor writerBooks = beanOutputConverter.convert(content);

		System.out.println("Formatted response: " + writerBooks);
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(
				com.example.spring.ai.demostructuredoutput.StructuredOutputApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}

}
