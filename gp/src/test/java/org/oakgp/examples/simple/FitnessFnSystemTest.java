/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.examples.simple;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Evolution;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.classify.IsNegative;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.function.classify.IsZero;
import org.oakgp.function.coll.Count;
import org.oakgp.function.compare.*;
import org.oakgp.function.hof.Filter;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.rank.fitness.FitFn;

import java.util.*;

import static org.oakgp.TestUtils.createArguments;
import static org.oakgp.NodeType.*;
import static org.oakgp.rank.fitness.TestDataFitFn.createIntegerTestDataFitnessFunction;
import static org.oakgp.util.Utils.intArrayType;
import static org.oakgp.util.Utils.intConsts;

/**
 * Performs full genetic programming runs without relying on any mock objects.
 * <p>
 * Would be better to have in a separate "system-test" directory under the "src" directory - or in a completely separate Git project (that has this project as a
 * dependency). Leaving here for the moment as it provides a convenient mechanism to perform a full test of the process.
 * </p>
 */
public class FitnessFnSystemTest {
    private static final int NUM_GENERATIONS = 50;
    private static final int INITIAL_POPULATION_SIZE = 50;
    private static final int INITIAL_POPULATION_MAX_DEPTH = 4;
    private static final Fn[] ARITHMETIC_FUNCTIONS = {IntFunc.the.add, IntFunc.the.subtract,
            IntFunc.the.multiply};

    private static Map<Assignments, Integer> createTests(int numVariables, java.util.function.Function<Assignments, Integer> f) {
        int count = 200;
        Map<Assignments, Integer> tests = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            Object[] inputs = createInputs(numVariables);
            Assignments assignments = new Assignments(inputs);
            tests.put(assignments, f.apply(assignments));
        }
        return tests;
    }

    private static Object[] createInputs(int numVariables) {
        Random random = new Random();
        Object[] variables = new Object[numVariables];
        for (int i = 0; i < numVariables; i++) {
            variables[i] = random.nextInt(40);
        }
        return variables;
    }

    @Test
    public void testSymbolicRegressionExample() {
        SymbolicRegressionExample.main(null);
    }

    @Test
    public void testTwoVariableArithmeticExpression() {
        ConstantNode[] constants = intConsts(0, 11);
        NodeType[] variableTypes = intArrayType(2);

        FitFn fitnessFunction = createIntegerTestDataFitnessFunction(createTests(variableTypes.length, a -> {
            int x = (int) a.get(0);
            int y = (int) a.get(1);
            return (x * x) + 2 * y + 3 * x + 5;
        }));

        new Evolution().returns(integerType())
                .constants(constants)
                .variables(variableTypes)
                .functions(ARITHMETIC_FUNCTIONS)
                .goal(fitnessFunction)
                .populationSize(INITIAL_POPULATION_SIZE)
                .populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopGenerations(NUM_GENERATIONS).get();
    }

    @Test
    public void testThreeVariableArithmeticExpression() {
        ConstantNode[] constants = intConsts(0, 11);
        NodeType[] variableTypes = intArrayType(3);

        FitFn fitnessFunction = createIntegerTestDataFitnessFunction(createTests(variableTypes.length, a -> {
            int x = (int) a.get(0);
            int y = (int) a.get(1);
            int z = (int) a.get(2);
            return (x * -3) + (y * 5) - z;
        }));

        new Evolution().returns(integerType()).constants(constants).variables(variableTypes).functions(ARITHMETIC_FUNCTIONS)
                .goal(fitnessFunction)
                .populationSize(INITIAL_POPULATION_SIZE)
                .populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopGenerations(NUM_GENERATIONS).get();
    }

    @Test
    public void testTwoVariableBooleanLogicExpression() {
        ConstantNode[] constants = intConsts(0, 5);
        NodeType[] variableTypes = intArrayType(2);
        Fn[] functions = {IntFunc.the.add, IntFunc.the.subtract, IntFunc.the.multiply,
                LessThan.create(integerType()), LessThanOrEqual.create(integerType()), new GreaterThan(integerType()), new GreaterThanOrEqual(integerType()),
                new Equal(integerType()), new NotEqual(integerType()), new If(integerType())};

        FitFn fitnessFunction = createIntegerTestDataFitnessFunction(createTests(variableTypes.length, a -> {
            int x = (int) a.get(0);
            int y = (int) a.get(1);
            return x > 20 ? x : y;
        }));

        new Evolution().returns(integerType()).constants(constants).variables(variableTypes).functions(functions)
                .goal(fitnessFunction).populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopGenerations(NUM_GENERATIONS).get();
    }

    @Test
    public void testIsCountOfZerosGreater() {
        IsPositive isPositive = new IsPositive();
        IsNegative isNegative = new IsNegative();
        IsZero isZero = new IsZero();
        ConstantNode[] constants = {new ConstantNode(Boolean.TRUE, booleanType()), new ConstantNode(Boolean.FALSE, booleanType()),
                new ConstantNode(isPositive, integerToBooleanFunctionType()), new ConstantNode(isNegative, integerToBooleanFunctionType()),
                new ConstantNode(isZero, integerToBooleanFunctionType()), new ConstantNode(new Arguments(new Node[]{}), integerArrayType()),
                new ConstantNode(0, integerType())};
        NodeType[] variableTypes = {integerArrayType()};
        List<Fn> functions = new ArrayList<>();
        Collections.addAll(functions, ARITHMETIC_FUNCTIONS);
        Collections.addAll(functions, new Fn[]{new Filter(integerType()), isPositive, isNegative, isZero, new Count(integerType())});

        Map<Assignments, Integer> testData = new HashMap<>();
        testData.put(new Assignments(createArguments("0", "0", "0", "0", "0", "0", "0", "0")), 8);
        testData.put(new Assignments(createArguments("6", "3", "4", "0", "2", "4", "1", "3")), 1);
        testData.put(new Assignments(createArguments("0", "0", "4", "0", "0", "0", "1", "0")), 6);
        testData.put(new Assignments(createArguments("1", "-1", "2", "5", "4", "-2")), 0);
        testData.put(new Assignments(createArguments("1", "0", "2", "5", "4", "-2")), 1);
        testData.put(new Assignments(createArguments("1", "0", "2", "5", "4", "0")), 2);
        testData.put(new Assignments(createArguments("-2", "0", "8", "7", "0", "-3", "0")), 3);
        testData.put(new Assignments(createArguments("0", "0", "0")), 3);
        FitFn fitnessFunction = createIntegerTestDataFitnessFunction(testData);

        new Evolution().returns(integerType()).constants(constants).variables(variableTypes).functions(functions)
                .goal(fitnessFunction).populationSize(INITIAL_POPULATION_SIZE).populationDepth(INITIAL_POPULATION_MAX_DEPTH)
                .stopGenerations(NUM_GENERATIONS).get();
    }
}
