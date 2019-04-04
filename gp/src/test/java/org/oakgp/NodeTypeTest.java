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


import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;


public class NodeTypeTest {
    @Test
    public void testString() {
        assertType("string", NodeType::stringType);
    }

    @Test
    public void testInteger() {
        assertType("integer", NodeType::integerType);
    }

    @Test
    public void testLong() {
        assertType("long", NodeType::longType);
    }

    @Test
    public void testDouble() {
        assertType("double", NodeType::doubleType);
    }

    @Test
    public void testBigInteger() {
        assertType("BigInteger", NodeType::bigIntegerType);
    }

    @Test
    public void testBigDecimal() {
        assertType("BigDecimal", NodeType::bigDecimalType);
    }

    @Test
    public void testBoolean() {
        assertType("boolean", NodeType::booleanType);
    }

    @Test
    public void testIntegerArray() {
        assertArrayType("integer", NodeType::integerArrayType);
    }

    @Test
    public void testBooleanArray() {
        assertArrayType("boolean", NodeType::booleanArrayType);
    }

    @Test
    public void testNullableInteger() {
        assertNullableType("integer", NodeType::integerType);
    }

    @Test
    public void testNullableBooleanArray() {
        assertNullableType("array [boolean]", NodeType::booleanArrayType);
    }

    @Test
    public void testIsNullable() {
        String nullableName = "nullable";
        NodeType integerType = NodeType.integerType();
        NodeType booleanType = NodeType.booleanType();

        assertNullable(NodeType.type(nullableName, integerType));
        assertNullable(NodeType.type(nullableName, booleanType));

        assertNotNullable(integerType);
        assertNotNullable(NodeType.type(nullableName, integerType, integerType));
    }

    private void assertNullable(NodeType t) {
        assertTrue(NodeType.isNullable(t));
    }

    private void assertNotNullable(NodeType t) {
        assertFalse(NodeType.isNullable(t));
    }

    @Test
    public void testIntegerToBooleanFunction() {
        NodeType t = NodeType.integerToBooleanFunctionType();
        assertSame(t, NodeType.integerToBooleanFunctionType());
        assertSame(t, NodeType.functionType(NodeType.booleanType(), NodeType.integerType()));
        assertEquals("function [boolean, integer]", t.toString());
    }

    @Test
    public void testNotEquals() {
        
        assertNotEquals(NodeType.booleanType(), NodeType.stringType());
        
        assertNotEquals(NodeType.booleanType(), NodeType.booleanArrayType());
        
        assertNotEquals(NodeType.booleanArrayType(), NodeType.integerArrayType());
        
        assertNotEquals(NodeType.functionType(NodeType.integerType(), NodeType.stringType()), NodeType.functionType(NodeType.stringType(), NodeType.integerType()));
    }

    @Test
    public void testUserDefinedType() {
        NodeType t = NodeType.type("qwerty", NodeType.integerType());
        assertEquals("qwerty [integer]", t.toString());
        assertEquals(t, NodeType.type("qwerty", NodeType.integerType()));
        assertSame(t, NodeType.type("qwerty", NodeType.integerType()));
        assertNotEquals(t, NodeType.type("Qwerty", NodeType.integerType()));
        assertNotEquals(t, NodeType.type("qwe-rty", NodeType.integerType()));
        assertNotEquals(t, NodeType.type("qwe rty", NodeType.integerType()));
        assertNotEquals(t, NodeType.type(" qwerty", NodeType.integerType()));
        assertNotEquals(t, NodeType.type("qwerty ", NodeType.integerType()));
        assertNotEquals(t, NodeType.type("qwerty"));
        assertNotEquals(t, NodeType.type("qwerty", NodeType.integerType(), NodeType.integerType()));
        assertNotEquals(t, NodeType.type("qwerty", NodeType.stringType()));
    }

    @Test
    public void testSameTypes() {
        NodeType[] t1 = {NodeType.booleanType(), NodeType.integerType(), NodeType.booleanType()};

        assertSameTypes(t1, new NodeType[]{NodeType.booleanType(), NodeType.integerType(), NodeType.booleanType()});

        
        assertNotSameTypes(t1, new NodeType[]{NodeType.integerType(), NodeType.booleanType(), NodeType.booleanType()});

        
        assertNotSameTypes(t1, new NodeType[]{NodeType.booleanType(), NodeType.integerType(), NodeType.stringType()});

        
        assertNotSameTypes(t1, new NodeType[]{NodeType.booleanType(), NodeType.integerType()});

        
        assertNotSameTypes(t1, new NodeType[]{NodeType.booleanType(), NodeType.integerType(), NodeType.booleanType(), NodeType.booleanType()});
    }

    private void assertType(String name, Supplier<NodeType> s) {
        NodeType t = s.get();
        assertEquals(t, s.get());
        assertSame(t, s.get());
        assertEquals(t, NodeType.type(name));
        assertSame(t, NodeType.type(name));
        assertEquals(name, t.toString());
    }

    private void assertArrayType(String name, Supplier<NodeType> s) {
        NodeType t = s.get();
        assertSame(t, s.get());
        assertSame(t, NodeType.arrayType(NodeType.type(name)));
        assertEquals("array [" + name + ']', t.toString());
    }

    private void assertNullableType(String name, Supplier<NodeType> s) {
        NodeType t = NodeType.type("nullable", s.get());
        assertSame(t, NodeType.type("nullable", s.get()));
        assertSame(t, NodeType.nullableType(s.get()));
        assertEquals("nullable [" + name + ']', t.toString());
    }

    private void assertSameTypes(NodeType[] a, NodeType[] b) {
        assertTrue(NodeType.equal(a, a));
        assertTrue(NodeType.equal(b, b));
        assertTrue(NodeType.equal(a, b));
        assertTrue(NodeType.equal(b, a));
    }

    private void assertNotSameTypes(NodeType[] a, NodeType[] b) {
        assertTrue(NodeType.equal(a, a));
        assertTrue(NodeType.equal(b, b));
        assertFalse(NodeType.equal(a, b));
        assertFalse(NodeType.equal(b, a));
    }
}
