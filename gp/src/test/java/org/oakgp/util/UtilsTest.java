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
package org.oakgp.util;

import org.junit.jupiter.api.Test;
import org.oakgp.NodeType;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.Node;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.NodeType.*;
import static org.oakgp.TestUtils.*;
import static org.oakgp.util.DummyRandom.GetIntExpectation.nextInt;

public class UtilsTest {
    @Test
    public void testGroupByType() {
        Node n1 = mockNode(integerType());
        Node n2 = mockNode(stringType());
        Node n3 = mockNode(integerType());
        Node n4 = mockNode(integerType());
        Node n5 = mockNode(booleanType());
        Node[] values = {n1, n2, n3, n4, n5};
        Map<NodeType, Node[]> groups = Utils.groupByType(values);
        assertEquals(3, groups.size());
        assertArrayEquals(new Node[]{n1, n3, n4}, groups.get(integerType()));
        assertArrayEquals(new Node[]{n2}, groups.get(stringType()));
        assertArrayEquals(new Node[]{n5}, groups.get(booleanType()));
    }

    @Test
    public void testGroupBy() {
        String[] values = {"aardvark", "apple", "bag", "cat", "cake", "caterpillar"};
        Map<Character, String[]> groups = Utils.groupBy(values, s -> s.charAt(0));
        assertEquals(3, groups.size());
        assertArrayEquals(new String[] {"aardvark", "apple"}, groups.get('a'));
        assertArrayEquals(new String[] {"bag"}, groups.get('b'));
        assertArrayEquals(new String[] {"cat", "cake", "caterpillar"}, groups.get('c'));
    }

    @Test
    public void testSelectSubNodeIndexFunctionNode() {
        assertSelectSubNodeIndex("(+ (+ 1 2) (+ 3 4))", 7, 3);
        assertSelectSubNodeIndex("(zero? 0)", 2, 0);
    }

    private void assertSelectSubNodeIndex(String input, int expectedNodeCount, int expectedIndex) {
        Node tree = readNode(input);
        assertEquals(expectedNodeCount, tree.size());
        int actual = Utils.selectSubNodeIndex(nextInt(expectedNodeCount - 1).returns(expectedIndex), tree);
        assertEquals(expectedIndex, actual);
    }

    @Test
    public void testSelectSubNodeIndexTerminalNode() {
        ConstantNode terminal = integerConstant(1);
        assertEquals(0, Utils.selectSubNodeIndex(DummyRandom.EMPTY, terminal));
    }

    @Test
    public void testSelectSubNodeIndex() {
        int expected = 2;
        assertEquals(expected, Utils.selectSubNodeIndex(nextInt(4).returns(expected), 5));
    }

    @Test
    public void testCreateIntegerConstants() {
        int minInclusive = 7;
        int maxInclusive = 12;

        ConstantNode[] result = Utils.intConsts(minInclusive, maxInclusive);

        assertEquals(6, result.length);
        for (int i = 0; i < result.length; i++) {
            assertSame(integerType(), result[i].returnType());
            assertEquals(i + minInclusive, result[i].eval(null));
        }
    }

    @Test
    public void testCreateIntegerTypeArray() {
        assertIntegerTypeArray(0);
        assertIntegerTypeArray(1);
        assertIntegerTypeArray(2);
        assertIntegerTypeArray(3);
        assertIntegerTypeArray(100);
    }

    private void assertIntegerTypeArray(int size) {
        NodeType[] t = Utils.intArrayType(size);
        assertEquals(size, t.length);
        for (NodeType element : t) {
            assertSame(integerType(), element);
        }
    }

    @Test
    public void testCopyOf() {
        String[] original = {"abc", "def", "ghi"};
        String[] copy = original.clone();
        assertNotSame(original, copy);
        assertTrue(Arrays.equals(original, copy));
    }

    @Test
    public void testCreateEnumConstants() {
        NodeType type = NodeType.type("testCreateEnumConstants");
        TestCreateEnumConstantsEnum[] input = TestCreateEnumConstantsEnum.values();

        ConstantNode[] result = Utils.enumConsts(TestCreateEnumConstantsEnum.class, type);

        assertEquals(input.length, result.length);
        for (int i = 0; i < input.length; i++) {
            assertSame(type, result[i].returnType());
            assertSame(input[i], result[i].eval(null));
        }
    }

    private enum TestCreateEnumConstantsEnum {
        A, B, C
    }
}
