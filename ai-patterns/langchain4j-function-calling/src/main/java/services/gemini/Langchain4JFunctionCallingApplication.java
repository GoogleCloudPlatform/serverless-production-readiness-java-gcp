/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services.gemini;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Langchain4JFunctionCallingApplication {
	@Value("${langchain4j.gemini.project-id}")
	private String project;

	@Value("${langchain4j.gemini.location}")
	private String location;

	@Value("${langchain4j.gemini.chat.options.model}")
	private String chatModel;

	static class FunctionCallingService {
		private static final Logger logger = LoggerFactory.getLogger(FunctionCallingService.class);
		record Transaction(String id) { }

		record Status(String name) { }

		private static final Map<Transaction, Status> DATASET = Map.of(
				new Transaction("001"), new Status("pending"),
				new Transaction("002"), new Status("approved"),
				new Transaction("003"), new Status("rejected"));

		@Tool("Get the status of a payment transaction")
		public Status paymentStatus(@P("The id of the payment transaction") String transaction) {
			return DATASET.get(new Transaction(transaction));
		}
	}

	@AiService
	interface FunctionCallingAssistant {
		@SystemMessage("You are a helpful assistant that can answer questions about payment transactions.")
		String chat(String userMessage);
	}

	@Bean
	ApplicationRunner applicationRunner() {
		return args -> {
			String userMessage = """
   							Please use multi-turn conversation to answer the following questions:
   							What is the status of my payment transactions 002, 001, 003?
   							Please indicate the status for each transaction and return the results in JSON format.
   							""";

			long start = System.currentTimeMillis();
			ChatLanguageModel model = VertexAiGeminiChatModel.builder()
					.project(project)
					.location(location)
					.modelName(chatModel)
					.temperature(0.2f)
					.maxOutputTokens(1000)
					.build();

			FunctionCallingService service = new FunctionCallingService();

			FunctionCallingAssistant assistant = AiServices.builder(FunctionCallingAssistant.class)
					.chatLanguageModel(model)
					// .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
					.tools(service)
					.build();

			System.out.println(assistant.chat(userMessage));
			System.out.println("VertexAI Gemini call took " + (System.currentTimeMillis() - start) + " ms");
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(Langchain4JFunctionCallingApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
