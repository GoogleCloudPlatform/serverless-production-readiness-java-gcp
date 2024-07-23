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
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.Transport;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;


@SpringBootApplication
@ImportRuntimeHints(Langchain4JFunctionCallingApplication.FunctionCallingRuntimeHints.class)
public class Langchain4JFunctionCallingApplication {
	@Value("${langchain4j.gemini.project-id}")
	private String project;

	@Value("${langchain4j.gemini.location}")
	private String location;

	@Value("${langchain4j.gemini.chat.options.model}")
	private String chatModel;

	@Value("${langchain4j.open-ai.chat-model.base-url}")
	private String openAIVertexUrl;

	@Value("${langchain4j.open-ai.chat-model.model-name}")
	private String openAIVertexChatModel;

	static class FunctionCallingService {
		record Transaction(String id) { }
		record Status(String name) { }

		private static final Map<Transaction, Status> DATASET = Map.of(
				new Transaction("001"), new Status("pending"),
				new Transaction("002"), new Status("approved"),
				new Transaction("003"), new Status("rejected"));

		@Tool("Get the status of a payment transaction")
		public Status paymentStatus(@P("The id of the payment transaction") String transaction) {
			System.out.println();
			return DATASET.get(new Transaction(transaction));
		}
	}

	// @AiService
	interface Assistant {
		@SystemMessage("You are a helpful assistant that can answer questions about payment transactions.")
		String chat(String userMessage);
	}

	@Bean
	ApplicationRunner applicationRunner() {
		return args -> {
			String userMessage = """
   							What is the status of my payment transactions 002, 001, 003?
   							Please indicate the status for each transaction and return the results in JSON format.
   							""";

			// test with the OpenAI API in Vertex AI using gRPC
			functionCallOpenAIAPIGeminiWithGRPC(userMessage);

			// test with VertexAI Gemini using REST API
			functionCallGeminiWithREST(userMessage);

			// test with VertexAI Gemini using gRPC
			functionCallGeminiWithGRPC(userMessage);
		};
	}

	// Get OAuth2 token
	// used as API Key for the OpenAI API
	private static String getToken(String target) throws IOException {
		// Load credentials from the environment (default)
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

		// Refresh if necessary
		if (credentials.getAccessToken() == null || credentials.getAccessToken().getExpirationTime().before(new Date())) {
			credentials.refresh();
		}

		// Get the access token
		AccessToken accessToken = credentials.getAccessToken();
		System.out.println("Access Token: " + accessToken.getTokenValue());

		return accessToken.getTokenValue();
	}

	public void functionCallOpenAIAPIGeminiWithGRPC(String userMessage) throws IOException {
			long start = System.currentTimeMillis();

			// String url = "https://us-central1-aiplatform.googleapis.com/v1beta1/projects/optimize-serverless-apps/locations/us-central1/endpoints/openapi";
			String token = getToken(openAIVertexUrl);
			// String modelName = "google/gemini-1.5-flash-001";

			ChatLanguageModel model = OpenAiChatModel.builder()
					.apiKey(token)
					.baseUrl(openAIVertexUrl)
					.modelName(openAIVertexChatModel)
					.maxTokens(4096)
					.temperature(0.2)
					.logRequests(true)
					.logResponses(true)
					.maxRetries(1)
					.build();

		FunctionCallingService service = new FunctionCallingService();

		Assistant assistant = AiServices.builder(Assistant.class)
				.chatLanguageModel(model)
				.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
				.tools(service)
				.build();

		System.out.printf("Calling model %s in project %s in location %s at URL %s\n", openAIVertexChatModel, project, location, openAIVertexUrl);
		System.out.println("User message: " + userMessage);
		System.out.println(assistant.chat(userMessage));
		System.out.println("VertexAI Gemini call using OPenAI API and GRPC took " + (System.currentTimeMillis() - start) + " ms");
	}

	private void functionCallGeminiWithGRPC(String userMessage) {
		long start = System.currentTimeMillis();

		ChatLanguageModel model = VertexAiGeminiChatModel.builder()
				.project(project)
				.location(location)
				.modelName(chatModel)
				.temperature(0.2f)
				.maxOutputTokens(1000)
				.build();

		FunctionCallingService service = new FunctionCallingService();

		Assistant assistant = AiServices.builder(Assistant.class)
				.chatLanguageModel(model)
				.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
				.tools(service)
				.build();

		System.out.println("User message: " + userMessage);
		System.out.println(assistant.chat(userMessage));
		System.out.println("VertexAI Gemini call using GRPC took " + (System.currentTimeMillis() - start) + " ms");
	}

	private void  functionCallGeminiWithREST(String userMessage) {
			long start = System.currentTimeMillis();

			VertexAI vertexAi = new VertexAI.Builder().setProjectId(project).setLocation(location).setTransport(Transport.REST).build();
			GenerativeModel generativeModel = new GenerativeModel(chatModel, vertexAi);
			GenerationConfig generationConfig = GenerationConfig.newBuilder().setTemperature(0.2f).setMaxOutputTokens(1000).build();

			ChatLanguageModel model = new VertexAiGeminiChatModel(generativeModel, generationConfig, 1);

			FunctionCallingService service = new FunctionCallingService();

			Assistant assistant = AiServices.builder(Assistant.class)
					.chatLanguageModel(model)
					.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
					.tools(service)
					.build();

			System.out.println("User message: " + userMessage);
			System.out.println(assistant.chat(userMessage));
			System.out.println("VertexAI Gemini call using REST took " + (System.currentTimeMillis() - start) + " ms");
	}

	public static class FunctionCallingRuntimeHints implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			try {
				// Register all the classes and methods that are used through reflection
				// or dynamic proxy generation in LangChain4j, especially those
				// related to function calling.
				// Register method for reflection
				var mcs = MemberCategory.values();
				hints.reflection().registerType(Langchain4JFunctionCallingApplication.Assistant.class, mcs);
				hints.proxies().registerJdkProxy(Langchain4JFunctionCallingApplication.Assistant.class);
				hints.reflection().registerType(FunctionCallingService.class, mcs);

				hints.reflection().registerMethod(
						FunctionCallingService.class.getMethod("paymentStatus", String.class),
						ExecutableMode.INVOKE
				);

				// ... register other necessary classes and methods ...
			} catch (NoSuchMethodException e) {
				// Handle the exception appropriately (e.g., log it)
				e.printStackTrace();
			}
		}
	}


	public static void main(String[] args) {
		new SpringApplicationBuilder(Langchain4JFunctionCallingApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
