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
package com.example.binpackingtest.web;

import com.example.binpackingtest.utility.BinPacking;
import jakarta.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BinPackingRandomController {
  private static final Logger logger = LoggerFactory.getLogger(BinPackingRandomController.class);

  @PostConstruct
  public void init() {
    logger.info("BinPackingApplication: BinPackingController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    logger.info("BinPackingApplication: BinPackingController Post Construct - StartupCheck can be enabled");
  }

  @GetMapping("/random")
  public ResponseEntity<String> getBinPackingResults(@RequestParam(value="limit", defaultValue = "100") Integer limit,
                                                     @RequestParam(value="bin", defaultValue = "100") Integer bin,
                                                     @RequestParam(value="itemmaxweight", defaultValue = "27") Integer itemMaxWeight){
    String result = BinPacking.testBinPacking(limit, bin, itemMaxWeight, null);

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
