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

import com.example.audit.domain.AuditService;
import com.google.api.core.ApiFuture;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Test persisting data in Firestore repository
 */
@SpringBootTest
@Testcontainers
//@ActiveProfiles("test")
public class AuditApplicationFirestoreContainerTests {
  @BeforeEach
  public void setup() {
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
              "gcr.io/google.com/cloudsdktool/cloud-sdk:439.0.0-emulators"));

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.gcp.firestore.host-port", firestoreEmulator::getEmulatorEndpoint);
  }

  // @Autowired
  private AuditService eventService;

//  @Disabled("Until Spring Boot 3.1 is released")
  @Test
  @DisplayName("Test persisting audit data for a Quote in Firestore")
  void testEventRepositoryAuditQuoteData() throws ExecutionException, InterruptedException {
    ApiFuture<WriteResult> writeResult = eventService.auditQuote("test quote", "test author", "test book", UUID.randomUUID().toString());
    Assertions.assertNotNull(writeResult.get().getUpdateTime());
  }
}