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
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.primitive.DummyPrimitiveSet;
import org.oakgp.primitive.PrimitiveSet;
import org.oakgp.select.DummyNodeSelector;
import org.oakgp.util.DummyRandom;
import org.oakgp.util.GPRandom;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.oakgp.TestUtils.*;
import static org.oakgp.util.DummyRandom.GetIntExpectation.nextInt;

public class ShrinkMutationTest {
    @Test
    public void testTerminal() {
        ConstantNode constant = integerConstant(1);
        Node result = shrinkMutate(DummyRandom.EMPTY, new DummyPrimitiveSet(), constant);
        assertSame(constant, result);
    }

    @Test
    public void testFunctionNodeWithTerminalArguments() {
        Node input = readNode("(+ v0 v1)");
        ConstantNode expectedResult = integerConstant(42);
        PrimitiveSet primitiveSet = new DummyPrimitiveSet() {
            @Override
            public Node nextAlternativeTerminal(Node nodeToReplace) {
                assertSame(input, nodeToReplace);
                return expectedResult;
            }
        };
        Node result = shrinkMutate(DummyRandom.EMPTY, primitiveSet, input);
        assertSame(expectedResult, result);
    }

    @Test
    public void testFunctionNode() {
        Node input = readNode("(+ (+ (if (zero? v0) 7 8) v1) (+ 9 v2))");
        DummyNodeSelector selector = DummyNodeSelector.repeat(4, input);
        ConstantNode expectedResult = integerConstant(42);
        PrimitiveSet primitiveSet = new DummyPrimitiveSet() {
            @Override
            public Node nextAlternativeTerminal(Node nodeToReplace) {
                return expectedResult;
            }
        };
        DummyRandom random = nextInt(4).returns(1, 3, 0, 2);
        ShrinkMutation mutator = new ShrinkMutation(random, primitiveSet);

        assertNodeEquals("(+ (+ 9 v2) (+ v1 (if 42 7 8)))", mutator.apply(selector));
        assertNodeEquals("(+ 42 (+ 9 v2))", mutator.apply(selector));
        assertNodeEquals("(+ 42 (+ v1 (if (zero? v0) 7 8)))", mutator.apply(selector));
        assertNodeEquals("(+ (+ 42 v1) (+ 9 v2))", mutator.apply(selector));

        selector.assertEmpty();
        random.assertEmpty();
    }

    private Node shrinkMutate(GPRandom random, PrimitiveSet primitiveSet, Node input) {
        DummyNodeSelector selector = new DummyNodeSelector(input);
        Node result = new ShrinkMutation(random, primitiveSet).apply(selector);
        selector.assertEmpty();
        return result;
    }
}
