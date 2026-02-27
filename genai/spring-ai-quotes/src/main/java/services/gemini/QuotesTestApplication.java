/*
 * Copyright 2026 Google LLC
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class QuotesTestApplication {

	@Value("${vertex.ai.anthropic.project-id}")
	private String projectId;

	@Value("${vertex.ai.anthropic.location}")
	private String location;

	@Value("${vertex.ai.anthropic.model}")
	private String model;

	@Value("${vertex.ai.anthropic.temperature}")
	private double temperature;

	@Value("${vertex.ai.anthropic.max-tokens}")
	private int maxTokens;

	@Bean
	AnthropicApi anthropicApi() throws IOException {
		String baseUrl = String.format(
				"https://aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models/%s",
				projectId, location, model);

		String token = getOauth2Token();

		RestClient.Builder restClientBuilder = RestClient.builder()
				.defaultHeader("Authorization", "Bearer " + token)
				.requestInterceptor(new VertexAiRequestInterceptor(new ObjectMapper()));

		return AnthropicApi.builder()
				.baseUrl(baseUrl)
				.completionsPath(":rawPredict")
				.apiKey(new SimpleApiKey(""))
				.anthropicVersion("vertex-2023-10-16")
				.anthropicBetaFeatures("")
				.restClientBuilder(restClientBuilder)
				.build();
	}

	@Bean
	AnthropicChatModel anthropicChatModel(AnthropicApi anthropicApi) {
		AnthropicChatOptions options = AnthropicChatOptions.builder()
				.model(model)
				.temperature(temperature)
				.maxTokens(maxTokens)
				.build();

		return AnthropicChatModel.builder()
				.anthropicApi(anthropicApi)
				.defaultOptions(options)
				.build();
	}

	@Bean
	ApplicationRunner applicationRunner(AnthropicChatModel chatModel) {
		return args -> {
			String book = "The Jungle Book";
			String userMsg = String.format(
					"You are an experienced literary critic. Please extract a famous quote from the book %s", book);

			long start = System.currentTimeMillis();
			Prompt prompt = new Prompt(List.of(new UserMessage(userMsg)));

			System.out.println("ANTHROPIC (Vertex AI): " + chatModel
					.call(prompt).getResult().getOutput().getText());
			System.out.println("Anthropic Claude call took " + (System.currentTimeMillis() - start) + " ms");
		};
	}

	private static String getOauth2Token() throws IOException {
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		credentials.refresh();
		System.out.println("Generated OAuth2 access token for Vertex AI");
		return credentials.getAccessToken().getTokenValue();
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(QuotesTestApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}

	/**
	 * Intercepts outgoing requests to transform Spring AI's Anthropic-native
	 * format into what Vertex AI's rawPredict endpoint expects.
	 */
	private static class VertexAiRequestInterceptor implements ClientHttpRequestInterceptor {

		private static final Set<String> HEADERS_TO_STRIP = Set.of(
				"anthropic-version", "anthropic-beta", "x-api-key");

		private final ObjectMapper objectMapper;

		VertexAiRequestInterceptor(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {

			// --- Modify the JSON body ---
			Map<String, Object> bodyMap = objectMapper.readValue(body,
					new TypeReference<LinkedHashMap<String, Object>>() {});
			bodyMap.put("anthropic_version", "vertex-2023-10-16");
			bodyMap.remove("model");
			byte[] modifiedBody = objectMapper.writeValueAsBytes(bodyMap);

			// --- Strip problematic headers ---
			HttpRequest wrapper = new HttpRequest() {
				@Override
				public HttpMethod getMethod() {
					return request.getMethod();
				}

				@Override
				public URI getURI() {
					return request.getURI();
				}

				@Override
				public Map<String, Object> getAttributes() {
					return request.getAttributes();
				}

				@Override
				public HttpHeaders getHeaders() {
					HttpHeaders filtered = new HttpHeaders();
					request.getHeaders().forEach((name, values) -> {
						if (!HEADERS_TO_STRIP.contains(name.toLowerCase())) {
							filtered.addAll(name, values);
						}
					});
					return filtered;
				}
			};

			return execution.execute(wrapper, modifiedBody);
		}
	}
}
