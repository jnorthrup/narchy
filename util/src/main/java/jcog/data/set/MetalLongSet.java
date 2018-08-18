package jcog.data.set;


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * adapted from apache lucene SentinelIntSet
 */
public class MetalLongSet {

    /**
     * A power-of-2 over-sized array holding the integers in the set along with empty values.
     */
    public long[] keys;
    public int size;
    public final long emptyVal;

    /**
     * the count at which a rehash should be done
     */
    public int rehashCount;

    /**
     * @param size     The minimum number of elements this set should be able to hold without rehashing
     *                 (i.e. the slots are guaranteed not to change)
     * @param emptyVal The integer value to use for EMPTY
     */
    public MetalLongSet(int size) {
        this.emptyVal = emptyVal();
        int tsize = Math.max(org.apache.lucene.util.BitUtil.nextHighestPowerOfTwo(size), 1);
        rehashCount = tsize - (tsize >> 2);
        if (size >= rehashCount) {  // should be able to hold "size" w/o re-hashing
            tsize <<= 1;
            rehashCount = tsize - (tsize >> 2);
        }
        keys = new long[tsize];
        if (emptyVal != 0)
            clear();
    }

    public MetalLongSet(long[] x) {
        this(x.length);
        addAll(x);
    }

    protected long emptyVal() {
        return Long.MIN_VALUE;
    }

    public void clear() {
        Arrays.fill(keys, emptyVal);
        size = 0;
    }

    /**
     * (internal) Return the hash for the key. The default implementation just returns the key,
     * which is not appropriate for general purpose use.
     */
    protected int hash(long key) {
        return Long.hashCode(key);
    }

    /**
     * The number of integers in this set.
     */
    public final int size() {
        return size;
    }

    /**
     * (internal) Returns the slot for this key
     */
    private int slot(long key) {

        int h = hash(key);
        long[] k = this.keys;

        int n = k.length;

        int s = h & (n - 1);
        long ks = k[s];
        if (ks == key || ks == emptyVal) return s;

        int increment = (h >> 7) | 1;
        do {
            ks = k[(s = (s + increment) & (n - 1))];
        } while (ks != key && ks != emptyVal);
        return s;
    }

    /**
     * (internal) Returns the slot for this key, or -slot-1 if not found
     */
    private int find(long key) {

        int h = hash(key);
        long[] k = this.keys;
        int n = k.length;

        int s = h & (n - 1);
        long ks = k[s];
        if (ks == emptyVal) return -s - 1;
        if (ks == key) return s;

        int increment = (h >> 7) | 1;
        for (; ; ) {
            s = (s + increment) & (n - 1);
            long kks = k[s];
            if (kks == emptyVal) return -s - 1;
            if (kks == key) return s;
        }
    }

    /**
     * Does this set contain the specified integer?
     */
    public boolean contains(long key) {
        return size() > 0 && find(key) >= 0;
    }

    public final boolean add(long x) {
        if (x == emptyVal)
            throw new UnsupportedOperationException();
        int i = find(x);
        if (i >= 0)
            return false;
        else {
            put(i, x);
            return true;
        }
    }
    public boolean remove(long x) {
        int s = find(x);
        if (s >= 0) {
            keys[s] = emptyVal;
            return true;
        }
        return false;
    }

    /**
     * Puts this integer (key) in the set, and returns the slot index it was added to.
     * It rehashes if adding it would make the set more than 75% full.
     */
    private int put(int s, long key) {
        if (++size >= rehashCount) {
            rehash();
            s = slot(key);
        } else {
            s = -s - 1;
        }
        keys[s] = key;
        return s;
    }

    /**
     * (internal) Rehashes by doubling {@code long[] key} and filling with the old values.
     */
    public void rehash() {
        int newSize = keys.length << 1;
        long[] oldKeys = keys;
        keys = new long[newSize];
        if (emptyVal != 0) Arrays.fill(keys, emptyVal);

        for (long key : oldKeys) {
            if (key != emptyVal)
                keys[slot(key)] = key;
        }
        rehashCount = newSize - (newSize >> 2);
    }

    public long[] toArray() {
        return toArray(null);
    }

    public long[] toArray(@Nullable long[] l) {
        int s = size();
        if (s == 0)
            return ArrayUtils.EMPTY_LONG_ARRAY;
        if (l == null || l.length < s)
            l = new long[s];
        int i = 0;
        for (long x : keys) {
            if (x != emptyVal)
                l[i++] = x;
        }
        return l;
    }

    public long[] toSortedArray() {
        return toSortedArray(null);
    }

    public long[] toSortedArray(@Nullable long[] l) {
        long[] x = toArray(l);
        if (x.length > 1) {
            Arrays.sort(x);
        }
        return x;
    }

    public void addAll(long[] x) {
        for (long i : x)
            add(i);
    }


//
//    /** Return the memory footprint of this class in bytes. */
//    public long ramBytesUsed() {
//        return RamUsageEstimator.alignObjectSize(
//                Integer.BYTES * 3
//                        + RamUsageEstimator.NUM_BYTES_OBJECT_REF)
//                + RamUsageEstimator.sizeOf(keys);
//    }
}
