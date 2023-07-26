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
package com.example.audit;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.example.audit.domain.AuditService;
import com.google.api.core.ApiFuture;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Test network failures for the Audit app integration with dependent services
 */
@SpringBootTest
@Testcontainers
//@ActiveProfiles("test")
public class AuditApplicationNetworkFailuresTests {

  private static final Network network = Network.newNetwork();

  private static Proxy firestoreProxy;
  
  @BeforeEach
  public void setup() throws IOException{
		firestoreProxy.toxics().getAll().forEach(toxic -> {
			try {
				toxic.remove();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

    FirestoreOptions options = FirestoreOptions.getDefaultInstance().toBuilder()
        .setHost(firestoreEmulator.getEmulatorEndpoint())
        .setCredentials(NoCredentials.getInstance())
        .setProjectId("fake-test-project-id")
        .build();
    Firestore firestore = options.getService();

    this.eventService = new AuditService(options, firestore);
  }

  @Container
  private static final FirestoreEmulatorContainer firestoreEmulator =
      new FirestoreEmulatorContainer(
          DockerImageName.parse(
              "gcr.io/google.com/cloudsdktool/cloud-sdk:439.0.0-emulators"))
              .withNetwork(network).withNetworkAliases("firestore");

	@Container
	private static final ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
			.withNetwork(network);
                    
  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) throws IOException{
    var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
    firestoreProxy = toxiproxyClient.createProxy("firestore", "0.0.0.0:8666", "firestore:5637");

    registry.add("spring.cloud.gcp.firestore.host-port", firestoreEmulator::getEmulatorEndpoint);
  }

  // @Autowired
  private AuditService eventService;

  @Test
  @DisplayName("The normal test, no Toxi proxy ")
  void testEventRepositoryStoreQuote() throws ExecutionException, InterruptedException {
    ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
    Assertions.assertNotNull(writeResult.get().getUpdateTime());
  }

  @Test
  @DisplayName("Test with latency, no timeout")
  void testEventRepositoryStoreQuoteWithLatency() throws ExecutionException, InterruptedException, IOException {
    firestoreProxy.toxics().latency("firestore-latency", ToxicDirection.DOWNSTREAM, 1600).setJitter(100);

    ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
    Assertions.assertNotNull(writeResult.get().getUpdateTime());
  }

  @Test
  @DisplayName("Test with latency, timeout encountered")
  void testEventRepositoryStoreQuoteWithLatencyandTimeout() throws ExecutionException, InterruptedException, IOException {
    firestoreProxy.toxics().latency("firestore-latency", ToxicDirection.DOWNSTREAM, 1600).setJitter(100);

		try {
			assertTimeout(Duration.ofSeconds(1), () -> {
        ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
        Assertions.assertNotNull(writeResult.get().getUpdateTime());
			});
		}catch (AssertionFailedError e){
			System.out.println("Test and encounter exception" + e.getMessage());
		}
	}

  @Test
	@DisplayName("Retry: Test with timeout, remove latency, then retry successfully")
	void testEventRepositoryStoreQuoteWithTimeoutandRetries() throws IOException, InterruptedException, ExecutionException {
    Latency latency = firestoreProxy.toxics().latency("firestore-latency", ToxicDirection.DOWNSTREAM, 1600).setJitter(100);

		try {
			assertTimeout(Duration.ofSeconds(1), () -> {
        ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
        Assertions.assertNotNull(writeResult.get().getUpdateTime());    
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

    ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
    Assertions.assertNotNull(writeResult.get().getUpdateTime());    
  }

	@Test
	void withToxiProxyConnectionDown() throws IOException, InterruptedException, ExecutionException {
		firestoreProxy.toxics().bandwidth("firestore-cut-connection-downstream", ToxicDirection.DOWNSTREAM, 0);
		firestoreProxy.toxics().bandwidth("firestore-cut-connection-upstream", ToxicDirection.UPSTREAM, 0);

		try {
			assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
        ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
        Assertions.assertNotNull(writeResult.get().getUpdateTime());    
          });
		}catch (AssertionFailedError e){
			System.out.println("Test and encounter exception" + e.getMessage());
		}

		firestoreProxy.toxics().get("firestore-cut-connection-downstream").remove();
		firestoreProxy.toxics().get("firestore-cut-connection-upstream").remove();

    ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
    Assertions.assertNotNull(writeResult.get().getUpdateTime());    
    System.out.println("Restore network connection and test succesfully!");
	}  
}