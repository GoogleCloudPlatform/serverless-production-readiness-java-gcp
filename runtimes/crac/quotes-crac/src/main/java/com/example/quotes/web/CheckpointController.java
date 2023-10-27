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
package com.example.quotes.web;

import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContextException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CheckpointController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Checkpoint endpoint for demo purpose, could be a secured Spring Boot actuator endpoint for example.
	 */
	@PostMapping("/checkpoint")
	void checkpoint() {
		logger.info("Triggering JVM checkpoint/restore");
		try {
			Core.checkpointRestore();
		}
		catch (UnsupportedOperationException ex) {
			throw new ApplicationContextException("CRaC checkpoint not supported on current JVM", ex);
		}
		catch (CheckpointException ex) {
			throw new ApplicationContextException("Failed to take CRaC checkpoint on refresh", ex);
		}
		catch (RestoreException ex) {
			throw new ApplicationContextException("Failed to restore CRaC checkpoint on refresh", ex);
		}
	}
}
