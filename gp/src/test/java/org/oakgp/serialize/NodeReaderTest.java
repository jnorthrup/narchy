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
package org.oakgp.serialize;

import org.junit.jupiter.api.Test;
import org.oakgp.Arguments;
import org.oakgp.function.Fn;
import org.oakgp.function.classify.IsPositive;
import org.oakgp.node.ConstantNode;
import org.oakgp.node.FnNode;
import org.oakgp.node.Node;
import org.oakgp.node.VariableNode;
import org.oakgp.primitive.VariableSet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.NodeType.*;
import static org.oakgp.TestUtils.*;
import static org.oakgp.util.Void.VOID;

public class NodeReaderTest {
    @Test
    public void testIsEndOfStream() throws IOException {
        try (NodeReader nr = new NodeReader("1 2 3", new Fn[0], new ConstantNode[0], VariableSet.Empty)) {
            assertFalse(nr.isEndOfStream());
            nr.readNode();

            assertFalse(nr.isEndOfStream());
            nr.readNode();

            assertFalse(nr.isEndOfStream());
            nr.readNode();

            assertTrue(nr.isEndOfStream());
            try {
                nr.readNode();
                fail("");
            } catch (IllegalStateException e) {
                
            }
        }
    }

    @Test
    public void testZero() {
        assertParseLiteral(0);
    }

    @Test
    public void testNegativeConstantNode() {
        assertParseLiteral(-9);
    }

    @Test
    public void testSingleCharacterConstantNode() {
        assertParseLiteral(4);
    }

    @Test
    public void testMulipleCharacterConstantNode() {
        assertParseLiteral(42);
    }

    @Test
    public void testInteger() {
        assertParseLiteral("42", 42);
        assertParseLiteral("2147483647", Integer.MAX_VALUE);
        assertParseLiteral("-2147483648", Integer.MIN_VALUE);
    }

    @Test
    public void testLong() {
        assertParseLiteral("42L", 42L);
        assertParseLiteral("9223372036854775807L", Long.MAX_VALUE);
        assertParseLiteral("-9223372036854775808L", Long.MIN_VALUE);
    }

    @Test
    public void testDouble() {
        assertParseLiteral("42.0", 42d);
        assertParseLiteral("42.5", 42.5d);
        assertParseLiteral("1.7976931348623157E308", Double.MAX_VALUE);
        assertParseLiteral("4.9E-324", Double.MIN_VALUE);
    }

    @Test
    public void testBigInteger() {
        assertParseLiteral("42I", new BigInteger("42"));
        assertParseLiteral("9223372036854775807I", BigInteger.valueOf(Long.MAX_VALUE));
        assertParseLiteral("-9223372036854775808I", BigInteger.valueOf(Long.MIN_VALUE));
    }

    @Test
    public void testBigDecimal() {
        assertParseLiteral("42D", new BigDecimal("42"));
        assertParseLiteral("42.5D", new BigDecimal("42.5"));
        assertParseLiteral("1.7976931348623157E308D", BigDecimal.valueOf(Double.MAX_VALUE));
        assertParseLiteral("4.9E-324D", BigDecimal.valueOf(Double.MIN_VALUE));
    }

    /**
     * Tests that, when available, parser uses constants defined in BigDecimal.
     */
    @Test
    public void testBigDecimalReuse() {
        assertSame(BigDecimal.ZERO, readConstant("0D").eval(null));
        assertSame(BigDecimal.ONE, readConstant("1D").eval(null));
        assertSame(BigDecimal.TEN, readConstant("10D").eval(null));
    }

    /**
     * Tests that, when available, parser uses constants defined in BigInteger.
     */
    @Test
    public void testBigIntegerReuse() {
        assertSame(BigInteger.ZERO, readConstant("0I").eval(null));
        assertSame(BigInteger.ONE, readConstant("1I").eval(null));
        assertSame(BigInteger.TEN, readConstant("10I").eval(null));
    }

    @Test
    public void testTrue() {
        assertParseLiteral(Boolean.TRUE);
    }

    @Test
    public void testFalse() {
        assertParseLiteral(Boolean.FALSE);
    }

    @Test
    public void testVoid() {
        assertParseLiteral("void", VOID);
    }

    @Test
    public void testSingleWordString() {
        assertParseLiteral("\"hello\"", "hello");
    }

    @Test
    public void testMultiWordString() {
        assertParseLiteral("\"Hello, world!\"", "Hello, world!");
    }

    @Test
    public void testFunctionSymbol() {
        
        assertParseFunction("pos?", IsPositive.class);
    }

    @Test
    public void testEmptyArray() {
        assertParseLiteral("[]", new Arguments(new Node[]{}));
    }

    @Test
    public void testTypeArray() {
        Arguments expected = new Arguments(new Node[]{new ConstantNode(9, integerType()), new ConstantNode(2, integerType()), createVariable(0), new ConstantNode(7, integerType())});
        assertParseLiteral("[9 2 v0 7]", expected);
    }

    @Test
    public void testMixedTypeArray() {
        assertReadException("[true 9 false v0]", "Mixed type array elements: boolean and integer");
    }

    @Test
    public void testSingleDigitIdVariableNode() {
        assertParseVariable(1);
    }

    @Test
    public void testMultipleDigitIdVariableNode() {
        assertParseVariable(78);
    }

    @Test
    public void testFunctionNodeSpecifiedBySymbol() {
        assertParseFunction("(+ 7 21)");
    }

    @Test
    public void testFunctionNodeWithFunctionNodeArguments() {
        assertParseFunction("(+ (* 43 v1) (- v0 587))");
    }

    @Test
    public void testEmptyString() {
        String input = "";
        List<Node> outputs = readNodes(input);
        assertTrue(outputs.isEmpty());
    }

    @Test
    public void testWhitespace() {
        String input = " \r\n\t\t  ";
        List<Node> outputs = readNodes(input);
        assertTrue(outputs.isEmpty());
    }

    @Test
    public void testPadded() {
        String input = " \r\n42\t\t  ";
        assertParseLiteral(input, 42);
    }

    @Test
    public void testConstantNode() throws IOException {
        String input = "TEST";
        ConstantNode expected = new ConstantNode(input, type("testConstantNode"));
        try (NodeReader r = new NodeReader(input, new Fn[0],
                new ConstantNode[]{expected}, VariableSet.Empty)) {
            Node actual = r.readNode();
            assertSame(expected, actual);
        }
    }

    @Test
    public void testUnknown() throws IOException {
        String input = "TEST";
        try (NodeReader r = new NodeReader(input, new Fn[0], new ConstantNode[0], VariableSet.Empty)) {
            r.readNode();
            fail("");
        } catch (IllegalArgumentException e) {
            
            assertEquals("Could not find version of function: TEST in: []", e.getMessage());
        }
    }

    @Test
    public void testMulipleNodes() {
        String[] inputs = {"6", "(+ v0 v1)", "42", "v0", "(+ 1 2)", "v98"};
        String combinedInput = " " + inputs[0] + inputs[1] + inputs[2] + " " + inputs[3] + "\n\r\t\t\t" + inputs[4] + "       \n   " + inputs[5] + "\r\n";
        List<Node> outputs = readNodes(combinedInput);
        assertEquals(inputs.length, outputs.size());
        for (int i = 0; i < inputs.length; i++) {
            assertEquals(inputs[i], outputs.get(i).toString());
        }
    }

    @Test
    public void testValidDisplayName() {
        assertValidDisplayName("x");
        assertValidDisplayName("X");
        assertValidDisplayName("hello");
        assertValidDisplayName("?x_Y-z!");

        
        assertValidDisplayName("-->");

        
        assertValidDisplayName("i5");
    }

    @Test
    public void testInvalidDisplayName() {
        
        assertInvalidDisplayName(null);
        assertInvalidDisplayName("");

        
        assertInvalidDisplayName(" ");
        assertInvalidDisplayName("hel lo");
        assertInvalidDisplayName("x ");
        assertInvalidDisplayName(" x");
        assertInvalidDisplayName("x\n");
        assertInvalidDisplayName("\tx");

        
        assertInvalidDisplayName("-9");
        assertInvalidDisplayName("9i");
    }

    private void assertValidDisplayName(String displayName) {
        assertIsValidDisplayName(displayName, true);
    }

    private void assertInvalidDisplayName(String displayName) {
        assertIsValidDisplayName(displayName, false);
    }

    private void assertIsValidDisplayName(String displayName, boolean isValid) {
        assertEquals(isValid, NodeReader.isValidDisplayName(displayName));
    }

    private void assertParseLiteral(Object expected) {
        assertParseLiteral(expected.toString(), expected);
    }

    private void assertParseLiteral(String input, Object expected) {
        Node output = readConstant(input);
        assertSame(expected.getClass(), output.eval(null).getClass());
        assertEquals(expected.toString(), output.toString());
        assertEquals(expected, output.eval(null));
    }

    private void assertParseFunction(String input, Class<? extends Fn> expected) {
        Node output = readConstant(input);
        assertSame(integerToBooleanFunctionType(), output.returnType());
        assertEquals(expected, ((ConstantNode) output).eval(null).getClass());
    }

    private void assertParseVariable(int id) {
        String input = "v" + id;
        Node output = readNode(input);
        assertSame(VariableNode.class, output.getClass());
        assertEquals(id, ((VariableNode) output).getId());
        assertEquals(input, output.toString());
    }

    private void assertParseFunction(String input) {
        Node output = readNode(input);
        assertSame(FnNode.class, output.getClass());
        assertEquals(input, output.toString());
    }

    private void assertReadException(String input, String expectedMessage) {
        try {
            readNode(input);
            fail("");
        } catch (RuntimeException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    private ConstantNode readConstant(String input) {
        Node output = readNode(input);
        assertSame(ConstantNode.class, output.getClass());
        return (ConstantNode) output;
    }
}
