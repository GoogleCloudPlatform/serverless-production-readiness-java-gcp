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
package com.example.binpackingtest.utility;

import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;

public class GenerateCSVFile {

  public static void main(String[] args) throws IOException {
    // Create a new Random object.
    Random random = new Random();
    DecimalFormat df = new DecimalFormat("0.00");
    df.setRoundingMode(RoundingMode.DOWN);

    int fileSize = 100000;
    // Create a FileWriter object to write to the CSV file.
    FileWriter writer = new FileWriter(String.format("%d.csv", fileSize));

    // Generate 100 random numbers and write them to the CSV file.
    for (int i = 0; i < fileSize; i++) {
      double number = random.nextDouble(1,27);
      writer.write(df.format(number) + "\n");
    }

    // Close the FileWriter object.
    writer.close();
  }
}
