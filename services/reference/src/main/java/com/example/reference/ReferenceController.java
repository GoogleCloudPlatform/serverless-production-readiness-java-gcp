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
package com.example.reference;

import com.example.reference.data.Metadata;
import com.google.cloud.MetadataConfig;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReferenceController {
  @Value("${delay}")
  Long delay;

  // logger
  private static final Log logger = LogFactory.getLog(ReferenceController.class);

  @GetMapping("start")
  String start() {
    logger.info("ReferenceApplication: ReferenceController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    return "ReferenceController started";
  }

  @GetMapping("/metadata") 
  public ResponseEntity<Metadata> metadata(){
    Metadata data = new Metadata();
    data.setProjectID(MetadataConfig.getProjectId());
    data.setZone(MetadataConfig.getZone());
    data.setInstanceID(MetadataConfig.getInstanceId());

    // introduce an artificial delay
    try {
      TimeUnit.SECONDS.sleep(delay);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    return new ResponseEntity<Metadata>(data, HttpStatus.OK);
  }
}
