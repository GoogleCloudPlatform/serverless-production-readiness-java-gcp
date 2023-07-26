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
package com.example.bff.actuator;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id="startup")
public class StartupCheck {
    // logger
    private static final Log logger = LogFactory.getLog(StartupCheck.class);

    private static boolean status = false;

    public static void up(){ status = true;}
    public static void down(){ status = false;}

    @ReadOperation
    public CustomData customEndpoint() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (!status) {
            logger.info("BFF Startup Endpoint: Application is ready to serve traffic !");
            return null;
        }

        logger.info("BFF Startup Endpoint: Application is ready to serve traffic !");

        CustomData data = new CustomData();
        details.put("StartupEndpoint", "BFF Startup Endpoint: Application is ready to serve traffic");
        data.setData(details);

        return data;
    }
}