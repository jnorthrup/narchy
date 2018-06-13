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
package org.oakgp.function.math;

import org.junit.jupiter.api.Test;
import org.oakgp.function.choice.If;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.*;
import static org.oakgp.NodeType.integerType;

public class NumberUtilsTest {
    private static final IntFunc NUMBER_UTILS = IntFunc.the;

    @Test
    public void testNegate() {
        assertNegate("1", "-1");
        assertNegate("-1", "1");
        assertNegate("(+ v0 v1)", "(- 0 (+ v0 v1))");
    }

    private void assertNegate(String before, String after) {
        Node input = readNode(before);
        Node output = readNode(after);
        assertEquals(output, NUMBER_UTILS.negate(input));
    }

    @Test
    public void testMultiplyByTwo() {
        assertMultiplyByTwo("v0", "(* 2 v0)");
        assertMultiplyByTwo("(+ v0 v1)", "(* 2 (+ v0 v1))");
    }

    private void assertMultiplyByTwo(String before, String after) {
        Node input = readNode(before);
        Node output = readNode(after);
        assertEquals(output, NUMBER_UTILS.multiplyByTwo(input));
    }

    @Test
    public void testIsNegative() {
        assertTrue(isNegative(Integer.MIN_VALUE));
        assertTrue(isNegative(-42));
        assertTrue(isNegative(-2));
        assertTrue(isNegative(-1));
        assertFalse(isNegative(0));
        assertFalse(isNegative(1));
        assertFalse(isNegative(2));
        assertFalse(isNegative(42));
        assertFalse(isNegative(Integer.MAX_VALUE));
    }

    private boolean isNegative(int i) {
        return NUMBER_UTILS.isNegative(integerConstant(i));
    }

    @Test
    public void testIsAdd() {
        assertIsAdd("(+ 1 2)", true);
        assertIsAdd("(- 1 2)", false);
        assertIsAdd("(* 1 2)", false);
        assertIsAdd("1", false);
        assertIsAdd("true", false);
    }

    private void assertIsAdd(String input, boolean expectedResult) {
        Node n = readNode(input);
        if (n instanceof FnNode) {
            assertEquals(expectedResult, NUMBER_UTILS.isAdd(((FnNode) n).func()));
        } else {
            assertFalse(expectedResult);
        }
    }

    @Test
    public void testIsSubtract() {
        assertIsSubtract("(- 1 2)", true);
        assertIsSubtract("(+ 1 2)", false);
        assertIsSubtract("(* 1 2)", false);
        assertIsSubtract("1", false);
        assertIsSubtract("true", false);
    }

    private void assertIsSubtract(String input, boolean expectedResult) {
        Node n = readNode(input);
        assertEquals(expectedResult, NUMBER_UTILS.isSubtract(n));
        if (n instanceof FnNode) {
            assertEquals(expectedResult, NUMBER_UTILS.isSubtract((FnNode) n));
            assertEquals(expectedResult, NUMBER_UTILS.isSubtract(((FnNode) n).func()));
        } else {
            assertFalse(expectedResult);
        }
    }

    @Test
    public void testIsMultiply() {
        assertIsMultiply("(* 1 2)", true);
        assertIsMultiply("(+ 1 2)", false);
        assertIsMultiply("(- 1 2)", false);
        assertIsMultiply("1", false);
        assertIsMultiply("true", false);
    }

    private void assertIsMultiply(String input, boolean expectedResult) {
        Node n = readNode(input);
        if (n instanceof FnNode) {
            assertEquals(expectedResult, NUMBER_UTILS.isMultiply((FnNode) n));
            assertEquals(expectedResult, NUMBER_UTILS.isMultiply(((FnNode) n).func()));
        } else {
            assertFalse(expectedResult);
        }
    }

    @Test
    public void testIsAddOrSubtract() {
        assertTrue(NUMBER_UTILS.isAddOrSubtract(NUMBER_UTILS.add));
        assertTrue(NUMBER_UTILS.isAddOrSubtract(NUMBER_UTILS.subtract));
        assertFalse(NUMBER_UTILS.isAddOrSubtract(NUMBER_UTILS.multiply));
        assertFalse(NUMBER_UTILS.isAddOrSubtract(new If(integerType())));
    }

    @Test
    public void testIsArithmeticExpression() {
        assertTrue(NUMBER_UTILS.isArithmeticExpression(readFunctionNode("(+ v0 v1)")));
        assertTrue(NUMBER_UTILS.isArithmeticExpression(readFunctionNode("(- v0 v1)")));
        assertTrue(NUMBER_UTILS.isArithmeticExpression(readFunctionNode("(* v0 v1)")));
        assertTrue(NUMBER_UTILS.isArithmeticExpression(readFunctionNode("(/ v0 v1)")));
        assertFalse(NUMBER_UTILS.isArithmeticExpression(readFunctionNode("(= v0 v1)")));
    }
}
