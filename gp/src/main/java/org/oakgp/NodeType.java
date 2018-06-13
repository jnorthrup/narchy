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

import jcog.Util;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a data type.
 * <p>
 * e.g. integer, boolean, string, array, function.
 */
public final class NodeType implements Comparable<NodeType> {
    private static final String NULLABLE = "nullable";
    private static final ConcurrentHashMap<NodeType, NodeType> TYPE_CACHE = new ConcurrentHashMap<>();
    private static final NodeType[] EMPTY_ARRAY = new NodeType[0];

    private final String name;
    private final NodeType[] args;
    private final int hashCode;

    private NodeType(String name, NodeType... args) {
        this.name = name;
        this.args = args;
        this.hashCode =
                Util.hashCombine(name, Arrays.hashCode(args));
                //(name.hashCode() * 31) * Arrays.hashCode(args);
    }

    /**
     * Returns the type associated with instances of {@code java.lang.String}.
     */
    public static NodeType stringType() {
        return type("string");
    }

    /**
     * Returns the type associated with instances of {@code java.lang.Boolean}.
     */
    public static NodeType booleanType() {
        return type("boolean");
    }

    /**
     * Returns the type associated with instances of {@code java.lang.Integer}.
     */
    public static NodeType integerType() {
        return type("integer");
    }

    /**
     * Returns the type associated with instances of {@code java.lang.Long}.
     */
    public static NodeType longType() {
        return type("long");
    }

    /**
     * Returns the type associated with instances of {@code java.lang.Double}.
     */
    public static NodeType doubleType() {
        return type("double");
    }

    /**
     * Returns the type associated with instances of {@code java.math.BigInteger}.
     */
    public static NodeType bigIntegerType() {
        return type("BigInteger");
    }

    /**
     * Returns the type associated with instances of {@code java.math.BigDecimal}.
     */
    public static NodeType bigDecimalType() {
        return type("BigDecimal");
    }

    /**
     * Returns the type associated with {@code org.oakgp.function.Function} instances that accept a {@link #integerType()} and return {@link #booleanType()}.
     */
    public static NodeType integerToBooleanFunctionType() {
        return functionType(booleanType(), integerType());
    }

    /**
     * Returns a type that represents values that can be either the given {@code Type} or {@code null}.
     * <p>
     * e.g. The result of calling {@code nullableType(stringType())} is a type that represents values that can be <i>either</i> a {@code java.lang.String}
     * <i>or</i> {@code null}.
     *
     * @see #isNullable(NodeType)
     */
    public static NodeType nullableType(NodeType t) {
        return type(NULLABLE, t);
    }

    /**
     * Returns the type associated with instances of {@code org.oakgp.function.Function} with the specified signature.
     *
     * @param signature the function signature with the first element representing the return type and the subsequent elements representing the argument types
     */
    public static NodeType functionType(NodeType... signature) {
        if (signature.length < 2) {
            throw new IllegalArgumentException();
        }
        return type("function", signature);
    }

    /**
     * Returns the type associated with a {@code Arguments} containing elements of type {@link #integerType()}.
     */
    public static NodeType integerArrayType() {
        return arrayType(integerType());
    }

    /**
     * Returns the type associated with a {@code Arguments} containing elements of type {@link #booleanType()}.
     */
    public static NodeType booleanArrayType() {
        return arrayType(booleanType());
    }

    /**
     * Returns the type associated with a {@code Arguments} containing elements of the specified type.
     */
    public static NodeType arrayType(NodeType t) {
        return type("array", t);
    }

    /**
     * Returns a {@code Type} with the given name.
     * <p>
     * If there already exists a {@code Type} with the given {@code name} and no arguments then it will be returned else a new {@code Type} with the given
     * {@code name} and no arguments will be created and returned.
     */
    public static NodeType type(String name) {
        return type(name, EMPTY_ARRAY);
    }

    /**
     * Returns a {@code Type} with the given name and arguments.
     * <p>
     * If there already exists a {@code Type} with the given {@code name} and {@code args} then it will be returned else a new {@code Type} with the given
     * {@code name} and {@code args} will be created and returned.
     */
    public static NodeType type(String name, NodeType... args) {
        return type(new NodeType(name, args));
    }

    private static NodeType type(NodeType t) {
        return TYPE_CACHE.computeIfAbsent(t, k -> k);
    }

    /**
     * Returns {@code true} if the given {@code Type} represents values that may be {@code null}.
     *
     * @see #nullableType(NodeType)
     */
    public static boolean isNullable(NodeType t) {
        return t.name.equals(NULLABLE) && t.args.length == 1;
    }

    /**
     * Returns {@code true} if the two arrays contain the same number of elements, and all corresponding pairs of elements in the two arrays are the same.
     * <p>
     * Assumes no {@code null} values. Compares elements using {@code ==} rather than {@code equals(Object)}.
     *
     * @param a one array to be tested for equality
     * @param b the other array to be tested for equality
     * @return {@code true} if the two arrays contain the same elements in the same order, else {@code false}
     */
    public static boolean equal(NodeType[] a, NodeType[] b) {
        if (a == b)
            return true;

        int length = a.length;
        if (b.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int compareTo(NodeType t) {
        if (this == t)
            return 0;

        int i = name.compareTo(t.name);
        if (i == 0) {

            i = Integer.compare(args.length, t.args.length);

            if (i == 0) {
                for (int x = 0; x < args.length; x++) {
                    i = args[x].compareTo(t.args[x]);
                    if (i != 0)
                        return i;
                }

                return 0;
            }
            return i;

        }
        return i;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof NodeType) {
            NodeType t = (NodeType) o;
            return t.hashCode == hashCode && this.name.equals(t.name) && equal(this.args, t.args);
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        if (args.length == 0) {
            return name;
        } else {
            return name + ' ' + Arrays.toString(args);
        }
    }
}
