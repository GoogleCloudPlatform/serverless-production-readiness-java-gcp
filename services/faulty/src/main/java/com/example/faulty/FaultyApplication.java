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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
@SpringBootApplication
public class FaultyApplication {
    // logger
    private static final Log logger = LogFactory.getLog(FaultyApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(FaultyApplication.class, args);
	}

	@PostConstruct
    public void init() {
        logger.info("FaultyApplication: Post Construct Initializer"); 
    }

	@PreDestroy
	public void shutDown(){
		logger.info(FaultyApplication.class.getSimpleName() + ": received SIGTERM ==> Shutting down resources !");
	}

}
