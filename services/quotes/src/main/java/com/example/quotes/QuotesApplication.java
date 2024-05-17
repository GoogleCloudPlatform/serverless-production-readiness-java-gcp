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
package com.example.quotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.context.annotation.ImportRuntimeHints;

import org.flywaydb.core.internal.configuration.PostgreSQLConfigurationExtension;


/**
 * Application to manage book quotes
 */
@ImportRuntimeHints(FlywayReflectionHints.class)
@SpringBootApplication
public class QuotesApplication {

  public static void main(String[] args) {
    Runtime r = Runtime.getRuntime();

    System.out.println("Runtime Data:");
    System.out.println("QuotesApplication: Active processors: " + r.availableProcessors());    
		System.out.println("QuotesApplication: Total memory: " + r.totalMemory()); 
		System.out.println("QuotesApplication: Free memory: " + r.freeMemory()); 
		System.out.println("QuotesApplication: Max memory: " + r.maxMemory()); 

    SpringApplication.run(QuotesApplication.class, args);
  }

  class FlywayReflectionHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerMethod(
                PostgreSQLConfigurationExtension.class.getDeclaredMethod("isTransactionalLock"),
                ExecutableMode.INVOKE
        );
        hints.reflection().registerMethod(
                PostgreSQLConfigurationExtension.class.getDeclaredMethod("getNamespace"),
                ExecutableMode.INVOKE
        );        
    }
}
}
