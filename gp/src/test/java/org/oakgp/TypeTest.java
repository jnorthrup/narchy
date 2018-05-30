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


public class TypeTest {
    @Test
    public void testString() {
        assertType("string", Type::stringType);
    }

    @Test
    public void testInteger() {
        assertType("integer", Type::integerType);
    }

    @Test
    public void testLong() {
        assertType("long", Type::longType);
    }

    @Test
    public void testDouble() {
        assertType("double", Type::doubleType);
    }

    @Test
    public void testBigInteger() {
        assertType("BigInteger", Type::bigIntegerType);
    }

    @Test
    public void testBigDecimal() {
        assertType("BigDecimal", Type::bigDecimalType);
    }

    @Test
    public void testBoolean() {
        assertType("boolean", Type::booleanType);
    }

    @Test
    public void testIntegerArray() {
        assertArrayType("integer", Type::integerArrayType);
    }

    @Test
    public void testBooleanArray() {
        assertArrayType("boolean", Type::booleanArrayType);
    }

    @Test
    public void testNullableInteger() {
        assertNullableType("integer", Type::integerType);
    }

    @Test
    public void testNullableBooleanArray() {
        assertNullableType("array [boolean]", Type::booleanArrayType);
    }

    @Test
    public void testIsNullable() {
        String nullableName = "nullable";
        Type integerType = Type.integerType();
        Type booleanType = Type.booleanType();

        assertNullable(Type.type(nullableName, integerType));
        assertNullable(Type.type(nullableName, booleanType));

        assertNotNullable(integerType);
        assertNotNullable(Type.type(nullableName, integerType, integerType));
    }

    private void assertNullable(Type t) {
        assertTrue(Type.isNullable(t));
    }

    private void assertNotNullable(Type t) {
        assertFalse(Type.isNullable(t));
    }

    @Test
    public void testIntegerToBooleanFunction() {
        Type t = Type.integerToBooleanFunctionType();
        assertSame(t, Type.integerToBooleanFunctionType());
        assertSame(t, Type.functionType(Type.booleanType(), Type.integerType()));
        assertEquals("function [boolean, integer]", t.toString());
    }

    @Test
    public void testNotEquals() {
        
        assertNotEquals(Type.booleanType(), Type.stringType());
        
        assertNotEquals(Type.booleanType(), Type.booleanArrayType());
        
        assertNotEquals(Type.booleanArrayType(), Type.integerArrayType());
        
        assertNotEquals(Type.functionType(Type.integerType(), Type.stringType()), Type.functionType(Type.stringType(), Type.integerType()));
    }

    @Test
    public void testUserDefinedType() {
        Type t = Type.type("qwerty", Type.integerType());
        assertEquals("qwerty [integer]", t.toString());
        assertEquals(t, Type.type("qwerty", Type.integerType()));
        assertSame(t, Type.type("qwerty", Type.integerType()));
        assertNotEquals(t, Type.type("Qwerty", Type.integerType()));
        assertNotEquals(t, Type.type("qwe-rty", Type.integerType()));
        assertNotEquals(t, Type.type("qwe rty", Type.integerType()));
        assertNotEquals(t, Type.type(" qwerty", Type.integerType()));
        assertNotEquals(t, Type.type("qwerty ", Type.integerType()));
        assertNotEquals(t, Type.type("qwerty"));
        assertNotEquals(t, Type.type("qwerty", Type.integerType(), Type.integerType()));
        assertNotEquals(t, Type.type("qwerty", Type.stringType()));
    }

    @Test
    public void testSameTypes() {
        Type[] t1 = {Type.booleanType(), Type.integerType(), Type.booleanType()};

        assertSameTypes(t1, new Type[]{Type.booleanType(), Type.integerType(), Type.booleanType()});

        
        assertNotSameTypes(t1, new Type[]{Type.integerType(), Type.booleanType(), Type.booleanType()});

        
        assertNotSameTypes(t1, new Type[]{Type.booleanType(), Type.integerType(), Type.stringType()});

        
        assertNotSameTypes(t1, new Type[]{Type.booleanType(), Type.integerType()});

        
        assertNotSameTypes(t1, new Type[]{Type.booleanType(), Type.integerType(), Type.booleanType(), Type.booleanType()});
    }

    private void assertType(String name, Supplier<Type> s) {
        Type t = s.get();
        assertEquals(t, s.get());
        assertSame(t, s.get());
        assertEquals(t, Type.type(name));
        assertSame(t, Type.type(name));
        assertEquals(name, t.toString());
    }

    private void assertArrayType(String name, Supplier<Type> s) {
        Type t = s.get();
        assertSame(t, s.get());
        assertSame(t, Type.arrayType(Type.type(name)));
        assertEquals("array [" + name + "]", t.toString());
    }

    private void assertNullableType(String name, Supplier<Type> s) {
        Type t = Type.type("nullable", s.get());
        assertSame(t, Type.type("nullable", s.get()));
        assertSame(t, Type.nullableType(s.get()));
        assertEquals("nullable [" + name + "]", t.toString());
    }

    private void assertSameTypes(Type[] a, Type[] b) {
        assertTrue(Type.equal(a, a));
        assertTrue(Type.equal(b, b));
        assertTrue(Type.equal(a, b));
        assertTrue(Type.equal(b, a));
    }

    private void assertNotSameTypes(Type[] a, Type[] b) {
        assertTrue(Type.equal(a, a));
        assertTrue(Type.equal(b, b));
        assertFalse(Type.equal(a, b));
        assertFalse(Type.equal(b, a));
    }
}
