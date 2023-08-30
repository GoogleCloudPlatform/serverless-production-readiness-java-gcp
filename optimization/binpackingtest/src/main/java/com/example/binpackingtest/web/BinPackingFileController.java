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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BinPackingFileController {
  private static final Logger logger = LoggerFactory.getLogger(BinPackingFileController.class);

  @PostConstruct
  public void init() {
    logger.info("BinPackingApplication: BinPackingController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    logger.info("BinPackingApplication: BinPackingController Post Construct - StartupCheck can be enabled");
  }

  @GetMapping("/file")
  public ResponseEntity<String> getBinPackingResults(@RequestParam(value="limit", defaultValue = "100") Integer limit,
                                                     @RequestParam(value="bin", defaultValue = "100") Integer bin,
                                                     @RequestParam(value="itemmaxweight", defaultValue = "27") Integer itemMaxWeight){

    File resource;
    Integer fileItems = 0;
    double weights[] = new double[limit];

    try {
      resource = new ClassPathResource(String.format("data/%d.csv", limit)).getFile();
      Scanner scanner = new Scanner(resource);

      while(scanner.hasNextDouble()){
        weights[fileItems++] = scanner.nextDouble();
      }
      System.out.println(String.format("Read CSV file with %d", fileItems));

    } catch (IOException e) {
      return new ResponseEntity<>(String.format("Could not read %d.csv file", limit), HttpStatus.NO_CONTENT);
    }

    String result = BinPacking.testBinPacking(limit, bin, itemMaxWeight, weights);

    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
