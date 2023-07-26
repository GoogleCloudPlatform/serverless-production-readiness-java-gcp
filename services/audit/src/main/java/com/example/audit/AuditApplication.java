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

import java.text.SimpleDateFormat;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application to persist audit information about Quote updates
 */
@SpringBootApplication
public class AuditApplication {
	private static final Logger logger = LoggerFactory.getLogger(AuditApplication.class);

	public static void main(String[] args) {
		logger.info("AuditApplication: Active processors: " + Runtime.getRuntime().availableProcessors());
		logger.info("AuditApplication app started : " +
			new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));

		SpringApplication.run(AuditApplication.class, args);
		logger.info("AuditApplication app  - Spring Boot FW started: " +
			new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
	}

	@PreDestroy
	public void shutDown(){
		logger.info(AuditApplication.class.getSimpleName() + ": received SIGTERM ==> Shutting down resources !");
	}	
}
