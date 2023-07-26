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

import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuration to start up Audit application using Testcontainers
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {
  private static FirestoreEmulatorContainer firestoreEmulator = null;

  @Bean
  public Firestore firestoreContainer(){
    firestoreEmulator = new FirestoreEmulatorContainer(
        DockerImageName.parse(
            "gcr.io/google.com/cloudsdktool/cloud-sdk:439.0.0-emulators"));

    firestoreEmulator.start();

    FirestoreOptions options = FirestoreOptions.getDefaultInstance().toBuilder()
        .setHost(firestoreEmulator.getEmulatorEndpoint())
        .setCredentials(NoCredentials.getInstance())
        .setProjectId("fake-test-project-id")
        .build();
    return options.getService();
  }

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.gcp.firestore.host-port", firestoreEmulator::getEmulatorEndpoint);
  }
}