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
package org.oakgp;

import org.oakgp.function.Fn;
import org.oakgp.function.choice.If;
import org.oakgp.function.choice.OrElse;
import org.oakgp.function.classify.IsNegative;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.function.classify.IsZero;
import org.oakgp.function.coll.Count;
import org.oakgp.function.compare.*;
import org.oakgp.function.hof.Filter;
import org.oakgp.function.hof.Reduce;
import org.oakgp.function.math.IntFunc;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.node.VariableNode;
import org.oakgp.primitive.VariableSet;
import org.oakgp.rank.Evolved;
import org.oakgp.rank.Ranking;
import org.oakgp.serialize.NodeReader;
import org.oakgp.serialize.NodeWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.oakgp.NodeType.*;
import static org.oakgp.util.Utils.intArrayType;

public class TestUtils {
    public static final VariableSet VARIABLE_SET = VariableSet.of(intArrayType(100));
    private static final Fn[] FUNCTIONS = createDefaultFunctions();

    public static void assertVariable(int expectedId, Node node) {
        assertTrue(node instanceof VariableNode);
        assertEquals(expectedId, ((VariableNode) node).getId());
    }

    public static void assertConstant(Object expectedValue, Node node) {
        assertTrue(node instanceof ConstantNode);
        assertEquals(expectedValue, node.eval(null));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void assertUnmodifiable(List list) {
        String ln = list.getClass().getName();
        assertTrue( ln.startsWith("java.util.ImmutableCollections$List") || ln.equals("java.util.Collections$UnmodifiableRandomAccessList"));
        try {
            list.add(new Object());
            fail("");
        } catch (UnsupportedOperationException e) {
            
        }
    }

    public static String writeNode(Node input) {
        return new NodeWriter().writeNode(input);
    }

    public static FnNode readFunctionNode(String input) {
        return (FnNode) readNode(input);
    }

    public static Node readNode(String input) {
        List<Node> outputs = readNodes(input);
        assertEquals(1, outputs.size());
        return outputs.get(0);
    }

    public static List<Node> readNodes(String input) {
        return readNodes(input, FUNCTIONS, VARIABLE_SET);
    }

    private static List<Node> readNodes(String input, Fn[] functions, VariableSet variableSet) {
        List<Node> outputs = new ArrayList<>();
        try (NodeReader nr = new NodeReader(input, functions, new ConstantNode[0], variableSet)) {
            while (!nr.isEndOfStream()) {
                outputs.add(nr.readNode());
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException caught reading: " + input, e);
        }
        return outputs;
    }

    private static Fn[] createDefaultFunctions() {
        List<Fn> functions = new ArrayList<>();

        functions.add(IntFunc.the.add);
        functions.add(IntFunc.the.subtract);
        functions.add(IntFunc.the.multiply);
        functions.add(IntFunc.the.divide);

        functions.add(LessThan.create(integerType()));
        functions.add(LessThanOrEqual.create(integerType()));
        functions.add(new GreaterThan(integerType()));
        functions.add(new GreaterThanOrEqual(integerType()));
        functions.add(new Equal(integerType()));
        functions.add(new NotEqual(integerType()));

        functions.add(new If(integerType()));
        functions.add(new OrElse(stringType()));
        functions.add(new OrElse(integerType()));

        functions.add(new Reduce(integerType()));
        functions.add(new Filter(integerType()));
        functions.add(new org.oakgp.function.hof.Map(integerType(), booleanType()));

        functions.add(new IsPositive());
        functions.add(new IsNegative());
        functions.add(new IsZero());

        functions.add(new Count(integerType()));
        functions.add(new Count(booleanType()));

        return functions.toArray(new Fn[functions.size()]);
    }

    public static Arguments createArguments(String... expressions) {
        Node[] args = new Node[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            args[i] = readNode(expressions[i]);
        }
        return new Arguments(args);
    }

    public static ConstantNode integerConstant(int value) {
        return new ConstantNode(value, integerType());
    }

    public static ConstantNode longConstant(long value) {
        return new ConstantNode(value, longType());
    }

    public static ConstantNode doubleConstant(double value) {
        return new ConstantNode(value, doubleType());
    }

    public static ConstantNode bigIntegerConstant(String value) {
        return new ConstantNode(new BigInteger(value), bigIntegerType());
    }

    public static ConstantNode bigDecimalConstant(String value) {
        return new ConstantNode(new BigDecimal(value), bigDecimalType());
    }

    public static ConstantNode booleanConstant(Boolean value) {
        return new ConstantNode(value, NodeType.booleanType());
    }

    public static ConstantNode stringConstant(String value) {
        return new ConstantNode(value, NodeType.stringType());
    }

    public static VariableNode createVariable(int id) {
        return VARIABLE_SET.get(id);
    }

    public static void assertRankedCandidate(Evolved actual, Node expectedNode, double expectedFitness) {
        assertSame(expectedNode, actual.id);
        assertEquals(expectedFitness, actual.pri(), 0.001f);
    }

    public static void assertNodeEquals(String expected, Node actual) {
        assertEquals(expected, writeNode(actual));
    }

    public static Ranking singletonRankedCandidates() {
        return singletonRankedCandidates(1);
    }

    public static Ranking singletonRankedCandidates(double fitness) {
        Ranking r = new Ranking(1);
        r.add(new Evolved(mockNode(), fitness));
        return r;
    }

    public static Node mockNode() {
        return mockNode(integerType());
    }

    public static Node mockNode(NodeType type) {
        Node mockNode = mock(Node.class);
        given(mockNode.returnType()).willReturn(type);
        return mockNode;
    }
}
