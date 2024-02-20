/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

@SpringBootApplication
public class BookImagesApplication {
	private static final Logger logger = LoggerFactory.getLogger(BookImagesApplication.class);

	public static void main(String[] args) {
		logger.info("BookImagesApplication: Active processors: " + Runtime.getRuntime().availableProcessors());
		logger.info("BookImagesApplication app started : " +
			new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));

		SpringApplication.run(BookImagesApplication.class, args);
		logger.info("BookImagesApplication app  - Spring Boot FW started: " +
			new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
	}

	@PreDestroy
	public void shutDown(){
		logger.info(BookImagesApplication.class.getSimpleName() + ": received SIGTERM ==> Shutting down resources !");
	}	
}
