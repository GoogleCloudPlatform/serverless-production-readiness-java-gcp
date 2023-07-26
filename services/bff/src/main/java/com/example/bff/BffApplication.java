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
package com.example.bff;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BffApplication {
    // logger
    private static final Log logger = LogFactory.getLog(BffApplication.class);

	public static void main(String[] args) {
		Runtime r = Runtime.getRuntime();
		logger.info("BffApplication: Active processors: " + r.availableProcessors()); 
		logger.info("BffApplication: Total memory: " + r.totalMemory()); 
		logger.info("BffApplication: Free memory: " + r.freeMemory()); 
		logger.info("BffApplication: Max memory: " + r.maxMemory()); 
				
		SpringApplication.run(BffApplication.class, args);
		logger.info("BffApplication: Running app");
	}

	@PostConstruct
    public void init() {
        logger.info("BffApplication: Post Construct Initializer"); 
    }

	@PreDestroy
	public void shutDown(){
		logger.info(BffApplication.class.getSimpleName() + ": received SIGTERM ==> Shutting down resources !");
	}
}
