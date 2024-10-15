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
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class FunctionCallingApplication {

	@Value("${spring.ai.openai.chat.base-url}")
	String baseURL;

	@Value("${spring.ai.openai.chat.completions-path}")
	String completionsPath;

	record Transaction(String id) {
	}

	record Status(String name) {
	}

	// record Transactions(List<Transaction> transactions) {
	// }
	//
	// record Statuses(List<Status> statuses) {
	// }
	private static final Map<Transaction, Status> DATASET = Map.of(
			new Transaction("001"), new Status("pending"),
			new Transaction("002"), new Status("approved"),
			new Transaction("003"), new Status("rejected"));

	// The spring.ai.<model>.chat.options.functions=paymentStatus properties
	// are used to register the paymentStatus function with the AI Mo
	@Bean
		@Description("Get the status of a payment transaction")
		public Function<Transaction, Status> paymentStatus() {
			return transaction -> DATASET.get(transaction);
		}

	// @Bean
	// @Description("Get the list statuses of a list of payment transactions")
	// public Function<Transactions, Statuses> paymentStatuses() {
	// 	return transactions -> {
	// 		return new Statuses(transactions.transactions().stream().map(t -> DATASET.get(t)).toList());
	// 	};
	// }

	@Bean
	ApplicationRunner applicationRunner(
			VertexAiGeminiChatModel vertexAiGemini,
			OpenAiChatModel openAI) {

		//--- Multi-turn function calling ---
		return args -> {
			String prompt = """
   							Please use multi-turn invocation to answer the following question:
   							What is the status of my payment transactions 002, 001 and 003?
   							Please indicate the status for each transaction and return the results in JSON format
   							""";

			long start = System.currentTimeMillis();
			System.out.println("VERTEX_AI_GEMINI multi-turn fn calling: " + vertexAiGemini.call(
							new Prompt(prompt,
									VertexAiGeminiChatOptions.builder()
											.withTemperature(0.2).build())
					).getResult().getOutput().getContent().trim());

			System.out.println("VertexAI Gemini multi-turn call took " + (System.currentTimeMillis() - start) + " ms");

			start = System.currentTimeMillis();
			Flux<ChatResponse> geminiStream = vertexAiGemini.stream(
					new Prompt(prompt,
							VertexAiGeminiChatOptions.builder()
									.withTemperature(0.2).build())
			);

			geminiStream.collectList().block().stream().findFirst().ifPresent(resp -> {
				System.out.println("\nVERTEX_AI_GEMINI (Streaming) multi-turn fn calling: " + resp.getResult().getOutput().getContent().trim());
			});
			System.out.println("VertexAI Gemini multi-turn streaming call took " + (System.currentTimeMillis() - start) + " ms");

			//--- Parallel function calling ---
			String parallelizedPrompt = """
   							What is the status of my payment transactions 002, 001 and 003?
   							Please indicate the status for each transaction and return the results in JSON format
   							""";

			// start = System.currentTimeMillis();
			// System.out.println("\nOPEN_AI parallel fn calling: " + openAI.call(
			// 		new Prompt(parallelizedPrompt,
			// 				VertexAiGeminiChatOptions.builder()
			// 						.withTemperature(0.2f).build())
			// ).getResult().getOutput().getContent().trim());
			// System.out.println("OpenAI (with parallel function calling) call took " + (System.currentTimeMillis() - start) + " ms");


			String token = getOauth2Token(baseURL + completionsPath);
			String model = "google/gemini-1.5-pro-002";
			// String model = "meta/llama3-405b-instruct-maas";

			OpenAiApi openAiApi = new OpenAiApi(baseURL, token, completionsPath,
													"/v1/embeddings",
																					RestClient.builder(),
																					WebClient.builder(),
																					RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

			OpenAiChatModel openAIGemini = new OpenAiChatModel(openAiApi);
			OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
					.withTemperature(0.2)
					.withModel(model)
					.build();

			start = System.currentTimeMillis();
			System.out.println("\nOPEN_AI API (with parallel fn calling) in Vertex AI: " + openAIGemini.call(
					new Prompt(parallelizedPrompt, openAiChatOptions))
					.getResult().getOutput().getContent());
			System.out.println("OpenAI API (with parallel function calling) in VertexAI call took " + (System.currentTimeMillis() - start) + " ms");

			start = System.currentTimeMillis();
			System.out.println("\nVERTEX_AI_GEMINI parallel fn calling: " + vertexAiGemini.call(
					new Prompt(parallelizedPrompt,
							VertexAiGeminiChatOptions.builder()
									.withTemperature(0.2).build())
			).getResult().getOutput().getContent().trim());

			System.out.println("VertexAI Gemini (with parallel function calling) call took " + (System.currentTimeMillis() - start) + " ms");

			start = System.currentTimeMillis();
			geminiStream = vertexAiGemini.stream(
					new Prompt(parallelizedPrompt,
							VertexAiGeminiChatOptions.builder()
									.withTemperature(0.2).build())
			);

			geminiStream.collectList().block().stream().findFirst().ifPresent(resp -> {
				System.out.println("\nVERTEX_AI_GEMINI (Streaming) parallel fn calling: " + resp.getResult().getOutput().getContent().trim());
			});
			System.out.println("VertexAI Gemini parallel streaming call took " + (System.currentTimeMillis() - start) + " ms");
		};
	}

	private static String getOauth2Token(String target) throws IOException {
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

	public static void main(String[] args) {
		new SpringApplicationBuilder(FunctionCallingApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
