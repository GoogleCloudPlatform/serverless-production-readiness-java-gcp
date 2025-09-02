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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

// @SpringBootApplication(exclude = {
// 		org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration.class
// })
@SpringBootApplication
public class QuotesTestApplication {
	@Bean
	ApplicationRunner applicationRunner(
      GoogleGenAiChatModel geminiChatModel) {

		return args -> {
			String book = "The Jungle Book";
			// sample prompt
			// String prompt = String.format("You are an experienced literary critic. Please write a summary of the book %s", book);
			String userMsg = String.format("You are an experienced literary critic. Please extract a famous quote from the book %s", book);

			// test against Gemini (Flash|Pro) 1.5
			long start = System.currentTimeMillis();
      Prompt prompt = new Prompt(List.of(new UserMessage(userMsg)));

			System.out.println("GOOGLE_GEN_AI: " + geminiChatModel
					.call(prompt).getResult().getOutput().getText());
			System.out.println("GOOGLE_GEN_AI Gemini call took " + (System.currentTimeMillis() - start) + " ms");

			// test against Anthropic SONNET (3.5)
			// start = System.currentTimeMillis();
			// System.out.println("ANTHROPIC_SONNET: " + anthropicChatModel
			// 		.call(
			// 				new Prompt(prompt,
			// 						AnthropicChatOptions.builder()
			// 								.withTemperature(0.2).build())
			// 		).getResult().getOutput().getContent());
			// System.out.println("Anthropic SONNET call took " + (System.currentTimeMillis() - start) + " ms");
      //
			// String baseURL = String.format("https://us-east5-aiplatform.googleapis.com/v1/projects/genai-playground/locations/us-east5/publishers/anthropic/models/claude-3-5-sonnet-20240620");
			// var anthropicApi = new AnthropicApi(baseURL, getOauth2Token(baseURL));
      //
			// var chatModel = new AnthropicChatModel(anthropicApi,
			// 		AnthropicChatOptions.builder()
			// 				.withModel("claude-3-5-sonnet-20240620")
			// 				.withTemperature(0.4)
			// 				.withMaxTokens(200)
			// 				.build());
      //
			// ChatResponse response = chatModel.call(
			// 		new Prompt("Generate the names of 5 famous pirates."));
		};
	}

	// private static String getOauth2Token(String target) throws IOException {
	// 	// Load credentials from the environment (default)
	// 	GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
  //
	// 	// Refresh if necessary
	// 	if (credentials.getAccessToken() == null || credentials.getAccessToken().getExpirationTime().before(new Date())) {
	// 		credentials.refresh();
	// 	}
  //
	// 	// Get the access token
	// 	AccessToken accessToken = credentials.getAccessToken();
	// 	System.out.println("Generated short-lived Access Token: " + accessToken.getTokenValue());
  //
	// 	return accessToken.getTokenValue();
	// }

	public static void main(String[] args) {
		new SpringApplicationBuilder(QuotesTestApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
