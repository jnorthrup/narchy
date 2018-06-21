/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
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

package com.metamx.collections.bitmap;

import com.google.common.collect.Sets;
import com.metamx.collections.IntSetTestUtility;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.IntIterator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class WrappedBitSetBitmapBitSetTest {

    private static final WrappedBitSetBitmap defaultBitSet() {
        return new WrappedBitSetBitmap(IntSetTestUtility.createSimpleBitSet(IntSetTestUtility.getSetBits()));
    }

    @Test
    void testIterator() {
        WrappedBitSetBitmap bitSet = new WrappedBitSetBitmap();
        for (int i : IntSetTestUtility.getSetBits()) {
            bitSet.add(i);
        }
        IntIterator intIt = bitSet.iterator();
        for (int i : IntSetTestUtility.getSetBits()) {
            assertTrue(intIt.hasNext());
            assertEquals(i, intIt.next());
        }
    }

    @Test
    void testSize() {
        BitSet bitSet = IntSetTestUtility.createSimpleBitSet(IntSetTestUtility.getSetBits());
        WrappedBitSetBitmap wrappedBitSetBitmapBitSet = new WrappedBitSetBitmap(bitSet);
        assertEquals(bitSet.cardinality(), wrappedBitSetBitmapBitSet.size());
    }

    @Test
    void testOffHeap() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(Long.SIZE * 100 / 8).order(ByteOrder.LITTLE_ENDIAN);
        BitSet testSet = BitSet.valueOf(buffer);
        testSet.set(1);
        WrappedImmutableBitSetBitmap bitMap = new WrappedImmutableBitSetBitmap(testSet);
        assertTrue(bitMap.get(1));
        testSet.set(2);
        assertTrue(bitMap.get(2));
    }

    @Test
    void testSimpleBitSet() {
        WrappedBitSetBitmap bitSet = new WrappedBitSetBitmap(IntSetTestUtility.createSimpleBitSet(IntSetTestUtility.getSetBits()));
        assertTrue(IntSetTestUtility.equalSets(IntSetTestUtility.getSetBits(), bitSet));
    }

    @Test
    void testUnion() {
        WrappedBitSetBitmap bitSet = new WrappedBitSetBitmap(IntSetTestUtility.createSimpleBitSet(IntSetTestUtility.getSetBits()));

        Set<Integer> extraBits = Sets.newHashSet(6, 9);
        WrappedBitSetBitmap bitExtraSet = new WrappedBitSetBitmap(IntSetTestUtility.createSimpleBitSet(extraBits));

        Set<Integer> union = Sets.union(extraBits, IntSetTestUtility.getSetBits());

        assertTrue(IntSetTestUtility.equalSets(union, bitSet.union(bitExtraSet)));
    }

    @Test
    void testIntersection() {
        WrappedBitSetBitmap bitSet = new WrappedBitSetBitmap(IntSetTestUtility.createSimpleBitSet(IntSetTestUtility.getSetBits()));

        Set<Integer> extraBits = Sets.newHashSet(1, 2, 3, 4, 5, 6, 7, 8);
        WrappedBitSetBitmap bitExtraSet = new WrappedBitSetBitmap(IntSetTestUtility.createSimpleBitSet(extraBits));

        Set<Integer> intersection = Sets.intersection(extraBits, IntSetTestUtility.getSetBits());

        assertTrue(IntSetTestUtility.equalSets(intersection, bitSet.intersection(bitExtraSet)));
    }

    @Test
    void testAnd() {
        WrappedBitSetBitmap bitSet = defaultBitSet();
        WrappedBitSetBitmap bitSet2 = defaultBitSet();
        Set<Integer> defaultBitSet = IntSetTestUtility.getSetBits();
        bitSet.remove(1);
        bitSet2.remove(2);

        bitSet.and(bitSet2);

        defaultBitSet.remove(1);
        defaultBitSet.remove(2);

        assertTrue(IntSetTestUtility.equalSets(defaultBitSet, bitSet));
    }


    @Test
    void testOr() {
        WrappedBitSetBitmap bitSet = defaultBitSet();
        WrappedBitSetBitmap bitSet2 = defaultBitSet();
        Set<Integer> defaultBitSet = IntSetTestUtility.getSetBits();
        bitSet.remove(1);
        bitSet2.remove(2);

        bitSet.or(bitSet2);

        assertTrue(IntSetTestUtility.equalSets(defaultBitSet, bitSet));
    }

    @Test
    void testAndNot() {
        WrappedBitSetBitmap bitSet = defaultBitSet();
        WrappedBitSetBitmap bitSet2 = defaultBitSet();
        Set<Integer> defaultBitSet = Sets.newHashSet();
        bitSet.remove(1);
        bitSet2.remove(2);

        bitSet.andNot(bitSet2);

        defaultBitSet.add(2);

        assertTrue(IntSetTestUtility.equalSets(defaultBitSet, bitSet));
    }


    @Test
    void testSerialize() {
        WrappedBitSetBitmap bitSet = defaultBitSet();
        Set<Integer> defaultBitSet = IntSetTestUtility.getSetBits();
        byte[] buffer = new byte[bitSet.getSizeInBytes()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        bitSet.serialize(byteBuffer);
    }
}
