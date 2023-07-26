package com.example.quotes;
/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.example.quotes.domain.QuoteRepository;
import com.example.quotes.domain.QuoteService;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Simulate and test network failures for the Quotes application
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuotesApplicationNetworkFailuresTests {
	private static final Logger logger = LoggerFactory.getLogger(QuotesApplicationNetworkFailuresTests.class);

	// @Rule
	private static final Network network = Network.newNetwork();

	private static Proxy postgresqlProxy;

	@Container
	private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3-alpine")
			.withNetwork(network).withNetworkAliases("postgres");

	@Autowired
	private QuoteRepository quoteRepository;
	private QuoteService quoteService;

	@Container
	private static final ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
			.withNetwork(network);

	@DynamicPropertySource
	static void sqlserverProperties(DynamicPropertyRegistry registry) throws IOException {
		var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
		postgresqlProxy = toxiproxyClient.createProxy("postgresql", "0.0.0.0:8666", "postgres:5432");

		registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/%s".formatted(toxiproxy.getHost(),
				toxiproxy.getMappedPort(8666), postgres.getDatabaseName()));
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.flyway.url", postgres::getJdbcUrl);
		registry.add("spring.flyway.user", postgres::getUsername);
		registry.add("spring.flyway.password", postgres::getPassword);
	}
	@BeforeEach
	void setUp() throws IOException {
		quoteService = new QuoteService(quoteRepository);

		postgresqlProxy.toxics().getAll().forEach(toxic -> {
			try {
				toxic.remove();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	@DisplayName("The normal test, no Toxi proxy")
	void testRandomQuotes() {
	  var quote = this.quoteService.findRandomQuote();
	  assertThat(quote).isNotNull();
	}

	@Test
	@DisplayName("Test with latency, no timeout")
	void withLatency() throws IOException{
		postgresqlProxy.toxics().latency("postgresql-latency", ToxicDirection.DOWNSTREAM, 1600).setJitter(100);

		var quote = this.quoteService.findRandomQuote();
		assertThat(quote).isNotNull();
	}

	@Test
	@DisplayName("Test with latency, timeout encountered")
	void withLatencyandTimeout() throws IOException{
		postgresqlProxy.toxics().latency("postgresql-latency", ToxicDirection.DOWNSTREAM, 1600).setJitter(100);

		try {
			assertTimeout(Duration.ofSeconds(1), () -> {
				this.quoteService.findRandomQuote();
			});
		}catch (AssertionFailedError e){
			System.out.println("Test and encounter exception" + e.getMessage());
		}
	}

	@Test
	@DisplayName("Retry: Test with timeout, remove latency, then retry successfully")
	void withLatencyWithRetries() throws IOException {
		Latency latency = postgresqlProxy.toxics().latency("postgresql-latency", ToxicDirection.DOWNSTREAM, 1600)
				.setJitter(100);

		System.out.println("First request fails with timeout");
		try {
			assertTimeout(Duration.ofSeconds(1), () -> {
				this.quoteService.findRandomQuote();
			});
		}catch (AssertionFailedError e){
			System.out.println("Test and encounter exception" + e.getMessage());
		}

		System.out.println("Remove latency, second request should succeed");
		try {
			latency.remove();
		}catch(IOException e){
			throw new RuntimeException(e);
		}

		var quote = this.quoteService.findRandomQuote();
		assertThat(quote).isNotNull();
	}

	@Test
	void withToxiProxyConnectionDown() throws IOException {
		postgresqlProxy.toxics().bandwidth("postgres-cut-connection-downstream", ToxicDirection.DOWNSTREAM, 0);
		postgresqlProxy.toxics().bandwidth("postgres-cut-connection-upstream", ToxicDirection.UPSTREAM, 0);

		try {
			assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
				var quote = this.quoteService.getAllQuotes();
				assertThat(quote).isNotNull();
			});
		}catch (AssertionFailedError e){
			System.out.println("Test and encounter exception" + e.getMessage());
		}

		postgresqlProxy.toxics().get("postgres-cut-connection-downstream").remove();
		postgresqlProxy.toxics().get("postgres-cut-connection-upstream").remove();

		var quote = this.quoteService.findRandomQuote();
		assertThat(quote).isNotNull();
	}
}
