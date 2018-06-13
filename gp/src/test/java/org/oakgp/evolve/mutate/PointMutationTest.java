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
package org.oakgp.evolve.mutate;

import org.junit.jupiter.api.Test;
import org.oakgp.evolve.GeneticOperator;
import org.oakgp.function.Function;
import org.oakgp.function.classify.IsZero;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.FunctionNode;
import org.oakgp.node.Node;
import org.oakgp.primitive.DummyPrimitiveSet;
import org.oakgp.primitive.PrimitiveSet;
import org.oakgp.select.DummyNodeSelector;
import org.oakgp.util.DummyRandom;
import org.oakgp.util.DummyValuesMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.oakgp.TestUtils.integerConstant;
import static org.oakgp.util.DummyRandom.GetIntExpectation.nextInt;

public class PointMutationTest {
    @Test
    public void testTerminal() {
        Node input = integerConstant(9);
        Node output = integerConstant(7);
        DummyNodeSelector dummySelector = new DummyNodeSelector(input);
        DummyValuesMap<Node, Node> alternativeTerminals = new DummyValuesMap.Builder<Node, Node>().put(input, output).build();
        PrimitiveSet primitiveSet = new DummyPrimitiveSet() {
            @Override
            public Node nextAlternativeTerminal(Node current) {
                return alternativeTerminals.next(current);
            }
        };
        GeneticOperator pointMutation = new PointMutation(DummyRandom.EMPTY, primitiveSet);

        Node offspring = pointMutation.apply(dummySelector);

        assertSame(output, offspring);

        dummySelector.assertEmpty();
    }

    @Test
    public void testFunctionSubNodes() {
        DummyRandom dummyRandom = nextInt(3).returns(1, 2, 0);
        Function rootFunction = new IsZero();
        Function inputFunction = IntFunc.the.add;
        Function outputFunction = IntFunc.the.subtract;
        Node inputArg1 = integerConstant(3);
        Node inputArg2 = integerConstant(7);
        Node input = new FunctionNode(rootFunction, new FunctionNode(inputFunction, inputArg1, inputArg2));
        DummyNodeSelector dummySelector = new DummyNodeSelector(input, input, input);
        Node outputArg1 = integerConstant(9);
        Node outputArg2 = integerConstant(2);
        DummyValuesMap<Function, Function> alternativeFunctions = new DummyValuesMap<>(inputFunction, outputFunction);
        DummyValuesMap<Node, Node> alternativeTerminals = new DummyValuesMap.Builder<Node, Node>().put(inputArg1, outputArg1).put(inputArg2, outputArg2).build();
        PrimitiveSet primitiveSet = new DummyPrimitiveSet() {
            @Override
            public Function nextAlternativeFunction(Function current) {
                return alternativeFunctions.next(current);
            }

            @Override
            public Node nextAlternativeTerminal(Node current) {
                return alternativeTerminals.next(current);
            }
        };
        GeneticOperator pointMutation = new PointMutation(dummyRandom, primitiveSet);

        assertEquals(new FunctionNode(rootFunction, new FunctionNode(inputFunction, inputArg1, outputArg2)), pointMutation.apply(dummySelector));
        assertEquals(new FunctionNode(rootFunction, new FunctionNode(outputFunction, inputArg1, inputArg2)), pointMutation.apply(dummySelector));
        assertEquals(new FunctionNode(rootFunction, new FunctionNode(inputFunction, outputArg1, inputArg2)), pointMutation.apply(dummySelector));

        dummyRandom.assertEmpty();
        dummySelector.assertEmpty();
    }
}
