/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http:
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jcog.data.map;

import com.google.common.collect.Lists;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Concurrent hash map
 *
 * Provides similar methods as a ConcurrentMap<K,V> but since it's an open hash map with linear probing, no node
 * allocations are required to store the values
 *
 * @param <V>
 * from: https:
 */
@SuppressWarnings("unchecked")
public class ConcurrentOpenHashMap<K, V> extends AbstractMap<K,V> {

    private static final Object DeletedKey = new Object();

    private static final int DefaultExpectedItems = 1024;
    private static final int DefaultConcurrencyLevel = Runtime.getRuntime().availableProcessors();

    private final Section<K, V>[] sections;

    public ConcurrentOpenHashMap() {
        this(DefaultExpectedItems);
    }

    public ConcurrentOpenHashMap(final int expectedItems) {
        this(expectedItems, DefaultConcurrencyLevel);
    }

    public ConcurrentOpenHashMap(final int expectedItems, final int concurrencyLevel) {
        this(expectedItems, concurrencyLevel, 2.0F /3f);
    }

    public ConcurrentOpenHashMap(final int expectedItems, final int concurrencyLevel, final float MapFillFactor) {
        checkArgument(expectedItems > 0);
        checkArgument(concurrencyLevel > 0);
        checkArgument(expectedItems >= concurrencyLevel);

        final int numSections = concurrencyLevel;
        final int perSectionExpectedItems = expectedItems / numSections;
        final float mapFillFactor;
        final int perSectionCapacity = (int) ((float) perSectionExpectedItems / (mapFillFactor = MapFillFactor));
        this.sections = (Section<K, V>[]) new Section[numSections];

        for (int i = 0; i < numSections; i++) {
            sections[i] = new Section<>(perSectionCapacity, MapFillFactor);
        }
    }

    public int size() {
        long size = 0L;
        for (final Section<K, V> s : sections) {
            final long opaque = (long) s.size.getOpaque();
            size += opaque;
        }
        if (size >= (long) Integer.MAX_VALUE)
            return Integer.MAX_VALUE-1;
        return (int) size;
    }

    public long capacity() {
        long capacity = 0L;
        for (final Section<K, V> s : sections) {
            final long l = (long) s.capacity;
            capacity += l;
        }
        return capacity;
    }

    public boolean isEmpty() {

        for (final Section<K, V> s : sections) {
            if (s.size.getOpaque() != 0) {
                return false;
            }
        }
        return true;
    }

    public V get(final Object key) {
        checkNotNull(key);
        final long h = hash(key);
        return section(h).get((K)key, (int) h);
    }

    public boolean containsKey(final Object key) {
        return get(key) != null;
    }

    public V put(final K key, final V value) {
        checkNotNull(key);
        checkNotNull(value);
        final long h = hash(key);
        return section(h).put(key, value, (int) h, false, null);
    }

    public V putIfAbsent(final K key, final V value) {
        checkNotNull(key);
        checkNotNull(value);
        final long h = hash(key);
        return section(h).put(key, value, (int) h, true, null);
    }

    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> provider) {
        checkNotNull(key);
        checkNotNull(provider);
        final long h = hash(key);
        return section(h).put(key, null, (int) h, true, provider);
    }

    public V remove(final Object key) {
        checkNotNull(key);
        final long h = hash(key);
        return section(h).remove((K)key, null, (int) h);
    }

    @Override public boolean remove(final Object key, final Object value) {
        checkNotNull(key);
        checkNotNull(value);
        final long h = hash(key);
        return section(h).remove((K)key, value, (int) h) != null;
    }

    private Section<K, V> section(final long hash) {

        final int sectionIdx = (int) (hash >>> 32) & (sections.length - 1);
        return sections[sectionIdx];
    }

    public void clear() {
        for (final Section<K, V> s : sections) {
            s.clear();
        }
    }

    public void forEach(final BiConsumer<? super K, ? super V> processor) {
        for (final Section<K, V> s : sections) {
            s.forEach(processor);
        }
    }

    /**
     * @return a new list of all keys (makes a copy)
     */
    public List<K> keys() {
        final List<K> keys = Lists.newArrayList();
        for (final Entry<K, V> entry : this.entrySet()) {
            final K key = entry.getKey();
            final V value = entry.getValue();
            keys.add(key);
        }
        return keys;
    }

    public V[] values(V[] target, final IntFunction<V[]> arrayBuilder) {
        final int s = size();
        if (s == 0)
            return (V[]) ArrayUtil.EMPTY_OBJECT_ARRAY;

        if (target == null || target.length!=s) {
            target = arrayBuilder.apply(s);
        }

        final int[] i = {0};
        final V[] t = target;
        for (final Entry<K, V> entry : this.entrySet()) {
            final K k = entry.getKey();
            final V v = entry.getValue();
            if (v != null) {
                if (i[0] < s)
                    t[i[0]++] = v;
            }
        }

        if (i[0] < s) {
            return Arrays.copyOf(t, i[0]); //dont leave suffix nulls; create new list
        } else {
            return t;
        }
    }

    public List<V> values() {
        final List<V> values = new FasterList(size());
        for (final Entry<K, V> entry : this.entrySet()) {
            final K key = entry.getKey();
            final V value = entry.getValue();
            values.add(value);
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new TODO();
    }


    @SuppressWarnings({"serial", "TooBroadScope"})
    private static final class Section<K, V> extends StampedLock {

        private final float MapFillFactor;
        private AtomicReferenceArray table;

        private int capacity;
        private final AtomicInteger size = new AtomicInteger();
        private final AtomicInteger usedBuckets = new AtomicInteger();
        private int resizeThreshold;

        Section(final int capacity, final float MapFillFactor) {
            this.capacity = alignToPowerOfTwo(capacity);
            this.table = new AtomicReferenceArray(2 * this.capacity);
            this.size.set(0);
            this.usedBuckets.set(0);
            this.resizeThreshold = (int) ((float) this.capacity * (this.MapFillFactor = MapFillFactor));
        }

        V get(final K key, final int keyHash) {
            boolean acquiredLock = false;
            int bucket = signSafeMod((long) keyHash, capacity);
            long stamp = tryOptimisticRead();

            try {
                while (true) {

                    AtomicReferenceArray table = this.table;
                    K storedKey = (K) table.getOpaque(bucket);
                    V storedValue = (V) table.getOpaque(bucket + 1);

                    if (!acquiredLock && validate(stamp)) {

                        if (key.equals(storedKey)) {
                            return storedValue;
                        } else if (storedKey == null) {
                            return null;
                        }

                    } else {


                        if (!acquiredLock) {
                            stamp = readLock();
                            acquiredLock = true;
                            table = this.table;

                            bucket = signSafeMod((long) keyHash, capacity);
                            storedKey = (K) table.getOpaque(bucket);
                            storedValue = (V) table.getOpaque(bucket + 1);
                        }

                        if (key.equals(storedKey)) {
                            return storedValue;
                        } else if (storedKey == null) {
                            return null;
                        }
                    }

                    bucket = (bucket + 2) & (table.length() - 1);
                }
            } finally {
                if (acquiredLock) {
                    unlockRead(stamp);
                }
            }
        }

        V put(final K key, V value, final int keyHash, final boolean onlyIfAbsent, final Function<? super K, ? extends V> valueProvider) {
            final long stamp = writeLock();
            int bucket = signSafeMod((long) keyHash, capacity);


            try {
                int firstDeletedKey = -1;
                while (true) {
                    final AtomicReferenceArray table = this.table;
                    final K storedKey = (K) table.getOpaque(bucket);
                    final V storedValue = (V) table.get(bucket + 1);

                    if (storedKey!=null && key.equals(storedKey)) {
                        if (!onlyIfAbsent) {
                            if (value!=storedValue)
                                table.set(bucket + 1, value);
                        }
                        return storedValue;
                    } else if (storedKey == null) {


                        if (firstDeletedKey != -1) {
                            bucket = firstDeletedKey;
                        } else {
                            usedBuckets.incrementAndGet();
                        }

                        if (value == null) {
                            value = valueProvider.apply(key);
                        }

                        size.incrementAndGet();
                        table.set(bucket, key);
                        table.set(bucket + 1, value);
                        return valueProvider != null ? value : null;
                    } else if (storedKey == DeletedKey) {

                        if (firstDeletedKey == -1) {
                            firstDeletedKey = bucket;
                        }
                    }

                    bucket = (bucket + 2) & (table.length() - 1);
                }
            } finally {
                if (usedBuckets.get() > resizeThreshold) {
                    try {
                        rehash();
                    } finally {
                        unlockWrite(stamp);
                    }
                } else {
                    unlockWrite(stamp);
                }
            }
        }

        private V remove(final K key, final Object value, final int keyHash) {
            final long stamp = writeLock();
            int bucket = signSafeMod((long) keyHash, capacity);

            try {
                while (true) {
                    final AtomicReferenceArray table = this.table;
                    final K storedKey = (K) table.getOpaque(bucket);
                    final V storedValue = (V) table.getOpaque(bucket + 1);
                    if (key.equals(storedKey)) {
                        if (value == null || value.equals(storedValue)) {
                            size.decrementAndGet();

                            final int nextInArray = (bucket + 2) & (table.length() - 1);
                            if (table.get(nextInArray) == null) {
                                table.set(bucket, null);
                                table.set(bucket + 1,  null);
                                usedBuckets.decrementAndGet();
                            } else {
                                table.set(bucket, DeletedKey);
                                table.set(bucket + 1, null);
                            }

                            return storedValue;
                        } else {
                            return null;
                        }
                    } else if (storedKey == null) {

                        return null;
                    }

                    bucket = (bucket + 2) & (table.length() - 1);
                }

            } finally {
                unlockWrite(stamp);
            }
        }

        void clear() {
            final long stamp = writeLock();

            try {
                final int l = table.length();
                for (int i = 0; i < l; i++)
                    table.set(i, null);
                this.size.set(0);
                this.usedBuckets.set(0);
            } finally {
                unlockWrite(stamp);
            }
        }

        //TODO whileEach

        public void forEach(final BiConsumer<? super K, ? super V> processor) {
            long stamp = tryOptimisticRead();

            AtomicReferenceArray table = this.table;
            boolean acquiredReadLock = false;

            try {


                if (!validate(stamp)) {

                    stamp = readLock();
                    acquiredReadLock = true;
                    table = this.table;
                }


                final int l = table.length();
                for (int bucket = 0; bucket < l; bucket += 2) {
                    K storedKey = (K) table.getOpaque(bucket);
                    V storedValue = (V) table.getOpaque(bucket + 1);

                    if (!acquiredReadLock && !validate(stamp)) {

                        stamp = readLock();
                        acquiredReadLock = true;

                        storedKey = (K) table.getOpaque(bucket);
                        storedValue = (V) table.getOpaque(bucket + 1);
                    }

                    if (storedKey != DeletedKey && storedKey != null) {
                        processor.accept(storedKey, storedValue);
                    }
                }
            } finally {
                if (acquiredReadLock) {
                    unlockRead(stamp);
                }
            }
        }

        private void rehash() {

            final int newCapacity = capacity * 2;
            final AtomicReferenceArray newTable = new AtomicReferenceArray(2 * newCapacity);


            final AtomicReferenceArray table = this.table;
            final int l = table.length();
            for (int i = 0; i < l; i += 2) {
                final K storedKey = (K) table.getOpaque(i);
                final V storedValue = (V) table.getOpaque(i + 1);
                if (storedKey != null && storedKey != DeletedKey) {
                    insertKeyValueNoLock(newTable, newCapacity, storedKey, storedValue);
                }
            }

            this.table = newTable;
            capacity = newCapacity;
            usedBuckets.set(size.getOpaque());
            resizeThreshold = (int) ((float) capacity * MapFillFactor);
        }

        private static <K, V> void insertKeyValueNoLock(final AtomicReferenceArray table, final int capacity, final K key, final V value) {
            int bucket = signSafeMod(hash(key), capacity);

            final int ll = (table.length() - 1);

            while (true) {
                final K storedKey = (K) table.getOpaque(bucket);

                if (storedKey == null) {
                    table.set(bucket, key);
                    table.set(bucket + 1, value);
                    return;
                }

                bucket = (bucket + 2) & ll;
            }
        }
    }

    private static final long HashMixer = 0xc6a4a7935bd1e995L;
    private static final int R = 47;

    static <K> long hash(final K key) {
        long hash = (long) key.hashCode() * HashMixer;
        hash ^= hash >>> R;
        hash *= HashMixer;
        return hash;
    }

    static int signSafeMod(final long n, final int Max) {
        return (int) (n & (long) (Max - 1)) << 1;
    }

    private static int alignToPowerOfTwo(final int n) {
        return (int) Math.pow(2.0, (double) (32 - Integer.numberOfLeadingZeros(n - 1)));
    }

//    private static class MyFasterList<V> extends FasterList<V> {
//        private final V[] emptyArray;
//
//        public MyFasterList(int size, V[] emptyArray) {
//            super(emptyArray);
//            this.emptyArray = emptyArray;
//        }
//
//        @Override
//        protected V[] newArray(int newCapacity) {
//            return Arrays.copyOf(emptyArray, newCapacity);
//        }
//    }
}