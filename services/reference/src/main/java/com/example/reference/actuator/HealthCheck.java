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
package  com.example.reference.actuator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("customHealthCheck")
public class HealthCheck implements HealthIndicator {
    // logger
    private static final Log logger = LogFactory.getLog(HealthCheck.class);

    @Override
    public Health health() {
        int errorCode = check(); // perform some specific health check

        if (errorCode != 0) {
            logger.error("Reference Application: failed health check with error code " + errorCode);
            return Health.down()
                    .withDetail("Custom Health Check Status - failed. Error Code", errorCode).build();
        }

        logger.info("Reference Application: Custom Health Check - passed");
        return Health.up().withDetail("Reference Application: Custom Health Check Status ", "passed").build();
    }

    public int check() {
        // custom logic - check health
        return 0;
    }
}
