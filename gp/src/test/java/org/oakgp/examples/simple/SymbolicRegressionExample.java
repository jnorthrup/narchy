/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.examples.simple;

import org.oakgp.Assignments;
import org.oakgp.Evolution;
import org.oakgp.Type;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.Node;
import org.oakgp.rank.Candidates;
import org.oakgp.util.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.oakgp.rank.fitness.TestDataFitnessFunction.createIntegerTestDataFitnessFunction;

/**
 * An example of using symbolic regression to evolve a program that best fits a given data set for the function {@code x2 + x + 1}.
 */
public class SymbolicRegressionExample {
    private static final int TARGET_FITNESS = 0;
    private static final int INITIAL_POPULATION_SIZE = 50;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;

    public static void main(String[] args) {


        Candidates o = new Evolution()
                .returns(Type.integerType())
                .constants(Utils.intConsts(0, 10)) // the constant set will contain the integers in the range 0-10 inclusive
                .variables(Type.integerType()) // the variable set will contain a single variable - representing the integer value input to the function
                .functions(
                        IntFunc.the.add,
                        IntFunc.the.subtract,
                        IntFunc.the.multiply
                )
                .goal(createIntegerTestDataFitnessFunction(createDataSet())) // the fitness function will compare candidates against a data set which maps inputs to their expected outputs
                .population(INITIAL_POPULATION_SIZE)
                .depth(INITIAL_POPULATION_MAX_DEPTH)
                .goalTarget(TARGET_FITNESS)
                .get();

        System.out.println(o);
        Node best = o.best().node;
        System.out.println(best);
    }

    /**
     * Returns the data set used to assess the fitness of candidates.
     * <p>
     * Creates a map of input values in the range [-10,+10] to the corresponding expected output value.
     */
    private static Map<Assignments, Integer> createDataSet() {
        Map<Assignments, Integer> tests = new HashMap<>();
        for (int i = -10; i < 11; i++) {
            Assignments assignments = new Assignments(i);
            tests.put(assignments, getExpectedOutput(i));
        }
        return tests;
    }

    private static int getExpectedOutput(int x) {
        return (x * x) + x + 1;
    }
}
