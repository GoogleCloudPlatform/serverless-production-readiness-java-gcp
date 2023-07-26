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
package com.example.faulty;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Component
public class FaultyService {

    private final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("faultyService");

    //TODO: add logic to simulate a backing service that is faulty in the following ways:
    // - Leaking resources, gradually slowing response times and triggering a HALF_OPEN state
    // - Unreliable health indicator, requiring retries to maintain an accurate error rate
    // - Limited concurrency, slowing down significantly over X concurrent requests, requiring a bulkhead
    public String getDataFromFaultyBackingService() {
        return circuitBreaker.decorateSupplier(() -> "Working as intended").get();
    }
}
