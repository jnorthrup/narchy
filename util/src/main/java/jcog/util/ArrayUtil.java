/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.util;


import com.google.common.collect.ForwardingListIterator;
import jcog.data.array.IntComparator;
import jcog.data.bit.MetalBitSet;
import jcog.random.Rand;
import jcog.sort.QuickSort;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * <p>Operations on arrays, primitive arrays (like {@code int[]}) and
 * primitive wrapper arrays (like {@code Integer[]}).
 *
 * <p>This class tries to handle {@code null} input gracefully.
 * An exception will not be thrown for a {@code null}
 * array input. However, an Object array that contains a {@code null}
 * element may throw an exception. Each method documents its behaviour.
 *
 * <p>#ThreadSafe#
 *
 * @since 2.0
 */
public enum ArrayUtil {
    ;

    /**
     * An empty immutable {@code Object} array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    /**
     * An empty immutable {@code Class} array.
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    /**
     * An empty immutable {@code String} array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /**
     * An empty immutable {@code long} array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    /**
     * An empty immutable {@code Long} array.
     */
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
    /**
     * An empty immutable {@code int} array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty immutable {@code Integer} array.
     */
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
    /**
     * An empty immutable {@code short} array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    /**
     * An empty immutable {@code Short} array.
     */
    public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];
    /**
     * An empty immutable {@code byte} array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * An empty immutable {@code Byte} array.
     */
    public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];
    /**
     * An empty immutable {@code double} array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    /**
     * An empty immutable {@code Double} array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];
    /**
     * An empty immutable {@code float} array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    /**
     * An empty immutable {@code Float} array.
     */
    public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];
    /**
     * An empty immutable {@code boolean} array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    /**
     * An empty immutable {@code Boolean} array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];
    /**
     * An empty immutable {@code char} array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    /**
     * An empty immutable {@code Character} array.
     */
    public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];

    /**
     * The index value when an element is not found in a list or array: {@code -1}.
     * This value is returned by methods in this class and can also be used in comparisons with values returned by
     * various method from {@link java.util.List}.
     */
    public static final int INDEX_NOT_FOUND = -1;
    public static final double[][] EMPTY_DOUBLE_DOUBLE = new double[0][0];
    public static final Consumer[] EMPTY_CONSUMER_ARRAY = new Consumer[0];
    public static final URL[] EMPTY_URL_ARRAY = new URL[0];
    public static final LongObjectPair[] EMPTY_LONGOBJECT_PAIR_ARRAY = new LongObjectPair[0];
    public static final Comparator NullCompactingComparator = (o1, o2) -> {
        if (o1 == o2) return 0;
        if (o1 == null) return 1;
        if (o2 == null) return -1;
        return Integer.compare(
                System.identityHashCode(o1),
                System.identityHashCode(o2)
        );
    };
    /**
     * The number of distinct byte values.
     */
//    private static final int NUM_BYTE_VALUES = 1 << 8;
    private static final byte[] BYTE_ZERO = {0};
    private static final byte[] BYTE_ONE = {1};
    private static final byte[] BYTE_TWO = {2};
    private static final byte[] BYTE_THREE = {3};
    private static final byte[] BYTE_ZERO_ZERO = {0, 0};
    private static final byte[] BYTE_ZERO_ONE = {0, 1};
    private static final byte[] BYTE_ONE_ZERO = {1, 0};
    private static final byte[] BYTE_ONE_ONE = {1, 1};
    private static final byte[] BYTE_ONE_TWO = {1, 2};
    private static final byte[] BYTE_ONE_NEGONE = {1, -1};
    private static final byte[] BYTE_ONE_NEGTWO = {1, -2};
    private static final byte[] BYTE_TWO_ONE = {2, 1};
    private static final byte[] BYTE_TWO_TWO = {2, 2};
    private static final byte[] BYTE_TWO_NEGONE = {2, -1};
    private static final byte[] BYTE_TWO_NEGTWO = {2, -2};
    private static final byte[] BYTE_NEGONE_ONE = {-1, 1};
    private static final byte[] BYTE_NEGONE_TWO = {-1, 2};
    private static final byte[] BYTE_NEGONE_NEGONE = {-1, -1};
    private static final byte[] BYTE_NEGONE_NEGTWO = {-1, -2};

    public static void sortNullsToEnd(Object[] x) {
        Arrays.sort(x, NullCompactingComparator);
    }

    /**
     * <p>Shallow clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>The objects in the array are not cloned, thus there is no special
     * handling for multi-dimensional arrays.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param <T>   the component type of the array
     * @param array the array to shallow clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    private static <T> T[] clone(T[] array) {
        return array == null ? null : array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static long[] clone(long[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static int[] clone(int[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static short[] clone(short[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static char[] clone(char[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static byte[] clone(byte[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static double[] clone(double[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static float[] clone(float[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Clones an array returning a typecast result and handling
     * {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static boolean[] clone(boolean[] array) {
        if (array == null) return null;
        return array.clone();
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * @param array the array to check for {@code null} or empty
     * @param type  the class representation of the desired array
     * @param <T>   the class type
     * @return the same array, {@code public static} empty array if {@code null}
     * @throws IllegalArgumentException if the type argument is null
     * @since 3.5
     */
    public static <T> T[] nullToEmpty(T[] array, Class<T[]> type) {
        if (type == null) throw new IllegalArgumentException("The type must not be null");

        if (array == null) return type.cast(Array.newInstance(type.getComponentType(), 0));
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Object[] nullToEmpty(Object[] array) {
        if (array.length == 0) return EMPTY_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 3.2
     */
    public static Class<?>[] nullToEmpty(Class<?>[] array) {
        if (array.length == 0) return EMPTY_CLASS_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static String[] nullToEmpty(String[] array) {
        if (array.length == 0) return EMPTY_STRING_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static long[] nullToEmpty(long[] array) {
        if (array.length == 0) return EMPTY_LONG_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static int[] nullToEmpty(int[] array) {
        if (array.length == 0) return EMPTY_INT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static short[] nullToEmpty(short[] array) {
        if (array.length == 0) return EMPTY_SHORT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static char[] nullToEmpty(char[] array) {
        if (array.length == 0) return EMPTY_CHAR_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static byte[] nullToEmpty(byte[] array) {
        if (array.length == 0) return EMPTY_BYTE_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static double[] nullToEmpty(double[] array) {
        if (array.length == 0) return EMPTY_DOUBLE_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static float[] nullToEmpty(float[] array) {
        if (array.length == 0) return EMPTY_FLOAT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static boolean[] nullToEmpty(boolean[] array) {
        if (array.length == 0) return EMPTY_BOOLEAN_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Long[] nullToEmpty(Long[] array) {
        if (array.length == 0) return EMPTY_LONG_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Integer[] nullToEmpty(Integer[] array) {
        if (array.length == 0) return EMPTY_INTEGER_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Short[] nullToEmpty(Short[] array) {
        if (array.length == 0) return EMPTY_SHORT_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Character[] nullToEmpty(Character[] array) {
        if (array.length == 0) return EMPTY_CHARACTER_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Byte[] nullToEmpty(Byte[] array) {
        if (array.length == 0) return EMPTY_BYTE_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Double[] nullToEmpty(Double[] array) {
        if (array.length == 0) return EMPTY_DOUBLE_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Float[] nullToEmpty(Float[] array) {
        if (array.length == 0) return EMPTY_FLOAT_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Defensive programming technique to change a {@code null}
     * reference to an empty one.
     *
     * <p>This method returns an empty array for a {@code null} input array.
     *
     * <p>As a memory optimizing technique an empty array passed in will be overridden with
     * the empty {@code public static} references in this class.
     *
     * @param array the array to check for {@code null} or empty
     * @return the same array, {@code public static} empty array if {@code null} or empty input
     * @since 2.5
     */
    public static Boolean[] nullToEmpty(Boolean[] array) {
        if (array.length == 0) return EMPTY_BOOLEAN_OBJECT_ARRAY;
        return array;
    }

    /**
     * <p>Produces a new array containing the elements between
     * the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * <p>The component type of the subarray is always the same as
     * that of the input array. Thus, if the input is an array of type
     * {@code Date}, the following usage is envisaged:
     *
     * <pre>
     * Date[] someDates = (Date[])ArrayUtils.subarray(allDates, 2, 5);
     * </pre>
     *
     * @param <T>                 the component type of the array
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(Object[], int, int)
     * @since 2.1
     */
    public static <T> T[] subarray(T[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        var type = array.getClass().getComponentType();
        if (newSize <= 0) {
            @SuppressWarnings("unchecked") var emptyArray = (T[]) Array.newInstance(type, 0);
            return emptyArray;
        }
        @SuppressWarnings("unchecked") var subarray = (T[]) Array.newInstance(type, newSize);
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    public static <T> T[] subarray(T[] array, int startIndexInclusive, int endIndexExclusive, IntFunction<T[]> builder) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        var type = array.getClass().getComponentType();
        if (newSize <= 0) return builder.apply(0);
        @SuppressWarnings("unchecked") var subarray = builder.apply(newSize);
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code long} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(long[], int, int)
     * @since 2.1
     */
    public static long[] subarray(long[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_LONG_ARRAY;

        var subarray = new long[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code int} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(int[], int, int)
     * @since 2.1
     */
    public static int[] subarray(int[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_INT_ARRAY;

        var subarray = new int[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code short} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(short[], int, int)
     * @since 2.1
     */
    public static short[] subarray(short[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_SHORT_ARRAY;

        var subarray = new short[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code char} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(char[], int, int)
     * @since 2.1
     */
    public static char[] subarray(char[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_CHAR_ARRAY;

        var subarray = new char[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code byte} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(byte[], int, int)
     * @since 2.1
     */
    public static byte[] subarray(byte[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_BYTE_ARRAY;

        var subarray = new byte[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code double} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(double[], int, int)
     * @since 2.1
     */
    public static double[] subarray(double[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_DOUBLE_ARRAY;

        var subarray = new double[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code float} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(float[], int, int)
     * @since 2.1
     */
    public static float[] subarray(float[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_FLOAT_ARRAY;

        var subarray = new float[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Produces a new {@code boolean} array containing the elements
     * between the start and end indices.
     *
     * <p>The start index is inclusive, the end index exclusive.
     * Null array input produces null output.
     *
     * @param array               the array
     * @param startIndexInclusive the starting index. Undervalue (&lt;0)
     *                            is promoted to 0, overvalue (&gt;array.length) results
     *                            in an empty array.
     * @param endIndexExclusive   elements up to endIndex-1 are present in the
     *                            returned subarray. Undervalue (&lt; startIndex) produces
     *                            empty array, overvalue (&gt;array.length) is demoted to
     *                            array length.
     * @return a new array containing the elements between
     * the start and end indices.
     * @see Arrays#copyOfRange(boolean[], int, int)
     * @since 2.1
     */
    public static boolean[] subarray(boolean[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return null;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive > array.length) endIndexExclusive = array.length;
        var newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) return EMPTY_BOOLEAN_ARRAY;

        var subarray = new boolean[newSize];
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    /**
     * <p>Returns the length of the specified array.
     * This method can deal with {@code Object} arrays and with primitive arrays.
     *
     * <p>If the input array is {@code null}, {@code 0} is returned.
     *
     * <pre>
     * ArrayUtils.getLength(null)            = 0
     * ArrayUtils.getLength([])              = 0
     * ArrayUtils.getLength([null])          = 1
     * ArrayUtils.getLength([true, false])   = 2
     * ArrayUtils.getLength([1, 2, 3])       = 3
     * ArrayUtils.getLength(["a", "b", "c"]) = 3
     * </pre>
     *
     * @param array the array to retrieve the length from, may be null
     * @return The length of the array, or {@code 0} if the array is {@code null}
     * @throws IllegalArgumentException if the object argument is not an array.
     * @since 2.1
     */
    public static int getLength(Object array) {
        return Array.getLength(array);
    }

    /**
     * <p>Checks whether two arrays are the same type taking into account
     * multi-dimensional arrays.
     *
     * @param array1 the first array, must not be {@code null}
     * @param array2 the second array, must not be {@code null}
     * @return {@code true} if type of arrays matches
     * @throws IllegalArgumentException if either array is {@code null}
     */
    public static boolean isSameType(Object array1, Object array2) {
//        if (array1 == null || array2 == null) {
//            throw new IllegalArgumentException("The Array must not be null");
//        }
        return array1.getClass().getName().equals(array2.getClass().getName());
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>There is no special handling for multi-dimensional arrays.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static <X> X[] reverse(X[] array) {
        reverse(array, 0, array.length);
        return array;
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(long[] array) {
        reverse(array, 0, array.length);
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param a the array to reverse, may be {@code null}
     */
    public static void reverse(int[] a) {
        var l = a.length;
        switch (l) {
            case 0:
            case 1:
                break;
            case 2: {
                var i = a[0];
                a[0] = a[1];
                a[1] = i;
                break;
            }
            default:
                reverse(a, 0, l);
                break;
        }
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(short[] array) {
        if (array == null) return;
        reverse(array, 0, array.length);
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(char[] array) {
        if (array == null) return;
        reverse(array, 0, array.length);
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(byte[] array) {
        if (array == null || array.length < 2)
            return;
        reverse(array, 0, array.length);
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(double[] array) {
        if (array == null) return;
        reverse(array, 0, array.length);
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(float[] array) {
        if (array == null) return;
        reverse(array, 0, array.length);
    }

    /**
     * <p>Reverses the order of the given array.
     *
     * <p>This method does nothing for a {@code null} input array.
     *
     * @param array the array to reverse, may be {@code null}
     */
    public static void reverse(boolean[] array) {
        if (array == null) return;
        reverse(array, 0, array.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(boolean[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(byte[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(char[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(double[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(float[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(int[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null || array.length <= 1 || (endIndexExclusive - startIndexInclusive <= 1)) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j--] = array[i];
            array[i++] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(long[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Under value (&lt;0) is promoted to 0, over value (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Under value (&lt; start index) results in no
     *                            change. Over value (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(Object[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     *
     * <p>
     * This method does nothing for a {@code null} input array.
     *
     * @param array               the array to reverse, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are reversed in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @since 3.2
     */
    public static void reverse(short[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) return;
        var i = Math.max(startIndexInclusive, 0);
        var j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            var tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * Swaps a series of elements in the given boolean array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap([true, false, true, false], 0, 2, 1) -&gt; [true, false, true, false]</li>
     * <li>ArrayUtils.swap([true, false, true, false], 0, 0, 1) -&gt; [true, false, true, false]</li>
     * <li>ArrayUtils.swap([true, false, true, false], 0, 2, 2) -&gt; [true, false, true, false]</li>
     * <li>ArrayUtils.swap([true, false, true, false], -3, 2, 2) -&gt; [true, false, true, false]</li>
     * <li>ArrayUtils.swap([true, false, true, false], 0, 3, 3) -&gt; [false, false, true, true]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(boolean[] array, int offset1, int offset2, int len) {
        if (array == null || array.length == 0 || offset1 >= array.length || offset2 >= array.length) return;
        if (offset1 < 0) offset1 = 0;
        if (offset2 < 0) offset2 = 0;
        len = Math.min(Math.min(len, array.length - offset1), array.length - offset2);
        for (var i = 0; i < len; i++, offset1++, offset2++) {
            var aux = array[offset1];
            array[offset1] = array[offset2];
            array[offset2] = aux;
        }
    }

    /**
     * Swaps a series of elements in the given byte array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 2, 1) -&gt; [3, 2, 1, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 0, 1) -&gt; [1, 2, 3, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 2, 0, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], -3, 2, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 3, 3) -&gt; [4, 2, 3, 1]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(byte[] array, int offset1, int offset2, int len) {
        if (len == 1) swapByte(array, offset1, offset2);
        else {
            var alen = array.length;
            if (alen <= 1 || offset1 >= alen || offset2 >= alen) return;
            if (offset1 < 0) offset1 = 0;
            if (offset2 < 0) offset2 = 0;
            len = Math.min(Math.min(len, alen - offset1), alen - offset2);
            for (var i = 0; i < len; i++, offset1++, offset2++)
                swapByte(array, offset1, offset2);
        }
    }

    public static void swapByte(byte[] array, int offset1, int offset2) {
        if (offset1 == offset2)
            return;
        var aux = array[offset1];
        array[offset1] = array[offset2];
        array[offset2] = aux;
    }

    public static void swapLong(long[] array, int offset1, int offset2) {
        if (offset1 == offset2)
            return;
        var aux = array[offset1];
        array[offset1] = array[offset2];
        array[offset2] = aux;
    }

    public static void swapInt(int[] array, int offset1, int offset2) {
        if (offset1 == offset2)
            return;
        var aux = array[offset1];
        array[offset1] = array[offset2];
        array[offset2] = aux;
    }

    public static void swapDouble(double[] array, int offset1, int offset2) {
        if (offset1 == offset2)
            return;
        var aux = array[offset1];
        array[offset1] = array[offset2];
        array[offset2] = aux;
    }

    public static void swapFloat(float[] array, int offset1, int offset2) {
        if (offset1 == offset2)
            return;
        var aux = array[offset1];
        array[offset1] = array[offset2];
        array[offset2] = aux;
    }

    public static void swapBool(boolean[] array, int offset1, int offset2) {
        if (offset1 == offset2)
            return;
        var aux = array[offset1];
        array[offset1] = array[offset2];
        array[offset2] = aux;
    }

    public static void swapObj(Object[] o, int a, int b) {
        if (a == b)
            return;
        var aux = o[a];
        o[a] = o[b];
        o[b] = aux;
    }

    public static void swapObjFloat(Object[] o, float[] f, int a, int b) {
        if (a == b) return;

        var ox = o[a];
        o[a] = o[b];
        o[b] = ox;

        var fx = f[a];
        f[a] = f[b];
        f[b] = fx;
    }

    public static void swapObjShort(Object[] o, short[] f, int a, int b) {
        if (a == b) return;

        var ox = o[a];
        o[a] = o[b];
        o[b] = ox;

        var fx = f[a];
        f[a] = f[b];
        f[b] = fx;
    }

    public static void swapObjInt(Object[] o, int[] f, int a, int b) {
        if (a == b) return;

        var ox = o[a];
        o[a] = o[b];
        o[b] = ox;

        var fx = f[a];
        f[a] = f[b];
        f[b] = fx;
    }

    static void swapShort(short[] array, int offset1, int offset2) {
        if (offset1 != offset2) {
            var aux = array[offset1];
            array[offset1] = array[offset2];
            array[offset2] = aux;
        }
    }

    /**
     * Swaps a series of elements in the given long array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 2, 1) -&gt; [3, 2, 1, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 0, 1) -&gt; [1, 2, 3, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 2, 0, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], -3, 2, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 3, 3) -&gt; [4, 2, 3, 1]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(long[] array, int offset1, int offset2, int len) {
        if (len == 1) swapLong(array, offset1, offset2);
        else {
            var alen = array.length;
            if (alen <= 1 || offset1 >= alen || offset2 >= alen) return;
            if (offset1 < 0) offset1 = 0;
            if (offset2 < 0) offset2 = 0;
            len = Math.min(Math.min(len, alen - offset1), alen - offset2);
            for (var i = 0; i < len; i++, offset1++, offset2++)
                swapLong(array, offset1, offset2);
        }
    }

    public static void swap(int[] array, int offset1, int offset2, int len) {
        if (len == 1) swapInt(array, offset1, offset2);
        else {
            var alen = array.length;
            if (alen <= 1 || offset1 >= alen || offset2 >= alen) return;
            if (offset1 < 0) offset1 = 0;
            if (offset2 < 0) offset2 = 0;
            len = Math.min(Math.min(len, alen - offset1), alen - offset2);
            for (var i = 0; i < len; i++, offset1++, offset2++)
                swapInt(array, offset1, offset2);
        }
    }

    public static void swap(short[] array, int offset1, int offset2, int len) {
        if (len == 1) swapShort(array, offset1, offset2);
        else {
            var alen = array.length;
            if (alen <= 1 || offset1 >= alen || offset2 >= alen) return;
            if (offset1 < 0) offset1 = 0;
            if (offset2 < 0) offset2 = 0;
            len = Math.min(Math.min(len, alen - offset1), alen - offset2);
            for (var i = 0; i < len; i++, offset1++, offset2++)
                swapShort(array, offset1, offset2);
        }
    }

    /**
     * Swaps a series of elements in the given char array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 2, 1) -&gt; [3, 2, 1, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 0, 1) -&gt; [1, 2, 3, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 2, 0, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], -3, 2, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 3, 3) -&gt; [4, 2, 3, 1]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(char[] array, int offset1, int offset2, int len) {
        if (array == null || array.length == 0 || offset1 >= array.length || offset2 >= array.length) return;
        if (offset1 < 0) offset1 = 0;
        if (offset2 < 0) offset2 = 0;
        len = Math.min(Math.min(len, array.length - offset1), array.length - offset2);
        for (var i = 0; i < len; i++, offset1++, offset2++) {
            var aux = array[offset1];
            array[offset1] = array[offset2];
            array[offset2] = aux;
        }
    }

    /**
     * Swaps a series of elements in the given double array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 2, 1) -&gt; [3, 2, 1, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 0, 1) -&gt; [1, 2, 3, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 2, 0, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], -3, 2, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 3, 3) -&gt; [4, 2, 3, 1]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(double[] array, int offset1, int offset2, int len) {
        if (array == null || array.length == 0 || offset1 >= array.length || offset2 >= array.length) return;
        if (offset1 < 0) offset1 = 0;
        if (offset2 < 0) offset2 = 0;
        len = Math.min(Math.min(len, array.length - offset1), array.length - offset2);
        for (var i = 0; i < len; i++, offset1++, offset2++) {
            var aux = array[offset1];
            array[offset1] = array[offset2];
            array[offset2] = aux;
        }
    }

    /**
     * Swaps a series of elements in the given float array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 2, 1) -&gt; [3, 2, 1, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 0, 1) -&gt; [1, 2, 3, 4]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 2, 0, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], -3, 2, 2) -&gt; [3, 4, 1, 2]</li>
     * <li>ArrayUtils.swap([1, 2, 3, 4], 0, 3, 3) -&gt; [4, 2, 3, 1]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(float[] array, int offset1, int offset2, int len) {
        if (array == null || array.length == 0 || offset1 >= array.length || offset2 >= array.length) return;
        if (offset1 < 0) offset1 = 0;
        if (offset2 < 0) offset2 = 0;
        len = Math.min(Math.min(len, array.length - offset1), array.length - offset2);
        for (var i = 0; i < len; i++, offset1++, offset2++) {
            var aux = array[offset1];
            array[offset1] = array[offset2];
            array[offset2] = aux;
        }

    }

    /**
     * Swaps a series of elements in the given array.
     *
     * <p>This method does nothing for a {@code null} or empty input array or
     * for overflow indices. Negative indices are promoted to 0(zero). If any
     * of the sub-arrays to swap falls outside of the given array, then the
     * swap is stopped at the end of the array and as many as possible elements
     * are swapped.</p>
     * <p>
     * Examples:
     * <ul>
     * <li>ArrayUtils.swap(["1", "2", "3", "4"], 0, 2, 1) -&gt; ["3", "2", "1", "4"]</li>
     * <li>ArrayUtils.swap(["1", "2", "3", "4"], 0, 0, 1) -&gt; ["1", "2", "3", "4"]</li>
     * <li>ArrayUtils.swap(["1", "2", "3", "4"], 2, 0, 2) -&gt; ["3", "4", "1", "2"]</li>
     * <li>ArrayUtils.swap(["1", "2", "3", "4"], -3, 2, 2) -&gt; ["3", "4", "1", "2"]</li>
     * <li>ArrayUtils.swap(["1", "2", "3", "4"], 0, 3, 3) -&gt; ["4", "2", "3", "1"]</li>
     * </ul>
     *
     * @param array   the array to swap, may be {@code null}
     * @param offset1 the index of the first element in the series to swap
     * @param offset2 the index of the second element in the series to swap
     * @param len     the number of elements to swap starting with the given indices
     * @since 3.5
     */
    public static void swap(Object[] array, int offset1, int offset2, int len) {
        if (len == 1) swapObj(array, offset1, offset2);
        else {
            var alen = array.length;
            if (alen <= 1 || offset1 >= alen || offset2 >= alen) return;
            if (offset1 < 0) offset1 = 0;
            if (offset2 < 0) offset2 = 0;
            len = Math.min(Math.min(len, alen - offset1), alen - offset2);
            for (var i = 0; i < len; i++, offset1++, offset2++)
                swapObj(array, offset1, offset2);
        }
    }

    /**
     * Shifts the order of the given array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(Object[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given long array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(long[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given int array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(int[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given short array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(short[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given char array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(char[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given byte array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(byte[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given double array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(double[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given float array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(float[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of the given boolean array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array  the array to shift, may be {@code null}
     * @param offset The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *               rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(boolean[] array, int offset) {
        shift(array, 0, array.length, offset);
    }

    /**
     * Shifts the order of a series of elements in the given boolean array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(boolean[] array, int startIndexInclusive, int endIndexExclusive, int offset) {
        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given byte array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(byte[] array, int startIndexInclusive, int endIndexExclusive, int offset) {
        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given char array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(char[] array, int startIndexInclusive, int endIndexExclusive, int offset) {
        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given double array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(double[] array, int startIndexInclusive, int endIndexExclusive, int offset) {
        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given float array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(float[] array, int startIndexInclusive, int endIndexExclusive, int offset) {
        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given int array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(int[] array, int startIndexInclusive, int endIndexExclusive, int offset) {
        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given long array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(long[] array, int startIndexInclusive, int endIndexExclusive, int offset) {

        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(Object[] array, int startIndexInclusive, int endIndexExclusive, int offset) {

        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * Shifts the order of a series of elements in the given short array.
     *
     * <p>There is no special handling for multi-dimensional arrays. This method
     * does nothing for {@code null} or empty input arrays.</p>
     *
     * @param array               the array to shift, may be {@code null}
     * @param startIndexInclusive the starting index. Undervalue (&lt;0) is promoted to 0, overvalue (&gt;array.length) results in no
     *                            change.
     * @param endIndexExclusive   elements up to endIndex-1 are shifted in the array. Undervalue (&lt; start index) results in no
     *                            change. Overvalue (&gt;array.length) is demoted to array length.
     * @param offset              The number of positions to rotate the elements.  If the offset is larger than the number of elements to
     *                            rotate, than the effective offset is modulo the number of elements to rotate.
     * @since 3.5
     */
    public static void shift(short[] array, int startIndexInclusive, int endIndexExclusive, int offset) {

        if (startIndexInclusive >= array.length - 1 || endIndexExclusive <= 0) return;
        if (startIndexInclusive < 0) startIndexInclusive = 0;
        if (endIndexExclusive >= array.length) endIndexExclusive = array.length;
        var n = endIndexExclusive - startIndexInclusive;
        if (n <= 1) return;
        offset %= n;
        if (offset < 0) offset += n;


        while (n > 1 && offset > 0) {
            var n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset, n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }

    /**
     * <p>Finds the index of the given object in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array        the array to search through for the object, may be {@code null}
     * @param objectToFind the object to find, may be {@code null}
     * @return the index of the object within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    /**
     * <p>Finds the index of the given object in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array        the array to search through for the object, may be {@code null}
     * @param objectToFind the object to find, may be {@code null}
     * @param startIndex   the index to start searching at
     * @return the index of the object within the array starting at the index,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {

        if (startIndex < 0) startIndex = 0;
        if (objectToFind == null) {
            return IntStream.range(startIndex, array.length).filter(i -> array[i] == null).findFirst().orElse(INDEX_NOT_FOUND);
        } else {
            return IntStream.range(startIndex, array.length).filter(i -> objectToFind.equals(array[i])).findFirst().orElse(INDEX_NOT_FOUND);
        }
    }

    public static <X> int indexOf(X[] array, Predicate<X> test) {
        return indexOf(array, test, 0);
    }

    public static <X> int indexOf(X[] array, Predicate<X> test, int startIndex) {
//        if (startIndex < 0)
//            startIndex = 0;

        return IntStream.range(startIndex, array.length).filter(i -> test.test(array[i])).findFirst().orElse(INDEX_NOT_FOUND);

    }

    /**
     * <p>Finds the last index of the given object within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array        the array to traverse backwards looking for the object, may be {@code null}
     * @param objectToFind the object to find, may be {@code null}
     * @return the last index of the object within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(Object[] array, Object objectToFind) {
        return lastIndexOf(array, objectToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given object in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than
     * the array length will search from the end of the array.
     *
     * @param array        the array to traverse for looking for the object, may be {@code null}
     * @param objectToFind the object to find, may be {@code null}
     * @param startIndex   the start index to traverse backwards from
     * @return the last index of the object within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        if (objectToFind == null)
            return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> array[i] == null).findFirst().orElse(INDEX_NOT_FOUND);
        else if (array.getClass().getComponentType().isInstance(objectToFind))
            return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> objectToFind.equals(array[i])).findFirst().orElse(INDEX_NOT_FOUND);
        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if the object is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array        the array to search through
     * @param objectToFind the object to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(long[] array, long valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(long[] array, long valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(long[] array, long valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(int[] array, int valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(int[] array, int valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * quick search for items by identity
     * returns first matching index, though others could exist
     */
    public static int indexOfInstance(Object[] xx, Object y) {
        return IntStream.range(0, xx.length).filter(i -> y == xx[i]).findFirst().orElse(-1);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(int[] array, int valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(int[] array, int valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(int[] array, int valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(short[] array, short valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(short[] array, short valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(short[] array, short valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(short[] array, short valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(short[] array, short valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int indexOf(char[] array, char valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int indexOf(char[] array, char valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int lastIndexOf(char[] array, char valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     * @since 2.1
     */
    public static int lastIndexOf(char[] array, char valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     * @since 2.1
     */
    public static boolean contains(char[] array, char valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(byte[] array, byte valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(byte[] array, byte valueToFind, int startIndex, int endIndex) {
//        if (array != null) {
//            if (startIndex < 0)
//                startIndex = 0;
        return IntStream.range(startIndex, endIndex).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
        //        }
    }

    public static int indexOf(byte[] array, byte valueToFind, int startIndex) {
        return indexOf(array, valueToFind, startIndex, array.length);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(byte[] array, byte valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(byte[] array, byte valueToFind, int startIndex) {
        if (array == null) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(byte[] array, byte valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value within a given tolerance in the array.
     * This method will return the index of the first value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param tolerance   tolerance of the search
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind, double tolerance) {
        return indexOf(array, valueToFind, 0, tolerance);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind, int startIndex) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     * This method will return the index of the first value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @param tolerance   tolerance of the search
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(double[] array, double valueToFind, int startIndex, double tolerance) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        var min = valueToFind - tolerance;
        var max = valueToFind + tolerance;
        return IntStream.range(startIndex, array.length).filter(i -> array[i] >= min && array[i] <= max).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value within a given tolerance in the array.
     * This method will return the index of the last value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param tolerance   tolerance of the search
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind, double tolerance) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE, tolerance);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind, int startIndex) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     * This method will return the index of the last value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @param tolerance   search for value within plus/minus this amount
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(double[] array, double valueToFind, int startIndex, double tolerance) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        var min = valueToFind - tolerance;
        var max = valueToFind + tolerance;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> array[i] >= min && array[i] <= max).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(double[] array, double valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Checks if a value falling within the given tolerance is in the
     * given array.  If the array contains a value within the inclusive range
     * defined by (value - tolerance) to (value + tolerance).
     *
     * <p>The method returns {@code false} if a {@code null} array
     * is passed in.
     *
     * @param array       the array to search
     * @param valueToFind the value to find
     * @param tolerance   the array contains the tolerance of the search
     * @return true if value falling within tolerance is in array
     */
    public static boolean contains(double[] array, double valueToFind, double tolerance) {
        return indexOf(array, valueToFind, 0, tolerance) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(float[] array, float valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(float[] array, float valueToFind, int startIndex) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(float[] array, float valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(float[] array, float valueToFind, int startIndex) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(float[] array, float valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value in the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(boolean[] array, boolean valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param array       the array to search through for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the index to start searching at
     * @return the index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null}
     * array input
     */
    public static int indexOf(boolean[] array, boolean valueToFind, int startIndex) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) startIndex = 0;
        return IntStream.range(startIndex, array.length).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Finds the last index of the given value within the array.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) if
     * {@code null} array input.
     *
     * @param array       the array to traverse backwards looking for the object, may be {@code null}
     * @param valueToFind the object to find
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(boolean[] array, boolean valueToFind) {
        return lastIndexOf(array, valueToFind, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than
     * the array length will search from the end of the array.
     *
     * @param array       the array to traverse for looking for the object, may be {@code null}
     * @param valueToFind the value to find
     * @param startIndex  the start index to traverse backwards from
     * @return the last index of the value within the array,
     * {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(boolean[] array, boolean valueToFind, int startIndex) {
        if (array.length == 0) return INDEX_NOT_FOUND;
        if (startIndex < 0) return INDEX_NOT_FOUND;
        if (startIndex >= array.length) startIndex = array.length - 1;
        return IntStream.iterate(startIndex, i -> i >= 0, i -> i - 1).filter(i -> valueToFind == array[i]).findFirst().orElse(INDEX_NOT_FOUND);
    }

    /**
     * <p>Checks if the value is in the given array.
     *
     * <p>The method returns {@code false} if a {@code null} array is passed in.
     *
     * @param array       the array to search through
     * @param valueToFind the value to find
     * @return {@code true} if the array contains the object
     */
    public static boolean contains(boolean[] array, boolean valueToFind) {
        return indexOf(array, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     * <p>Converts an array of object Characters to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Character} array, may be {@code null}
     * @return a {@code char} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static char[] toPrimitive(Character[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_CHAR_ARRAY;
        var result = new char[array.length];
        for (var i = 0; i < array.length; i++) result[i] = array[i];
        return result;
    }

    /**
     * <p>Converts an array of object Character to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Character} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code char} array, {@code null} if null array input
     */
    public static char[] toPrimitive(Character[] array, char valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_CHAR_ARRAY;
        var result = new char[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive chars to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code char} array
     * @return a {@code Character} array, {@code null} if null array input
     */
    public static Character[] toObject(char[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_CHARACTER_OBJECT_ARRAY;
        return IntStream.range(0, array.length).mapToObj(i -> array[i]).toArray(Character[]::new);
    }

    /**
     * <p>Converts an array of object Longs to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Long} array, may be {@code null}
     * @return a {@code long} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static long[] toPrimitive(Long[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_LONG_ARRAY;
        var result = new long[10];
        var count = 0;
        for (var aLong : array) {
            if (result.length == count) result = Arrays.copyOf(result, count * 2);
            long l = aLong;
            result[count++] = l;
        }
        result = Arrays.copyOfRange(result, 0, count);
        return result;
    }

    /**
     * <p>Converts an array of object Long to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Long} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code long} array, {@code null} if null array input
     */
    public static long[] toPrimitive(Long[] array, long valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_LONG_ARRAY;
        var result = new long[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive longs to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code long} array
     * @return a {@code Long} array, {@code null} if null array input
     */
    public static Long[] toObject(long[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_LONG_OBJECT_ARRAY;
        return Arrays.stream(array).boxed().toArray(Long[]::new);
    }

    /**
     * <p>Converts an array of object Integers to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Integer} array, may be {@code null}
     * @return an {@code int} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static int[] toPrimitive(Integer[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_INT_ARRAY;
        var result = new int[10];
        var count = 0;
        for (var integer : array) {
            if (result.length == count) result = Arrays.copyOf(result, count * 2);
            int i = integer;
            result[count++] = i;
        }
        result = Arrays.copyOfRange(result, 0, count);
        return result;
    }

    /**
     * <p>Converts an array of object Integer to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Integer} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return an {@code int} array, {@code null} if null array input
     */
    public static int[] toPrimitive(Integer[] array, int valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_INT_ARRAY;
        var result = new int[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive ints to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array an {@code int} array
     * @return an {@code Integer} array, {@code null} if null array input
     */
    public static Integer[] toObject(int[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_INTEGER_OBJECT_ARRAY;
        return Arrays.stream(array).boxed().toArray(Integer[]::new);
    }

    /**
     * <p>Converts an array of object Shorts to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Short} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static short[] toPrimitive(Short[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_SHORT_ARRAY;
        var result = new short[array.length];
        for (var i = 0; i < array.length; i++) result[i] = array[i];
        return result;
    }

    /**
     * <p>Converts an array of object Short to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Short} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static short[] toPrimitive(Short[] array, short valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_SHORT_ARRAY;
        var result = new short[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive shorts to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code short} array
     * @return a {@code Short} array, {@code null} if null array input
     */
    public static Short[] toObject(short[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_SHORT_OBJECT_ARRAY;
        return IntStream.range(0, array.length).mapToObj(i -> array[i]).toArray(Short[]::new);
    }

    /**
     * <p>Converts an array of object Bytes to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Byte} array, may be {@code null}
     * @return a {@code byte} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static byte[] toPrimitive(Byte[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_BYTE_ARRAY;
        var result = new byte[array.length];
        for (var i = 0; i < array.length; i++) result[i] = array[i];
        return result;
    }

    /**
     * <p>Converts an array of object Bytes to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Byte} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static byte[] toPrimitive(Byte[] array, byte valueForNull) {
        byte[] res;
        if (array == null) res = null;
        else if (array.length == 0) res = EMPTY_BYTE_ARRAY;
        else {
            var result = new byte[array.length];
            for (var i = 0; i < array.length; i++) {
                var b = array[i];
                result[i] = (b == null ? valueForNull : b);
            }
            res = result;
        }
        return res;
    }

    /**
     * <p>Converts an array of primitive bytes to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code byte} array
     * @return a {@code Byte} array, {@code null} if null array input
     */
    public static Byte[] toObject(byte[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_BYTE_OBJECT_ARRAY;
        return IntStream.range(0, array.length).mapToObj(i -> array[i]).toArray(Byte[]::new);
    }

    /**
     * <p>Converts an array of object Doubles to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Double} array, may be {@code null}
     * @return a {@code double} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static double[] toPrimitive(Double[] array) {
        double[] res;
        if (array == null) res = null;
        else if (array.length == 0) res = EMPTY_DOUBLE_ARRAY;
        else {
            var arr = new double[10];
            var count = 0;
            for (var aDouble : array) {
                if (arr.length == count) arr = Arrays.copyOf(arr, count * 2);
                double v = aDouble;
                arr[count++] = v;
            }
            arr = Arrays.copyOfRange(arr, 0, count);
            res = arr;
        }
        return res;
    }

    /**
     * <p>Converts an array of object Doubles to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Double} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code double} array, {@code null} if null array input
     */
    public static double[] toPrimitive(Double[] array, double valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_DOUBLE_ARRAY;
        var result = new double[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive doubles to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code double} array
     * @return a {@code Double} array, {@code null} if null array input
     */
    public static Double[] toObject(double[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_DOUBLE_OBJECT_ARRAY;
        return Arrays.stream(array).boxed().toArray(Double[]::new);
    }

    /**
     * <p>Converts an array of object Floats to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Float} array, may be {@code null}
     * @return a {@code float} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static float[] toPrimitive(Float[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_FLOAT_ARRAY;
        var result = new float[array.length];
        for (var i = 0; i < array.length; i++) result[i] = array[i];
        return result;
    }

    /**
     * <p>Converts an array of object Floats to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Float} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code float} array, {@code null} if null array input
     */
    public static float[] toPrimitive(Float[] array, float valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_FLOAT_ARRAY;
        var result = new float[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive floats to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code float} array
     * @return a {@code Float} array, {@code null} if null array input
     */
    public static Float[] toObject(float[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_FLOAT_OBJECT_ARRAY;
        return IntStream.range(0, array.length).mapToObj(i -> array[i]).toArray(Float[]::new);
    }

    /**
     * <p>Converts an array of object Booleans to primitives.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code Boolean} array, may be {@code null}
     * @return a {@code boolean} array, {@code null} if null array input
     * @throws NullPointerException if array content is {@code null}
     */
    public static boolean[] toPrimitive(Boolean[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_BOOLEAN_ARRAY;
        var result = new boolean[array.length];
        for (var i = 0; i < array.length; i++) result[i] = array[i];
        return result;
    }

    /**
     * <p>Converts an array of object Booleans to primitives handling {@code null}.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array        a {@code Boolean} array, may be {@code null}
     * @param valueForNull the value to insert if {@code null} found
     * @return a {@code boolean} array, {@code null} if null array input
     */
    public static boolean[] toPrimitive(Boolean[] array, boolean valueForNull) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_BOOLEAN_ARRAY;
        var result = new boolean[array.length];
        for (var i = 0; i < array.length; i++) {
            var b = array[i];
            result[i] = (Optional.ofNullable(b).orElse(valueForNull));
        }
        return result;
    }

    /**
     * <p>Converts an array of primitive booleans to objects.
     *
     * <p>This method returns {@code null} for a {@code null} input array.
     *
     * @param array a {@code boolean} array
     * @return a {@code Boolean} array, {@code null} if null array input
     */
    public static Boolean[] toObject(boolean[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_BOOLEAN_OBJECT_ARRAY;
        return IntStream.range(0, array.length).mapToObj(i -> (array[i] ? Boolean.TRUE : Boolean.FALSE)).toArray(Boolean[]::new);
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(null, null)     = null
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * ArrayUtils.addAll([null], [null]) = [null, null]
     * ArrayUtils.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param <T>    the component type of the array
     * @param array1 the first array whose elements are added to the new array, may be {@code null}
     * @param array2 the second array whose elements are added to the new array, may be {@code null}
     * @return The new array, {@code null} if both arrays are {@code null}.
     * The type of the new array is the type of the first array,
     * unless the first array is null, in which case the type is the same as the second array.
     * @throws IllegalArgumentException if the array types are incompatible
     * @since 2.1
     */
    @SafeVarargs
    public static <T> T[] addAll(T[] array1, T... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var type1 = array1.getClass().getComponentType();
        @SuppressWarnings("unchecked") var joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        try {
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        } catch (ArrayStoreException ase) {

            /*
             * We do this here, rather than before the copy because:
             * - it would be a wasted check most of the time
             * - safer, in case check turns out to be too strict
             */
            var type2 = array2.getClass().getComponentType();
            if (!type1.isAssignableFrom(type2))
                throw new IllegalArgumentException("Cannot store " + type2.getName() + " in an array of "
                        + type1.getName(), ase);
            throw ase;
        }
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new boolean[] array.
     * @since 2.1
     */
    public static boolean[] addAll(boolean[] array1, boolean... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new boolean[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new char[] array.
     * @since 2.1
     */
    public static char[] addAll(char[] array1, char... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new char[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new byte[] array.
     * @since 2.1
     */
    public static byte[] addAll(byte[] array1, byte... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new short[] array.
     * @since 2.1
     */
    public static short[] addAll(short[] array1, short... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new short[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new int[] array.
     * @since 2.1
     */
    public static int[] addAll(int[] array1, int... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new int[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new long[] array.
     * @since 2.1
     */
    public static long[] addAll(long[] array1, long... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new long[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new float[] array.
     * @since 2.1
     */
    public static float[] addAll(float[] array1, float... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new float[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * <p>The new array contains all of the element of {@code array1} followed
     * by all of the elements {@code array2}. When an array is returned, it is always
     * a new array.
     *
     * <pre>
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array.
     * @param array2 the second array whose elements are added to the new array.
     * @return The new double[] array.
     * @since 2.1
     */
    public static double[] addAll(double[] array1, double... array2) {
        if (array1 == null) return clone(array2);
        if (array2 == null) return clone(array1);
        var joinedArray = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element, unless the element itself is null,
     * in which case the return type is Object[]
     *
     * <pre>
     * ArrayUtils.addAt(null, null)      = IllegalArgumentException
     * ArrayUtils.addAt(null, "a")       = ["a"]
     * ArrayUtils.addAt(["a"], null)     = ["a", null]
     * ArrayUtils.addAt(["a"], "b")      = ["a", "b"]
     * ArrayUtils.addAt(["a", "b"], "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param <T>     the component type of the array
     * @param array   the array to "addAt" the element to, may be {@code null}
     * @param element the object to addAt, may be {@code null}
     * @return A new array containing the existing elements plus the new element
     * The returned array type will be that of the input array (unless null),
     * in which case it will have the same type as the element.
     * If both are null, an IllegalArgumentException is thrown
     * @throws IllegalArgumentException if both arguments are null
     * @since 2.1
     */
    public static <T> T[] add(T[] array, T element) {
        Class<?> type;
        if (array != null) type = array.getClass().getComponentType();
        else if (element != null) type = element.getClass();
        else throw new IllegalArgumentException("Arguments cannot both be null");
        @SuppressWarnings("unchecked") var newArray = (T[]) copyArrayGrow1(array, type);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, true)          = [true]
     * ArrayUtils.addAt([true], false)       = [true, false]
     * ArrayUtils.addAt([true, false], true) = [true, false, true]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static boolean[] add(boolean[] array, boolean element) {
        var newArray = (boolean[]) copyArrayGrow1(array, Boolean.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0)   = [0]
     * ArrayUtils.addAt([1], 0)    = [1, 0]
     * ArrayUtils.addAt([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static byte[] add(byte[] array, byte element) {
        var newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, '0')       = ['0']
     * ArrayUtils.addAt(['1'], '0')      = ['1', '0']
     * ArrayUtils.addAt(['1', '0'], '1') = ['1', '0', '1']
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static char[] add(char[] array, char element) {
        var newArray = (char[]) copyArrayGrow1(array, Character.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0)   = [0]
     * ArrayUtils.addAt([1], 0)    = [1, 0]
     * ArrayUtils.addAt([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static double[] add(double[] array, double element) {
        var newArray = (double[]) copyArrayGrow1(array, Double.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0)   = [0]
     * ArrayUtils.addAt([1], 0)    = [1, 0]
     * ArrayUtils.addAt([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static float[] add(float[] array, float element) {
        var newArray = (float[]) copyArrayGrow1(array, Float.TYPE);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0)   = [0]
     * ArrayUtils.addAt([1], 0)    = [1, 0]
     * ArrayUtils.addAt([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static int[] add(int[] array, int element) {
        var newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0)   = [0]
     * ArrayUtils.addAt([1], 0)    = [1, 0]
     * ArrayUtils.addAt([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static long[] add(long[] array, long element) {
        var newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    /**
     * <p>Copies the given array and adds the given element at the end of the new array.
     *
     * <p>The new array contains the same elements of the input
     * array plus the given element in the last position. The component type of
     * the new array is the same as that of the input array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0)   = [0]
     * ArrayUtils.addAt([1], 0)    = [1, 0]
     * ArrayUtils.addAt([1, 0], 1) = [1, 0, 1]
     * </pre>
     *
     * @param array   the array to copy and add the element to, may be {@code null}
     * @param element the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static short[] add(short[] array, short element) {
        var newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    public static short[] prepend(short[] array, short element) {
        var newArray = new short[array.length + 1];
        newArray[0] = element;
        System.arraycopy(array, 0, newArray, 1, array.length);
        return newArray;
    }

    /**
     * Returns a copy of the given array of size 1 greater than the argument.
     * The last value of the array is left to the default value.
     *
     * @param array                 The array to copy, must not be {@code null}.
     * @param newArrayComponentType If {@code array} is {@code null}, create a
     *                              size 1 array of this type.
     * @return A new copy of the array of size 1 greater than the input.
     */
    private static Object copyArrayGrow1(Object array, Class<?> newArrayComponentType) {
        if (array != null) {
            var arrayLength = Array.getLength(array);
            var newArray = Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
            System.arraycopy(array, 0, newArray, 0, arrayLength);
            return newArray;
        }
        return Array.newInstance(newArrayComponentType, 1);
    }

    private static byte[] copyArrayGrow1(byte[] array) {
        return Optional.ofNullable(array).map(bytes -> Arrays.copyOf(bytes, bytes.length + 1)).orElseGet(() -> new byte[1]);
    }

    private static short[] copyArrayGrow1(short[] array) {
        return Optional.ofNullable(array).map(shorts -> Arrays.copyOf(shorts, shorts.length + 1)).orElseGet(() -> new short[1]);
    }

    private static long[] copyArrayGrow1(long[] array) {
        return Optional.ofNullable(array).map(longs -> Arrays.copyOf(longs, longs.length + 1)).orElseGet(() -> new long[1]);
    }

    private static int[] copyArrayGrow1(int[] array) {
        return Optional.ofNullable(array).map(ints -> Arrays.copyOf(ints, ints.length + 1)).orElseGet(() -> new int[1]);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0, null)      = IllegalArgumentException
     * ArrayUtils.addAt(null, 0, "a")       = ["a"]
     * ArrayUtils.addAt(["a"], 1, null)     = ["a", null]
     * ArrayUtils.addAt(["a"], 1, "b")      = ["a", "b"]
     * ArrayUtils.addAt(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param <T>     the component type of the array
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; array.length).
     * @throws IllegalArgumentException  if both array and element are null
     * @deprecated this method has been superseded by {@link #insert(int, Object[], Object...) insert(int, T[], T...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static <T> T[] add(T[] array, int index, T element) {
        Class<?> clss;
        if (array != null) clss = array.getClass().getComponentType();
        else if (element != null) clss = element.getClass();
        else throw new IllegalArgumentException("Array and element cannot both be null");
        @SuppressWarnings("unchecked") var newArray = (T[]) add(array, index, element, clss);
        return newArray;
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0, true)          = [true]
     * ArrayUtils.addAt([true], 0, false)       = [false, true]
     * ArrayUtils.addAt([false], 1, true)       = [false, true]
     * ArrayUtils.addAt([true, false], 1, true) = [true, true, false]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, boolean[], boolean...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static boolean[] add(boolean[] array, int index, boolean element) {
        return (boolean[]) add(array, index, element, Boolean.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt(null, 0, 'a')            = ['a']
     * ArrayUtils.addAt(['a'], 0, 'b')           = ['b', 'a']
     * ArrayUtils.addAt(['a', 'b'], 0, 'c')      = ['c', 'a', 'b']
     * ArrayUtils.addAt(['a', 'b'], 1, 'k')      = ['a', 'k', 'b']
     * ArrayUtils.addAt(['a', 'b', 'c'], 1, 't') = ['a', 't', 'b', 'c']
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, char[], char...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static char[] add(char[] array, int index, char element) {
        return (char[]) add(array, index, element, Character.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt([1], 0, 2)         = [2, 1]
     * ArrayUtils.addAt([2, 6], 2, 3)      = [2, 6, 3]
     * ArrayUtils.addAt([2, 6], 0, 1)      = [1, 2, 6]
     * ArrayUtils.addAt([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, byte[], byte...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static byte[] add(byte[] array, int index, byte element) {
        return (byte[]) add(array, index, element, Byte.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt([1], 0, 2)         = [2, 1]
     * ArrayUtils.addAt([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.addAt([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.addAt([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, short[], short...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static short[] add(short[] array, int index, short element) {
        return (short[]) add(array, index, element, Short.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt([1], 0, 2)         = [2, 1]
     * ArrayUtils.addAt([2, 6], 2, 10)     = [2, 6, 10]
     * ArrayUtils.addAt([2, 6], 0, -4)     = [-4, 2, 6]
     * ArrayUtils.addAt([2, 6, 3], 2, 1)   = [2, 6, 1, 3]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, int[], int...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static int[] add(int[] array, int index, int element) {
        return (int[]) add(array, index, element, Integer.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt([1L], 0, 2L)           = [2L, 1L]
     * ArrayUtils.addAt([2L, 6L], 2, 10L)      = [2L, 6L, 10L]
     * ArrayUtils.addAt([2L, 6L], 0, -4L)      = [-4L, 2L, 6L]
     * ArrayUtils.addAt([2L, 6L, 3L], 2, 1L)   = [2L, 6L, 1L, 3L]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, long[], long...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static long[] add(long[] array, int index, long element) {
        return (long[]) add(array, index, element, Long.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt([1.1f], 0, 2.2f)               = [2.2f, 1.1f]
     * ArrayUtils.addAt([2.3f, 6.4f], 2, 10.5f)        = [2.3f, 6.4f, 10.5f]
     * ArrayUtils.addAt([2.6f, 6.7f], 0, -4.8f)        = [-4.8f, 2.6f, 6.7f]
     * ArrayUtils.addAt([2.9f, 6.0f, 0.3f], 2, 1.0f)   = [2.9f, 6.0f, 1.0f, 0.3f]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, float[], float...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static float[] add(float[] array, int index, float element) {
        return (float[]) add(array, index, element, Float.TYPE);
    }

    /**
     * <p>Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array plus the given element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, a new one element array is returned
     * whose component type is the same as the element.
     *
     * <pre>
     * ArrayUtils.addAt([1.1], 0, 2.2)              = [2.2, 1.1]
     * ArrayUtils.addAt([2.3, 6.4], 2, 10.5)        = [2.3, 6.4, 10.5]
     * ArrayUtils.addAt([2.6, 6.7], 0, -4.8)        = [-4.8, 2.6, 6.7]
     * ArrayUtils.addAt([2.9, 6.0, 0.3], 2, 1.0)    = [2.9, 6.0, 1.0, 0.3]
     * </pre>
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt; array.length).
     * @deprecated this method has been superseded by {@link #insert(int, double[], double...)} and
     * may be removed in a future release. Please note the handling of {@code null} input arrays differs
     * in the new method: inserting {@code X} into a {@code null} array results in {@code null} not {@code X}.
     */
    @Deprecated
    public static double[] add(double[] array, int index, double element) {
        return (double[]) add(array, index, element, Double.TYPE);
    }

    /**
     * Underlying implementation of addAt(array, index, element) methods.
     * The last parameter is the class, which may not equal element.getClass
     * for primitives.
     *
     * @param array   the array to add the element to, may be {@code null}
     * @param index   the position of the new object
     * @param element the object to addAt
     * @param clss    the type of the element being added
     * @return A new array containing the existing elements and the new element
     */
    private static Object add(Object array, int index, Object element, Class<?> clss) {
        if (array == null) {
            if (index != 0) throw new IndexOutOfBoundsException("Index: " + index + ", Length: 0");
            var joinedArray = Array.newInstance(clss, 1);
            Array.set(joinedArray, 0, element);
            return joinedArray;
        }
        var length = Array.getLength(array);
        if (index > length || index < 0) throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        var result = Array.newInstance(clss, length + 1);
        System.arraycopy(array, 0, result, 0, index);
        Array.set(result, index, element);
        if (index < length) System.arraycopy(array, index, result, index + 1, length - index);
        return result;
    }


    public static <X> X[] prepend(X element, X[] x) {
        var len = x.length;
        var y = Arrays.copyOf(x, len + 1);
        y[0] = element;
        System.arraycopy(x, 0, y, 1, len);
        return y;
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove(["a"], 0)           = []
     * ArrayUtils.remove(["a", "b"], 0)      = ["b"]
     * ArrayUtils.remove(["a", "b"], 1)      = ["a"]
     * ArrayUtils.remove(["a", "b", "c"], 1) = ["a", "c"]
     * </pre>
     *
     * @param <T>   the component type of the array
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] remove(T[] array, int index) {
        return (T[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, "a")            = null
     * ArrayUtils.removeElement([], "a")              = []
     * ArrayUtils.removeElement(["a"], "b")           = ["a"]
     * ArrayUtils.removeElement(["a", "b"], "a")      = ["b"]
     * ArrayUtils.removeElement(["a", "b", "a"], "a") = ["b", "a"]
     * </pre>
     *
     * @param <T>     the component type of the array
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static <T> T[] removeElement(T[] array, Object element) {
        var index = indexOf(array, element);
        return index == INDEX_NOT_FOUND ? clone(array) : remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([true], 0)              = []
     * ArrayUtils.remove([true, false], 0)       = [false]
     * ArrayUtils.remove([true, false], 1)       = [true]
     * ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static boolean[] remove(boolean[] array, int index) {
        return (boolean[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, true)                = null
     * ArrayUtils.removeElement([], true)                  = []
     * ArrayUtils.removeElement([true], false)             = [true]
     * ArrayUtils.removeElement([true, false], false)      = [true]
     * ArrayUtils.removeElement([true, false, true], true) = [false, true]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static boolean[] removeElement(boolean[] array, boolean element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([1], 0)          = []
     * ArrayUtils.remove([1, 0], 0)       = [0]
     * ArrayUtils.remove([1, 0], 1)       = [1]
     * ArrayUtils.remove([1, 0, 1], 1)    = [1, 1]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static byte[] remove(byte[] array, int index) {
        return (byte[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)        = null
     * ArrayUtils.removeElement([], 1)          = []
     * ArrayUtils.removeElement([1], 0)         = [1]
     * ArrayUtils.removeElement([1, 0], 0)      = [1]
     * ArrayUtils.removeElement([1, 0, 1], 1)   = [0, 1]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static byte[] removeElement(byte[] array, byte element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove(['a'], 0)           = []
     * ArrayUtils.remove(['a', 'b'], 0)      = ['b']
     * ArrayUtils.remove(['a', 'b'], 1)      = ['a']
     * ArrayUtils.remove(['a', 'b', 'c'], 1) = ['a', 'c']
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static char[] remove(char[] array, int index) {
        return (char[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 'a')            = null
     * ArrayUtils.removeElement([], 'a')              = []
     * ArrayUtils.removeElement(['a'], 'b')           = ['a']
     * ArrayUtils.removeElement(['a', 'b'], 'a')      = ['b']
     * ArrayUtils.removeElement(['a', 'b', 'a'], 'a') = ['b', 'a']
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static char[] removeElement(char[] array, char element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static double[] remove(double[] array, int index) {
        return (double[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static double[] removeElement(double[] array, double element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([1.1], 0)           = []
     * ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     * ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     * ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static float[] remove(float[] array, int index) {
        return (float[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1.1)            = null
     * ArrayUtils.removeElement([], 1.1)              = []
     * ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     * ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     * ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static float[] removeElement(float[] array, float element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static int[] remove(int[] array, int index) {
        return (int[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static int[] removeElement(int[] array, int element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static long[] remove(long[] array, int index) {
        return (long[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static long[] removeElement(long[] array, long element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.remove([1], 0)         = []
     * ArrayUtils.remove([2, 6], 0)      = [6]
     * ArrayUtils.remove([2, 6], 1)      = [2]
     * ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    public static short[] remove(short[] array, int index) {
        return (short[]) remove((Object) array, index);
    }

    /**
     * <p>Removes the first occurrence of the specified element from the
     * specified array. All subsequent elements are shifted to the left
     * (subtracts one from their indices). If the array doesn't contains
     * such an element, no elements are removed from the array.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the first occurrence of the specified element. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <pre>
     * ArrayUtils.removeElement(null, 1)      = null
     * ArrayUtils.removeElement([], 1)        = []
     * ArrayUtils.removeElement([1], 2)       = [1]
     * ArrayUtils.removeElement([1, 3], 1)    = [3]
     * ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     *
     * @param array   the array to remove the element from, may be {@code null}
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     * occurrence of the specified element.
     * @since 2.1
     */
    public static short[] removeElement(short[] array, short element) {
        var index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) return clone(array);
        return remove(array, index);
    }

    /**
     * <p>Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from
     * their indices).
     *
     * <p>This method returns a new array with the same elements of the input
     * array except the element on the specified position. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * @param array the array to remove the element from, may not be {@code null}
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     * at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 2.1
     */
    private static Object remove(Object array, int index) {
        var length = getLength(array);
        if (index < 0 || index >= length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);

        var result = Array.newInstance(array.getClass().getComponentType(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1) System.arraycopy(array, index + 1, result, index, length - index - 1);

        return result;
    }

    public static <X> X[] remove(X[] input, IntFunction<X[]> outputter, int index) {
        var length = input.length;
        var output = outputter.apply(length - 1);
        System.arraycopy(input, 0, output, 0, index);
        if (index < length - 1) System.arraycopy(input, index + 1, output, index, length - index - 1);
        return output;
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.removeAll(["a", "b", "c"], 0, 2) = ["b"]
     * ArrayUtils.removeAll(["a", "b", "c"], 1, 2) = ["a"]
     * </pre>
     *
     * @param <T>     the component type of the array
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     * at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] removeAll(T[] array, int... indices) {
        return (T[]) removeAll((Object) array, indices);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, "a", "b")            = null
     * ArrayUtils.removeElements([], "a", "b")              = []
     * ArrayUtils.removeElements(["a"], "b", "c")           = ["a"]
     * ArrayUtils.removeElements(["a", "b"], "a", "c")      = ["b"]
     * ArrayUtils.removeElements(["a", "b", "a"], "a")      = ["b", "a"]
     * ArrayUtils.removeElements(["a", "b", "a"], "a", "a") = ["b"]
     * </pre>
     *
     * @param <T>    the component type of the array
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    @SafeVarargs
    public static <T> T[] removeElements(T[] array, T... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<T, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            var count = occurrences.get(v);
            if (count == null) occurrences.put(v, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        @SuppressWarnings("unchecked") var result = (T[]) removeAll(array, toRemove);
        return result;
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static byte[] removeElements(byte[] array, byte... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Byte, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Byte boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (byte[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except those at the specified positions. The component
     * type of the returned array is always the same as that of the input
     * array.
     *
     * <p>If the input array is {@code null}, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     *
     * <pre>
     * ArrayUtils.removeAll([1], 0)             = []
     * ArrayUtils.removeAll([2, 6], 0)          = [6]
     * ArrayUtils.removeAll([2, 6], 0, 1)       = []
     * ArrayUtils.removeAll([2, 6, 3], 1, 2)    = [2]
     * ArrayUtils.removeAll([2, 6, 3], 0, 2)    = [6]
     * ArrayUtils.removeAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param array   the array to remove the element from, may not be {@code null}
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those
     * at the specified positions.
     * @throws IndexOutOfBoundsException if any index is out of range
     *                                   (index &lt; 0 || index &gt;= array.length), or if the array is {@code null}.
     * @since 3.0.1
     */
    public static short[] removeAll(short[] array, int... indices) {
        return (short[]) removeAll((Object) array, indices);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static short[] removeElements(short[] array, short... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Short, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Short boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (short[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static int[] removeElements(int[] array, int... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Integer, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Integer boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (int[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static char[] removeElements(char[] array, char... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Character, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Character boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (char[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static long[] removeElements(long[] array, long... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Long, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Long boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (long[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static float[] removeElements(float[] array, float... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Float, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Float boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (float[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, 1, 2)      = null
     * ArrayUtils.removeElements([], 1, 2)        = []
     * ArrayUtils.removeElements([1], 2, 3)       = [1]
     * ArrayUtils.removeElements([1, 3], 1, 2)    = [3]
     * ArrayUtils.removeElements([1, 3, 1], 1)    = [3, 1]
     * ArrayUtils.removeElements([1, 3, 1], 1, 1) = [3]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static double[] removeElements(double[] array, double... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Double, MutableInt> occurrences = new HashMap<>(values.length);
        for (var v : values) {
            Double boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (double[]) removeAll(array, toRemove);
    }

    /**
     * <p>Removes occurrences of specified elements, in specified quantities,
     * from the specified array. All subsequent elements are shifted left.
     * For any element-to-be-removed specified in greater quantities than
     * contained in the original array, no change occurs beyond the
     * removal of the existing matching items.
     *
     * <p>This method returns a new array with the same elements of the input
     * array except for the earliest-encountered occurrences of the specified
     * elements. The component type of the returned array is always the same
     * as that of the input array.
     *
     * <pre>
     * ArrayUtils.removeElements(null, true, false)               = null
     * ArrayUtils.removeElements([], true, false)                 = []
     * ArrayUtils.removeElements([true], false, false)            = [true]
     * ArrayUtils.removeElements([true, false], true, true)       = [false]
     * ArrayUtils.removeElements([true, false, true], true)       = [false, true]
     * ArrayUtils.removeElements([true, false, true], true, true) = [false]
     * </pre>
     *
     * @param array  the array to remove the element from, may be {@code null}
     * @param values the elements to be removed
     * @return A new array containing the existing elements except the
     * earliest-encountered occurrences of the specified elements.
     * @since 3.0.1
     */
    public static boolean[] removeElements(boolean[] array, boolean... values) {
        if (array.length == 0 || values.length == 0) return clone(array);
        Map<Boolean, MutableInt> occurrences = new HashMap<>(2);
        for (var v : values) {
            Boolean boxed = v;
            var count = occurrences.get(boxed);
            if (count == null) occurrences.put(boxed, new MutableInt(1));
            else count.increment();
        }
        var toRemove = new BitSet();
        for (var i = 0; i < array.length; i++) {
            var key = array[i];
            var count = occurrences.get(key);
            if (count != null) {
                if (count.decrementAndGet() == 0) occurrences.remove(key);
                toRemove.set(i);
            }
        }
        return (boolean[]) removeAll(array, toRemove);
    }

    /**
     * Removes multiple array elements specified by index.
     *
     * @param array   source
     * @param indices to remove
     * @return new array of same type minus elements specified by unique values of {@code indices}
     * @since 3.0.1
     */

    @Deprecated
    static Object removeAll(Object array, int... indices) {
        var length = getLength(array);
        if (length == 0)
            return array;

        var clonedIndices = clone(indices);
        Arrays.sort(clonedIndices);


        var diff = 0;
        {
            var i = clonedIndices.length;
            var prevIndex = length;
            while (--i >= 0) {
                var index = clonedIndices[i];
                if (index < 0 || index >= length)
                    throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
                if (index >= prevIndex) continue;
                diff++;
                prevIndex = index;
            }
        }


        var result = Array.newInstance(array.getClass().getComponentType(), length - diff);
        if (diff < length) {
            var end = length;
            var dest = length - diff;
            for (var i = clonedIndices.length - 1; i >= 0; i--) {
                var index = clonedIndices[i];
                if (end - index > 1) {
                    var cp = end - index - 1;
                    dest -= cp;
                    System.arraycopy(array, index + 1, result, dest, cp);

                }
                end = index;
            }
            if (end > 0) System.arraycopy(array, 0, result, 0, end);
        }
        return result;
    }

//    /**
//     * Removes the occurrences of the specified element from the specified array.
//     *
//     * <p>
//     * All subsequent elements are shifted to the left (subtracts one from their indices).
//     * If the array doesn't contains such an element, no elements are removed from the array.
//     * <code>null</code> will be returned if the input array is <code>null</code>.
//     * </p>
//     *
//     * @param <T>     the type of object in the array
//     * @param element the element to remove
//     * @param array   the input array
//     * @return A new array containing the existing elements except the occurrences of the specified element.
//     * @since 3.5
//     */
//    public static <T> T[] removeAllOccurences(final T[] array, final T element) {
//        int index = indexOf(array, element);
//        if (index == INDEX_NOT_FOUND) {
//
//            return array;
//        }
//
//        final MetalBitSet indices = MetalBitSet.bits(array.length);
//        indices[0] = index;
//        int count = 1;
//
//        while ((index = indexOf(array, element, indices[count - 1] + 1)) != INDEX_NOT_FOUND) {
//            indices[count++] = index;
//        }
//
//        return removeAll(array, Arrays.copyOf(indices, count));
//    }

    /**
     * Removes multiple array elements specified by indices.
     *
     * @param array   source
     * @param indices to remove
     * @return new array of same type minus elements specified by the set bits in {@code indices}
     * @since 3.2
     */

    static Object removeAll(Object array, BitSet indices) {
        var srcLength = ArrayUtil.getLength(array);


        var removals = indices.cardinality();
        var result = Array.newInstance(array.getClass().getComponentType(), srcLength - removals);
        var srcIndex = 0;
        var destIndex = 0;
        int count;
        int set;
        while ((set = indices.nextSetBit(srcIndex)) != -1) {
            count = set - srcIndex;
            if (count > 0) {
                System.arraycopy(array, srcIndex, result, destIndex, count);
                destIndex += count;
            }
            srcIndex = indices.nextClearBit(set);
        }
        count = srcLength - srcIndex;
        if (count > 0) System.arraycopy(array, srcIndex, result, destIndex, count);
        return result;
    }

    public static byte[] removeAll(byte[] x, MetalBitSet indices) {
        var toRemove = indices.cardinality();
        var srcLength = x.length;
        if (toRemove == 0)
            return x;
        var remain = srcLength - toRemove;
        switch (remain) {
            case 0:
                return ArrayUtil.EMPTY_BYTE_ARRAY;
            case 1:
                return new byte[]{x[indices.next(false, 0, srcLength)]};
            default:
                var y = new byte[remain];
                var j = 0;
                for (var i = 0; i < srcLength; i++)
                    if (!indices.get(i))
                        y[j++] = x[i];
                return y;
        }
    }

    public static <X> X[] removeAll(X[] x, MetalBitSet keep) {
        return removeAll(x, keep, false);
    }

    public static <X> X[] removeAll(X[] x, MetalBitSet indices, boolean iff) {
        var toRemove = indices.cardinality();
        if (toRemove == 0)
            return x;
        int srcLength = x.length, remain = srcLength - toRemove;
        switch (remain) {
            case 0:
                return (X[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
            case 1: {
                var y = Arrays.copyOf(x, 1);
                y[0] = x[indices.next(iff, 0, srcLength)];
                return y;
            }
            default: {
                var y = Arrays.copyOf(x, remain);
                var j = 0;
                for (var i = 0; i < srcLength; i++)
                    if (iff == indices.get(i))
                        y[j++] = x[i];
                return y;
            }
        }
    }

    public static int[] removeAll(int[] x, MetalBitSet indices) {
        var toRemove = indices.cardinality();
        var srcLength = x.length;
        if (toRemove == 0)
            return x;
        var remain = srcLength - toRemove;
        switch (remain) {
            case 0:
                return ArrayUtil.EMPTY_INT_ARRAY;
            case 1:
                return new int[]{x[indices.next(false, 0, srcLength)]};
            default:
                var y = new int[remain];
                var j = 0;
                for (var i = 0; i < srcLength; i++)
                    if (!indices.get(i))
                        y[j++] = x[i];
                return y;
        }
    }

    /**
     * <p>This method checks whether the provided array is sorted according to the class's
     * {@code compareTo} method.
     *
     * @param array the array to check
     * @param <T>   the datatype of the array to check, it must implement {@code Comparable}
     * @return whether the array is sorted
     * @since 3.4
     */
    public static <T extends Comparable<? super T>> boolean isSorted(T[] array) {
        return isSorted(array, Comparator.naturalOrder());
    }

    /**
     * <p>This method checks whether the provided array is sorted according to the provided {@code Comparator}.
     *
     * @param array      the array to check
     * @param comparator the {@code Comparator} to compare over
     * @param <T>        the datatype of the array
     * @return whether the array is sorted
     * @since 3.4
     */
    public static <T> boolean isSorted(T[] array, Comparator<T> comparator) {
        if (comparator == null) throw new IllegalArgumentException("Comparator should not be null.");

        if (array == null || array.length < 2) return true;

        var previous = array[0];
        var n = array.length;
        for (var i = 1; i < n; i++) {
            var current = array[i];
            if (comparator.compare(previous, current) > 0) return false;

            previous = current;
        }
        return true;
    }

    /**
     * <p>This method checks whether the provided array is sorted according to natural ordering.
     *
     * @param array the array to check
     * @return whether the array is sorted according to natural ordering
     * @since 3.4
     */
    public static boolean isSorted(double[] array) {
        if (array == null || array.length < 2) return true;

        var previous = array[0];
        var n = array.length;
        for (var i = 1; i < n; i++) {
            var current = array[i];
            if (previous > current)
                return false;
            previous = current;
        }
        return true;
    }

    /**
     * <p>This method checks whether the provided array is sorted according to natural ordering.
     *
     * @param array the array to check
     * @return whether the array is sorted according to natural ordering
     * @since 3.4
     */
    public static boolean isSorted(float[] array) {
        if (array == null || array.length < 2)
            return true;

        var n = array.length;
        var previous = array[0];
        for (var i = 1; i < n; i++) {
            var current = array[i];
            if (previous > current)
                return false;
            previous = current;
        }
        return true;
    }

    public static boolean isSorted(long[] array) {
        if (array == null || array.length < 2)
            return true;

        var n = array.length;
        var previous = array[0];
        for (var i = 1; i < n; i++) {
            var current = array[i];
            if (previous > current)
                return false;
            previous = current;
        }
        return true;
    }

    /**
     * Removes the occurrences of the specified element from the specified byte array.
     *
     * <p>
     * All subsequent elements are shifted to the left (subtracts one from their indices).
     * If the array doesn't contains such an element, no elements are removed from the array.
     * <code>null</code> will be returned if the input array is <code>null</code>.
     * </p>
     *
     * @param element the element to remove
     * @param array   the input array
     * @return the original array if element not found, or
     * A new array containing the existing elements except the occurrences of the specified element.
     * @since 3.5
     */
    public static byte[] removeFirstOccurence(byte[] array, byte element) {
        switch (array.length) {
            case 0:
                return ArrayUtil.EMPTY_BYTE_ARRAY;
            case 1:
                if (array[0] == element) return ArrayUtil.EMPTY_BYTE_ARRAY;
                break;
            case 2:
                if (array[0] == element) return new byte[]{array[1]};
                if (array[1] == element) return new byte[]{array[0]};
                break;
            default:
                var index = indexOf(array, element);
                if (index != -1)
                    return remove(array, index);
                break;
        }
        return array;
    }


    public static <T> T[] removeNulls(T[] array) {
        if (array.length == 0)
            return array;
        var count = Arrays.stream(array).filter(Objects::isNull).count();
        var nulls = (int) count;
        if (nulls == 0)
            return array;
        else
            return removeNulls(array, nulls);
    }

    public static @NotNull <T> T[] removeNulls(T[] array, int nulls) {
        var s = array.length - nulls;
        var a = Arrays.copyOf(array, s);
        if (s > 0) {
            var j = 0;
            for (var x : array)
                if (x != null)
                    a[j++] = x;
        }
        return a;
    }

    /**
     * <p>Returns an array containing the string representation of each element in the argument array.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array the {@code Object[]} to be processed, may be null
     * @return {@code String[]} of the same size as the source with its element's string representation,
     * {@code null} if null array input
     * @throws NullPointerException if array contains {@code null}
     * @since 3.6
     */
    public static String[] toStringArray(Object[] array) {
        if (array == null) return null;
        if (array.length == 0) return EMPTY_STRING_ARRAY;

        return Arrays.stream(array).map(Object::toString).toArray(String[]::new);
    }

    /**
     * <p>Returns an array containing the string representation of each element in the argument
     * array handling {@code null} elements.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array                the Object[] to be processed, may be null
     * @param valueForNullElements the value to insert if {@code null} is found
     * @return a {@code String} array, {@code null} if null array input
     * @since 3.6
     */
    public static String[] toStringArray(Object[] array, String valueForNullElements) {
        if (null == array) return null;
        if (array.length == 0) return EMPTY_STRING_ARRAY;

        var result = new String[array.length];
        for (var i = 0; i < array.length; i++) {
            var object = array[i];
            result[i] = (object == null ? valueForNullElements : object.toString());
        }

        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static boolean[] insert(int index, boolean[] array, boolean... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new boolean[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static byte[] insert(int index, byte[] array, byte... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new byte[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static char[] insert(int index, char[] array, char... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new char[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static double[] insert(int index, double[] array, double... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new double[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static float[] insert(int index, float[] array, float... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new float[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static int[] insert(int index, int[] array, int... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new int[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static long[] insert(int index, long[] array, long... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new long[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    public static short[] insert(int index, short[] array, short... values) {
        if (array == null) return null;
        if (values == null || values.length == 0) return clone(array);
        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);

        var result = new short[array.length + values.length];

        System.arraycopy(values, 0, result, index, values.length);
        if (index > 0) System.arraycopy(array, 0, result, 0, index);
        if (index < array.length) System.arraycopy(array, index, result, index + values.length, array.length - index);
        return result;
    }

    /**
     * <p>Inserts elements into an array at the given index (starting from zero).</p>
     *
     * <p>When an array is returned, it is always a new array.</p>
     *
     * <pre>
     * ArrayUtils.insert(index, null, null)      = null
     * ArrayUtils.insert(index, array, null)     = cloned copy of 'array'
     * ArrayUtils.insert(index, null, values)    = null
     * </pre>
     *
     * @param <T>    The type of elements in {@code array} and {@code values}
     * @param index  the position within {@code array} to insert the new values
     * @param array  the array to insert the values into, may be {@code null}
     * @param values the new values to insert, may be {@code null}
     * @return The new array.
     * @throws IndexOutOfBoundsException if {@code array} is provided
     *                                   and either {@code index < 0} or {@code index > array.length}
     * @since 3.6
     */
    @SafeVarargs
    public static <T> T[] insert(int index, T[] array, T... values) {
        /*
         * Note on use of @SafeVarargs:
         *
         * By returning null when 'array' is null, we avoid returning the vararg
         * array to the caller. We also avoid relying on the type of the vararg
         * array, by inspecting the component type of 'array'.
         */

        if (array == null)
            return null;

        if (values == null || values.length == 0)
            return clone(array);

        if (index < 0 || index > array.length)
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);


        //final Class<?> type = array.getClass().getComponentType();
        @SuppressWarnings("unchecked")
        //T[]) Array.newInstance(type,
                var result = Arrays.copyOf(array, array.length + values.length);

        System.arraycopy(values, 0, result, index, values.length);

        if (index > 0)
            System.arraycopy(array, 0, result, 0, index);

        if (index < array.length)
            System.arraycopy(array, index, result, index + values.length, array.length - index);

        return result;
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(Object[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapObj(array, i - 1, random.nextInt(i));
    }

    /**
     * from,to is an inclusive range which is non-standard wrt the other shuffle methods
     */
    public static void shuffle(int from, int to, Random random, IntIntProcedure swapper) {
        var range = 1 + (to - from);
        for (var i = to; i > from; i--) {
            var a = i - 1;
            var b = random.nextInt(range) + from;
            if (a != b)
                swapper.value(a, b);
        }
    }

    public static void shuffle(Object[] array, Rand random) {
        for (var i = array.length; i > 1; i--)
            swapObj(array, i - 1, random.nextInt(i));
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(boolean[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapBool(array, i - 1, random.nextInt(i));
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(byte[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapByte(array, i - 1, random.nextInt(i));
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(char[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swap(array, i - 1, random.nextInt(i), 1);
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(short[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapShort(array, i - 1, random.nextInt(i));
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(long[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapLong(array, i - 1, random.nextInt(i));
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(float[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapFloat(array, i - 1, random.nextInt(i));
    }

    /**
     * Randomly permutes the elements of the specified array using the Fisher-Yates algorithm.
     *
     * @param array  the array to shuffle
     * @param random the source of randomness used to permute the elements
     * @see <a href="https:
     * @since 3.6
     */
    public static void shuffle(double[] array, Random random) {
        for (var i = array.length; i > 1; i--)
            swapDouble(array, i - 1, random.nextInt(i));
    }

    /**
     * Ensures that a range given by its first (inclusive) and last (exclusive) elements fits an array of given length.
     * <p>
     * <P>This method may be used whenever an array range check is needed.
     *
     * @param arrayLength an array length.
     * @param from        a start index (inclusive).
     * @param to          an end index (inclusive).
     * @throws IllegalArgumentException       if <code>from</code> is greater than <code>to</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>from</code> or <code>to</code> are greater than <code>arrayLength</code> or negative.
     */
    public static void ensureFromTo(int arrayLength, int from, int to) {
        if (from < 0) throw new ArrayIndexOutOfBoundsException("Start index (" + from + ") is negative");
        if (from > to)
            throw new IllegalArgumentException("Start index (" + from + ") is greater than end index (" + to + ')');
        if (to > arrayLength)
            throw new ArrayIndexOutOfBoundsException("End index (" + to + ") is greater than array length (" + arrayLength + ')');
    }

    /**
     * Ensures that a range given by an offset and a length fits an array of given length.
     * <p>
     * <P>This method may be used whenever an array range check is needed.
     *
     * @param arrayLength an array length.
     * @param offset      a start index for the fragment
     * @param length      a length (the number of elements in the fragment).
     * @throws IllegalArgumentException       if <code>length</code> is negative.
     * @throws ArrayIndexOutOfBoundsException if <code>offset</code> is negative or <code>offset</code>+<code>length</code> is greater than <code>arrayLength</code>.
     */
    public static void ensureOffsetLength(int arrayLength, int offset, int length) {
        if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
        if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
        if (offset + length > arrayLength)
            throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + arrayLength + ')');
    }

    /**
     * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
     * <code>[first, middle)</code> and <code>[middle, last)</code>, and the resulting range is
     * <code>[first, last)</code>. Elements in the first input range will precede equal elements in
     * the second.
     */
    private static void inPlaceMerge(int from, int mid, int to, IntComparator comp, IntIntProcedure swapper) {
        if (from >= mid || mid >= to) return;
        if (to - from == 2) {
            if (comp.compare(mid, from) < 0) swapper.value(from, mid);
            return;
        }
        int firstCut;
        int secondCut;
        if (mid - from > to - mid) {
            firstCut = from + (mid - from) / 2;
            secondCut = lowerBound(mid, to, firstCut, comp);
        } else {
            secondCut = mid + (to - mid) / 2;
            firstCut = upperBound(from, mid, secondCut, comp);
        }

        var first2 = firstCut;
        var middle2 = mid;
        var last2 = secondCut;
        if (middle2 != first2 && middle2 != last2) {
            var first1 = first2;
            var last1 = middle2;
            while (first1 < --last1)
                swapper.value(first1++, last1);
            first1 = middle2;
            last1 = last2;
            while (first1 < --last1)
                swapper.value(first1++, last1);
            first1 = first2;
            last1 = last2;
            while (first1 < --last1)
                swapper.value(first1++, last1);
        }

        mid = firstCut + (secondCut - mid);
        inPlaceMerge(from, firstCut, mid, comp, swapper);
        inPlaceMerge(mid, secondCut, to, comp, swapper);
    }

    /**
     * Performs a binary search on an already-sorted range: finds the first position where an
     * element can be inserted without violating the ordering. Sorting is by a user-supplied
     * comparison function.
     *
     * @param mid      Beginning of the range.
     * @param to       One past the end of the range.
     * @param firstCut Element to be searched for.
     * @param comp     Comparison function.
     * @return The largest index i such that, for every j in the range <code>[first, i)</code>,
     * <code>comp.apply(array[j], x)</code> is <code>true</code>.
     */
    private static int lowerBound(int mid, int to, int firstCut, IntComparator comp) {

        var len = to - mid;
        while (len > 0) {
            var half = len / 2;
            var middle = mid + half;
            if (comp.compare(middle, firstCut) < 0) {
                mid = middle + 1;
                len -= half + 1;
            } else len = half;
        }
        return mid;
    }


    /**
     * Sorts the specified range of elements using the specified swapper and according to the order induced by the specified
     * comparator using mergesort.
     * <p>
     * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
     * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
     * standard mergesort, but does not allocate additional memory.
     *
     * @param from    the index of the first element (inclusive) to be sorted.
     * @param to      the index of the last element (exclusive) to be sorted.
     * @param c       the comparator to determine the order of the generic data (arguments are positions).
     * @param swapper an object that knows how to swap the elements at any two positions.
     */
    public static void mergeSort(int from, int to, IntComparator c, IntIntProcedure swapper) {
        /*
         * We retain the same method signature as quickSort. Given only a comparator and swapper we
         * do not know how to copy and move elements from/to temporary arrays. Hence, in contrast to
         * the JDK mergesorts this is an "in-place" mergesort, i.e. does not allocate any temporary
         * arrays. A non-inplace mergesort would perhaps be faster in most cases, but would require
         * non-intuitive delegate objects...
         */
        var length = to - from;


        if (length < QuickSort.SMALL) {
            for (var i = from; i < to; i++)
                for (var j = i; j > from && (c.compare(j - 1, j) > 0); j--) swapper.value(j, j - 1);
            return;
        }


        var mid = (from + to) >>> 1;
        mergeSort(from, mid, c, swapper);
        mergeSort(mid, to, c, swapper);


        if (c.compare(mid - 1, mid) <= 0) return;


        inPlaceMerge(from, mid, to, c, swapper);
    }

    /**
     * Performs a binary search on an already sorted range: finds the last position where an element
     * can be inserted without violating the ordering. Sorting is by a user-supplied comparison
     * function.
     *
     * @param from      Beginning of the range.
     * @param mid       One past the end of the range.
     * @param secondCut Element to be searched for.
     * @param comp      Comparison function.
     * @return The largest index i such that, for every j in the range <code>[first, i)</code>,
     * <code>comp.apply(x, array[j])</code> is <code>false</code>.
     */
    private static int upperBound(int from, int mid, int secondCut, IntComparator comp) {

        var len = mid - from;
        while (len > 0) {
            var half = len / 2;
            var middle = from + half;
            if (comp.compare(secondCut, middle) < 0) len = half;
            else {
                from = middle + 1;
                len -= half + 1;
            }
        }
        return from;
    }


    public static void shuffle(int[] array, Random random) {
        shuffle(array, array.length, random);
    }

    public static void shuffle(int[] array, int len, Random random) {
        for (var i = len; i > 1; i--)
            swapInt(array, i - 1, random.nextInt(i));
    }

    public static void shuffle(byte[] array, int len, Random random) {
        for (var i = len; i > 1; i--)
            swapByte(array, i - 1, random.nextInt(i));
    }


    /**
     * modifies input
     */
    public static int[] removeDuplicates(int[] tt) {
        if (tt.length > 1) if (tt.length == 2) {
            if (tt[0] == tt[1])
                return new int[]{tt[0]};
        } else {

            Arrays.sort(tt);

            MetalBitSet m = null;
            for (int i = 1, ttLength = tt.length; i < ttLength; i++)
                if (tt[i] == tt[i - 1]) {
                    if (m == null) m = MetalBitSet.bits(ttLength);
                    m.set(i);
                }
            if (m != null)
                return removeAll(tt, m);
        }
        return tt;
    }

    public static int nextIndexOf(byte[] array, int startingAt, int endingAt, byte[] target, int targetFrom, int targetTo) {
        var targetLen = targetTo - targetFrom;
        assert (targetLen > 0);

        if (endingAt - startingAt < targetLen)
            return -1;

        outer:
        for (var i = startingAt; i < endingAt - targetLen + 1; i++) {
            for (var j = 0; j < targetLen; j++) if (array[i + j] != target[targetFrom + j]) continue outer;
            return i;
        }
        return -1;


    }

    /**
     * creates a never-ending (until empty) cyclic ListIterator for a given List
     * adapted from Guava's Iterators.cycle(...)
     */
    public static <T> ListIterator<T> cycle(List<T> l) {

        return new ForwardingListIterator<>() {

            ListIterator<T> i = l.listIterator();

            @Override
            protected final ListIterator<T> delegate() {
                return i;
            }

            @Override
            public boolean hasNext() {
                if (!i.hasNext()) {
                    if (l.isEmpty())
                        return false;
                    i = l.listIterator();
                }

                return true;
            }
        };
    }

    /**
     * simple byte[] interner for low-count elements
     */
    public static byte[] intern(byte[] map) {
        var result = map;
        var finished = false;
        switch (map.length) {
            case 0:
                result = ArrayUtil.EMPTY_BYTE_ARRAY;
                break;
            case 1: {
                switch (map[0]) {
                    case 0:
                        result = BYTE_ZERO;
                        finished = true;
                        break;
                    case 1:
                        result = BYTE_ONE;
                        finished = true;
                        break;
                    case 2:
                        result = BYTE_TWO;
                        finished = true;
                        break;
                    case 3:
                        result = BYTE_THREE;
                        finished = true;
                        break;
                }
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
            }
            case 2: {
                switch (map[0]) {
                    case 0:
                        switch (map[1]) {
                            case 0:
                                result = BYTE_ZERO_ZERO;
                                finished = true;
                                break;
                            case 1:
                                result = BYTE_ZERO_ONE;
                                finished = true;
                                break;
                        }
                        if (finished) break;
                        if (finished) break;
                    case 1:
                        switch (map[1]) {
                            case 0:
                                result = BYTE_ONE_ZERO;
                                finished = true;
                                break;
                            case 1:
                                result = BYTE_ONE_ONE;
                                finished = true;
                                break;
                            case 2:
                                result = BYTE_ONE_TWO;
                                finished = true;
                                break;
                            case -1:
                                result = BYTE_ONE_NEGONE;
                                finished = true;
                                break;
                            case -2:
                                result = BYTE_ONE_NEGTWO;
                                finished = true;
                                break;
                        }
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                    case -1:
                        switch (map[1]) {
                            case 1:
                                result = BYTE_NEGONE_ONE;
                                finished = true;
                                break;
                            case 2:
                                result = BYTE_NEGONE_TWO;
                                finished = true;
                                break;
                            case -1:
                                result = BYTE_NEGONE_NEGONE;
                                finished = true;
                                break;
                            case -2:
                                result = BYTE_NEGONE_NEGTWO;
                                finished = true;
                                break;
                        }
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                    case 2:
                        switch (map[1]) {
                            case 1:
                                result = BYTE_TWO_ONE;
                                finished = true;
                                break;
                            case 2:
                                result = BYTE_TWO_TWO;
                                finished = true;
                                break;
                            case -1:
                                result = BYTE_TWO_NEGONE;
                                finished = true;
                                break;
                            case -2:
                                result = BYTE_TWO_NEGTWO;
                                finished = true;
                                break;
                        }
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                        if (finished) break;
                }
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
                if (finished) break;
            }
        }

        return result;
    }

    public static boolean equalsIdentity(Object[] x, Object[] y) {
        if (Arrays.equals(x, y)) return true;
        if (x.length != y.length) return false;
        return IntStream.range(0, x.length).noneMatch(i -> x[i] != y[i]);
    }

    public static short[] toShort(int[] x) {
        if (x.length == 0)
            return EMPTY_SHORT_ARRAY;

        var s = new short[x.length];
        var i = 0;
        for (var xx : x) {
            assert (xx <= Short.MAX_VALUE && xx >= Short.MIN_VALUE);
            s[i++] = (short) xx;
        }
        return s;
    }

    public static boolean containsIdentity(Object[] xx, Object x) {
        return indexOfInstance(xx, x) != -1;
    }

    /**
     * doesnt do null tests
     */
    public static boolean equalArraysDirect(Object[] a, Object[] b) {
        var len = a.length;
        if (b.length != len)
            return false;

        return IntStream.range(0, len).allMatch(i -> a[i].equals(b[i]));
    }
}