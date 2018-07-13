/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http:
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package jcog.data.pool;

import jcog.data.list.FasterList;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Object pool for arrays.
 *
 * @author jezek2
 */
public class ArrayPool<T> extends FasterList {

    private static final ThreadLocal<ArrayPool<byte[]>> bytes =
            ThreadLocal.withInitial(() -> new ArrayPool(byte[].class));
    private static final ThreadLocal<ArrayPool<short[]>> shorts =
            ThreadLocal.withInitial(() -> new ArrayPool(short[].class));

    private static final ThreadLocal<Map<Class, ArrayPool>> typed = ThreadLocal.withInitial(HashMap::new);

    private final Class componentType;
    private final boolean primitive;

    /**
     * Returns per-thread array pool for given type, or create one if it doesn't exist.
     *
     * @param cls type
     * @return object pool
     */
    public static ArrayPool the(Class cls) {
        return typed.get().computeIfAbsent(cls, ArrayPool::new);
    }

    public static void cleanCurrentThread() {
        typed.remove();
    }

    public static ArrayPool<byte[]> bytes() {
        return bytes.get();
    }
    public static ArrayPool<short[]> shorts() {
        return shorts.get();
    }

    @Deprecated
    abstract static class ArrayLengthMatcher implements Comparator {
        public int value;

        @Override
        public int compare(Object o1, Object o2) {
            boolean isOne = (o1 == this);
            int len1 = isOne ? value : len(o1);
            int len2 = !isOne ? value : len(o2);
            return Integer.compare(len1, len2);
        }

        abstract int len(Object o);
    }

    static class ObjectArrayLengthMatcher extends ArrayLengthMatcher {

        @Override
        int len(Object o) {
            return ((Object[]) o).length;
        }
    }

    private final ArrayLengthMatcher comparator;

    /**
     * Creates object pool.
     *
     * @param arrayType
     */
    public ArrayPool(Class arrayType) {

        this.componentType = arrayType.getComponentType();

        this.primitive = componentType.isPrimitive();
        if (this.primitive) {
            if (componentType == byte.class) {
                comparator = new ByteArrayLengthMatcher();
            } else if (componentType == short.class) {
                comparator = new ShortArrayLengthMatcher();
            } else {
                throw new UnsupportedOperationException("TODO " + componentType);
            }
        } else {
            comparator = new ObjectArrayLengthMatcher();
        }














    }

    @SuppressWarnings("unchecked")
    private T create(int length) {
        return (T) Array.newInstance(componentType, length);
    }

    /**
     * Returns array of exactly the same length as demanded, or create one if not
     * present in the pool.
     *
     * @param length
     * @return array
     */
    @SuppressWarnings("unchecked")
    public T getExact(int length) {
        comparator.value = length;
        int index = Collections.binarySearch(this, comparator, comparator);
        if (index < 0) {
            return create(length);
        }
        return (T) remove(index);
    }

    /**
     * Returns array that has same or greater length, or create one if not present
     * in the pool.
     *
     * @param minLength the minimum length required
     * @return array
     */
    @SuppressWarnings("unchecked")
    public T getMin(int minLength) {
        comparator.value = minLength;
        int index = Collections.binarySearch(this, comparator, comparator);
        if (index < 0) {
            index = -index - 1;
            return index < size ? (T) remove(index) : create(minLength);
        }
        return (T) remove(index);
    }

    /**
     * Releases array into object pool.
     *
     * @param array previously obtained array from this pool
     */
    @SuppressWarnings("unchecked")
    public void release(T array) {
        int index = Collections.binarySearch(this, array, comparator);
        if (index < 0) index = -index - 1;
        add(index, array);

        
        if (!primitive) {
            Object[] objArray = (Object[]) array;
            Arrays.fill(objArray, null);
        }
    }


    private static class ByteArrayLengthMatcher extends ArrayLengthMatcher {
        @Override final int len(Object o) {
            return ((byte[])o).length;
        }
    }
    private static class ShortArrayLengthMatcher extends ArrayLengthMatcher {
        @Override final int len(Object o) {
            return ((short[])o).length;
        }
    }
}
