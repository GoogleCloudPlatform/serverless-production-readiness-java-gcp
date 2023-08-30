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

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/** Bin packing problem. */
public class BinPacking {
  static class DataModel {

    public int limit;
    public double[] weights;
    public int numItems;
    public int numBins;
    public int binCapacity;
    public int itemMaxWeight;

    public DataModel(int limit, int binCapacity, int itemMaxWeight, double[] weights) {
      this.numItems = limit;
      this.numBins = limit;
      this.binCapacity = binCapacity;
      this.itemMaxWeight = itemMaxWeight;

      if(weights == null){
        this.weights = new double[limit];

        Random randomDouble = new Random();
        // account for the fact that an item can not have weight = 0
        for (int counter = 0; counter < limit; counter++)
          this.weights[counter] = randomDouble.nextDouble(1, itemMaxWeight);
      } else {
        this.weights = weights;
      }
    }
  }

  private BinPacking() {}

  public static String testBinPacking(int limit, int capacity, int itemMaxWeight, double[] weights){
    String response;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    LocalTime localTime = LocalTime.now();
    System.out.println("Start... " + dtf.format(localTime));

    long loaded = System.currentTimeMillis();

    final DataModel data = new DataModel(limit, capacity, itemMaxWeight, weights);

    // Create the linear solver with the SCIP backend.
    MPSolver solver = MPSolver.createSolver("SCIP");
    if (solver == null) {
      System.out.println("Could not create solver SCIP");
      return "Could not create solver SCIP";
    }

    MPVariable[][] x = new MPVariable[data.numItems][data.numBins];
    for (int i = 0; i < data.numItems; ++i) {
      for (int j = 0; j < data.numBins; ++j) {
        x[i][j] = solver.makeIntVar(0, 1, "");
      }
    }
    MPVariable[] y = new MPVariable[data.numBins];
    for (int j = 0; j < data.numBins; ++j) {
      y[j] = solver.makeIntVar(0, 1, "");
    }

    double infinity = Double.POSITIVE_INFINITY;
    for (int i = 0; i < data.numItems; ++i) {
      MPConstraint constraint = solver.makeConstraint(1, 1, "");
      for (int j = 0; j < data.numBins; ++j) {
        constraint.setCoefficient(x[i][j], 1);
      }
    }
    // The bin capacity constraint for bin j is
    //   sum_i w_i x_ij <= C*y_j
    // To define this constraint, first subtract the left side from the right to get
    //   0 <= C*y_j - sum_i w_i x_ij
    //
    // Note: Since sum_i w_i x_ij is positive (and y_j is 0 or 1), the right side must
    // be less than or equal to C. But it's not necessary to add this constraint
    // because it is forced by the other constraints.

    for (int j = 0; j < data.numBins; ++j) {
      MPConstraint constraint = solver.makeConstraint(0, infinity, "");
      constraint.setCoefficient(y[j], data.binCapacity);
      for (int i = 0; i < data.numItems; ++i) {
        constraint.setCoefficient(x[i][j], -data.weights[i]);
      }
    }

    MPObjective objective = solver.objective();
    for (int j = 0; j < data.numBins; ++j) {
      objective.setCoefficient(y[j], 1);
    }
    objective.setMinimization();

    final MPSolver.ResultStatus resultStatus = solver.solve();

    // Check that the problem has an optimal solution.
    if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
      System.out.println("Number of bins used: " + objective.value());
      double totalWeight = 0;
      for (int j = 0; j < data.numBins; ++j) {
        if (y[j].solutionValue() == 1) {
          System.out.println("\nBin " + j + "\n");
          double binWeight = 0;
          for (int i = 0; i < data.numItems; ++i) {
            if (x[i][j].solutionValue() == 1) {
              System.out.println("Item " + i + " - weight: " + data.weights[i]);
              binWeight += data.weights[i];
            }
          }
          System.out.println("Packed bin weight: " + binWeight);
          totalWeight += binWeight;
        }
      }
      response = String.format("\nTotal packed weight: %s, \nTotal bins: %s, \nOptimal solution: %d ms",
                              totalWeight,
                              objective.value(),
                              System.currentTimeMillis()-loaded);
      System.out.println(response);
    } else {
      response = String.format("\nThe problem does not have an optimal solution: %d ms",
          System.currentTimeMillis()-loaded);
      System.err.println(response);
    }

    // clean up
    solver.clear();
    solver.delete();

    return response;
  }

  public static void main(String[] args) throws Exception {
    Loader.loadNativeLibraries();

    testBinPacking(100,100, 27, null);
  }

}