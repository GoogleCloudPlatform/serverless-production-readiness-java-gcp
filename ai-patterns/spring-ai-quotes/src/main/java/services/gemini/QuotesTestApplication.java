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

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuotesTestApplication {
	@Bean
	ApplicationRunner applicationRunner(
			VertexAiGeminiChatModel geminiChatModel,
			AnthropicChatModel anthropicChatModel) {

		return args -> {
			String book = "The Jungle Book";
			// sample prompt
			// String prompt = String.format("You are an experienced literary critic. Please write a summary of the book %s", book);
			String prompt = String.format("You are an experienced literary critic. Please extract a famous quote from the book %s", book);

			// test against Gemini (Flash|Pro) 1.5
			long start = System.currentTimeMillis();
			System.out.println("VERTEX_AI_GEMINI: " + geminiChatModel
					.call(
							new Prompt(prompt,
									VertexAiGeminiChatOptions.builder()
											.withTemperature(0.2f).build())
					).getResult().getOutput().getContent());
			System.out.println("VertexAI Gemini call took " + (System.currentTimeMillis() - start) + " ms");

			// test against Anthropic SONNET (3.5)
			start = System.currentTimeMillis();
			System.out.println("ANTHROPIC_SONNET: " + anthropicChatModel
					.call(
							new Prompt(prompt,
									AnthropicChatOptions.builder()
											.withTemperature(0.2f).build())
					).getResult().getOutput().getContent());
			System.out.println("Anthropic SONNET call took " + (System.currentTimeMillis() - start) + " ms");

		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(QuotesTestApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
