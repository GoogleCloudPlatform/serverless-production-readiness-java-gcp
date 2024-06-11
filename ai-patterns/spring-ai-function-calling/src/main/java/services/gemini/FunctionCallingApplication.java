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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class FunctionCallingApplication {

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
			VertexAiGeminiChatModel vertexAiGemini) {

		return args -> {
			String prompt = """
   							Please use multi-turn invocation to answer the following question:
   							What is the status of my payment transactions 002, 001 and 003?
   							Please indicate the status for each transaction and return the results in JSON format
   							""";

			long start = System.currentTimeMillis();
			System.out.println("VERTEX_AI_GEMINI: " + vertexAiGemini.call(
							new Prompt(prompt,
									VertexAiGeminiChatOptions.builder()
											.withTemperature(0.2f).build())
					).getResult().getOutput().getContent());

			System.out.println("VertexAI Gemini call took " + (System.currentTimeMillis() - start) + " ms");

			// Currently, SpringAI supports streaming Function calls only for VertexAI Gemini.
			start = System.currentTimeMillis();
			Flux<ChatResponse> geminiStream = vertexAiGemini.stream(
					new Prompt(prompt,
							VertexAiGeminiChatOptions.builder()
									.withTemperature(0.0f).build())
			);

			geminiStream.collectList().block().stream().findFirst().ifPresent(resp -> {
				System.out.println("VERTEX_AI_GEMINI (Streaming): " + resp.getResult().getOutput().getContent());
			});
			System.out.println("VertexAI Gemini streaming call took " + (System.currentTimeMillis() - start) + " ms");
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(FunctionCallingApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
