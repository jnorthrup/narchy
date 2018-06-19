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
package org.oakgp.generate;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.NodeType;
import org.oakgp.function.Fn;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;
import org.oakgp.primitive.*;
import org.oakgp.util.DummyRandom;
import org.oakgp.util.Signature;

import static org.oakgp.NodeType.integerType;
import static org.oakgp.NodeType.type;
import static org.oakgp.TestUtils.assertNodeEquals;
import static org.oakgp.TestUtils.integerConstant;
import static org.oakgp.function.math.IntFunc.the;
import static org.oakgp.util.DummyRandom.random;

public class TreeGeneratorTest {
    @Test
    public void testFull() {
        PrimitiveSet p = createPrimitiveSet();
        TreeGenerator g = TreeGeneratorImpl.full(p);
        Node result = g.generate(integerType(), 3);
        assertNodeEquals("(+ (+ (+ 1 2) (+ 3 4)) (+ (+ 5 6) (+ 7 8)))", result);
    }

    @Test
    public void testGrow() {
        PrimitiveSet p = createPrimitiveSet();
        TreeGenerator g = TreeGeneratorImpl.grow(p, random().setBooleans(true, true, false, true, true, true, false).build());
        Node result = g.generate(integerType(), 3);
        assertNodeEquals("(+ (+ 1 (+ 2 3)) (+ 6 (+ 4 5)))", result);
    }

    /**
     * Tests building a tree when the return types of the elements of the primitive set force a specific result.
     * <p>
     * i.e. In this test there is only one function or terminal node for each of the required types.
     */
    @Test
    public void testWhenTypesEnforceStructure() {
        Fn f1 = createFunction("f1", type("a"), type("b"));
        Fn f2 = createFunction("f2", type("b"), type("c"), type("d"));
        ConstantNode c = new ConstantNode("X", type("c"));

        DummyRandom random = random().setDoubles(1d, 1d).build();
        PrimitiveSet p = new PrimitiveSetImpl(new FnSet(f1, f2), NodeSet.byType(c), VariableSet.of(type("d")), random, .5);
        TreeGenerator g = TreeGeneratorImpl.full(p);
        Node result = g.generate(type("a"), 3);
        assertNodeEquals("(f1 (f2 X v0))", result);
    }

    private PrimitiveSet createPrimitiveSet() {
        PrimitiveSet p = new DummyPrimitiveSet() {
            int terminalCtr = 1;

            @Override
            public Fn next(NodeType type) {
                return the.add;
            }

            @Override
            public Node nextTerminal(NodeType type) {
                return integerConstant(terminalCtr++);
            }
        };
        return p;
    }

    private Fn createFunction(String displayName, NodeType returnType, NodeType... arguments) {
        return new Fn() {
            @Override
            public Object evaluate(Arguments arguments, Assignments assignments) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Signature sig() {
                return new Signature(returnType, arguments);
            }

            @Override
            public String name() {
                return displayName;
            }
        };
    }
}
