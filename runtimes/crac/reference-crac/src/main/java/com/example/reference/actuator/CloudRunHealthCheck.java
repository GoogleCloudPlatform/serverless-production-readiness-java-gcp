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
package com.example.reference.actuator;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id="cloudrunhealth")
public class CloudRunHealthCheck {
    // logger
    private static final Logger logger = LoggerFactory.getLogger(StartupCheck.class);

    @ReadOperation
    public CustomData customEndpoint() {
        Map<String, Object> details = new LinkedHashMap<>();        
        logger.info("Reference Application: Custom Health Check - passed");

        details.put("CloudRunHealthCheckEndpoint", "Reference Health Check: passed");

        CustomData data = new CustomData();
        data.setData(details);

        return data;
    }
}