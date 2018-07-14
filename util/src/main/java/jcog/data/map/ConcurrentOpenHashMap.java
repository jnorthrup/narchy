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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    private static final float MapFillFactor = 0.9f;

    private static final int DefaultExpectedItems = 1024;
    private static final int DefaultConcurrencyLevel = 4;

    private final Section<K, V>[] sections;

    public ConcurrentOpenHashMap() {
        this(DefaultExpectedItems);
    }

    public ConcurrentOpenHashMap(int expectedItems) {
        this(expectedItems, DefaultConcurrencyLevel);
    }

    public ConcurrentOpenHashMap(int expectedItems, int concurrencyLevel) {
        checkArgument(expectedItems > 0);
        checkArgument(concurrencyLevel > 0);
        checkArgument(expectedItems >= concurrencyLevel);

        int numSections = concurrencyLevel;
        int perSectionExpectedItems = expectedItems / numSections;
        int perSectionCapacity = (int) (perSectionExpectedItems / MapFillFactor);
        this.sections = (Section<K, V>[]) new Section[numSections];

        for (int i = 0; i < numSections; i++) {
            sections[i] = new Section<>(perSectionCapacity);
        }
    }

    public int size() {
        long size = 0;
        for (Section<K, V> s : sections) {
            size += s.size.getOpaque();
        }
        if (size >= Integer.MAX_VALUE)
            return Integer.MAX_VALUE-1; 
        return (int) size;
    }

    public long capacity() {
        long capacity = 0;
        for (Section<K, V> s : sections) {
            capacity += s.capacity;
        }
        return capacity;
    }

    public boolean isEmpty() {
        for (Section<K, V> s : sections) {
            if (s.size.getOpaque() != 0) {
                return false;
            }
        }

        return true;
    }

    public V get(Object key) {
        checkNotNull(key);
        long h = hash(key);
        return getSection(h).get((K)key, (int) h);
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public V put(K key, V value) {
        checkNotNull(key);
        checkNotNull(value);
        long h = hash(key);
        return getSection(h).put(key, value, (int) h, false, null);
    }

    public V putIfAbsent(K key, V value) {
        checkNotNull(key);
        checkNotNull(value);
        long h = hash(key);
        return getSection(h).put(key, value, (int) h, true, null);
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> provider) {
        checkNotNull(key);
        checkNotNull(provider);
        long h = hash(key);
        return getSection(h).put(key, null, (int) h, true, provider);
    }

    public V remove(Object key) {
        checkNotNull(key);
        long h = hash(key);
        return getSection(h).remove((K)key, null, (int) h);
    }

    @Override public boolean remove(Object key, Object value) {
        checkNotNull(key);
        checkNotNull(value);
        long h = hash(key);
        return getSection(h).remove((K)key, value, (int) h) != null;
    }

    private Section<K, V> getSection(long hash) {
        
        final int sectionIdx = (int) (hash >>> 32) & (sections.length - 1);
        return sections[sectionIdx];
    }

    public void clear() {
        for (Section<K, V> s : sections) {
            s.clear();
        }
    }

    public void forEach(BiConsumer<? super K, ? super V> processor) {
        for (Section<K, V> s : sections) {
            s.forEach(processor);
        }
    }

    /**
     * @return a new list of all keys (makes a copy)
     */
    public List<K> keys() {
        List<K> keys = Lists.newArrayList();
        forEach((key, value) -> keys.add(key));
        return keys;
    }

    public List<V> values(V[] emptyArray) {
        List<V> values = new MyFasterList<V>(size(), emptyArray);
        forEach((key, value) -> values.add(value));
        return values;
    }

    public List<V> values() {
        List<V> values = new FasterList(size());
        forEach((key, value) -> values.add(value));
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new TODO();
    }

    
    @SuppressWarnings("serial")
    private static final class Section<K, V> extends StampedLock {
        
        private Object[] table;

        private int capacity;
        private final AtomicInteger size = new AtomicInteger();
        private int usedBuckets;
        private int resizeThreshold;

        Section(int capacity) {
            this.capacity = alignToPowerOfTwo(capacity);
            this.table = new Object[2 * this.capacity];
            this.size.setRelease(0);
            this.usedBuckets = 0;
            this.resizeThreshold = (int) (this.capacity * MapFillFactor);
        }

        V get(K key, int keyHash) {
            long stamp = tryOptimisticRead();
            boolean acquiredLock = false;
            int bucket = signSafeMod(keyHash, capacity);

            try {
                while (true) {
                    
                    Object[] table = this.table;
                    K storedKey = (K) table[bucket];
                    V storedValue = (V) table[bucket + 1];

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

                            bucket = signSafeMod(keyHash, capacity);
                            storedKey = (K) table[bucket];
                            storedValue = (V) table[bucket + 1];
                        }

                        if (key.equals(storedKey)) {
                            return storedValue;
                        } else if (storedKey == null) {
                            
                            return null;
                    }
                    }

                    bucket = (bucket + 2) & (table.length - 1);
                }
            } finally {
                if (acquiredLock) {
                    unlockRead(stamp);
                }
            }
        }

        V put(K key, V value, int keyHash, boolean onlyIfAbsent, Function<? super K, ? extends V> valueProvider) {
            long stamp = writeLock();
            int bucket = signSafeMod(keyHash, capacity);

            
            int firstDeletedKey = -1;

            try {
                while (true) {
                    Object[] table = this.table;
                    K storedKey = (K) table[bucket];
                    V storedValue = (V) table[bucket + 1];

                    if (storedKey!=null && key.equals(storedKey)) {
                        if (!onlyIfAbsent) {
                            
                            table[bucket + 1] = value;
                            return storedValue;
                        } else {
                            return storedValue;
                        }
                    } else if (storedKey == null) {


                        if (firstDeletedKey != -1) {
                            bucket = firstDeletedKey;
                        } else {
                            ++usedBuckets;
                        }

                        if (value == null) {
                            value = valueProvider.apply(key);
                        }

                        size.incrementAndGet();
                        table[bucket] = key;
                        table[bucket + 1] = value;
                        return valueProvider != null ? value : null;
                    } else if (storedKey == DeletedKey) {

                        if (firstDeletedKey == -1) {
                            firstDeletedKey = bucket;
                        }
                    }

                    bucket = (bucket + 2) & (table.length - 1);
                }
            } finally {
                if (usedBuckets > resizeThreshold) {
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

        private V remove(K key, Object value, int keyHash) {
            long stamp = writeLock();
            int bucket = signSafeMod(keyHash, capacity);

            try {
                while (true) {
                    Object[] table = this.table;
                    K storedKey = (K) table[bucket];
                    V storedValue = (V) table[bucket + 1];
                    if (key.equals(storedKey)) {
                        if (value == null || value.equals(storedValue)) {
                            size.decrementAndGet();

                            int nextInArray = (bucket + 2) & (table.length - 1);
                            if (table[nextInArray] == null) {
                                table[bucket] = null;
                                table[bucket + 1] = null;
                                --usedBuckets;
                            } else {
                                table[bucket] = DeletedKey;
                                table[bucket + 1] = null;
                            }

                            return storedValue;
                        } else {
                            return null;
                        }
                    } else if (storedKey == null) {
                        
                        return null;
                    }

                    bucket = (bucket + 2) & (table.length - 1);
                }

            } finally {
                unlockWrite(stamp);
            }
        }

        void clear() {
            long stamp = writeLock();

            try {
                Arrays.fill(table, null);
                this.size.setRelease(0);
                this.usedBuckets = 0;
            } finally {
                unlockWrite(stamp);
            }
        }

        public void forEach(BiConsumer<? super K, ? super V> processor) {
            long stamp = tryOptimisticRead();

            Object[] table = this.table;
            boolean acquiredReadLock = false;

            try {

                
                if (!validate(stamp)) {
                    
                    stamp = readLock();
                    acquiredReadLock = true;
                    table = this.table;
                }

                
                for (int bucket = 0; bucket < table.length; bucket += 2) {
                    K storedKey = (K) table[bucket];
                    V storedValue = (V) table[bucket + 1];

                    if (!acquiredReadLock && !validate(stamp)) {
                        
                        stamp = readLock();
                        acquiredReadLock = true;

                        storedKey = (K) table[bucket];
                        storedValue = (V) table[bucket + 1];
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
            
            int newCapacity = capacity * 2;
            Object[] newTable = new Object[2 * newCapacity];

            
            Object[] table = this.table;
            for (int i = 0; i < table.length; i += 2) {
                K storedKey = (K) table[i];
                V storedValue = (V) table[i + 1];
                if (storedKey != null && storedKey != DeletedKey) {
                    insertKeyValueNoLock(newTable, newCapacity, storedKey, storedValue);
                }
            }

            this.table = newTable;
            capacity = newCapacity;
            usedBuckets = size.getOpaque();
            resizeThreshold = (int) (capacity * MapFillFactor);
        }

        private static <K, V> void insertKeyValueNoLock(Object[] table, int capacity, K key, V value) {
            int bucket = signSafeMod(hash(key), capacity);

            while (true) {
                K storedKey = (K) table[bucket];

                if (storedKey == null) {
                    
                    table[bucket] = key;
                    table[bucket + 1] = value;
                    return;
                }

                bucket = (bucket + 2) & (table.length - 1);
            }
        }
    }

    private static final long HashMixer = 0xc6a4a7935bd1e995L;
    private static final int R = 47;

    static <K> long hash(K key) {
        long hash = key.hashCode() * HashMixer;
        hash ^= hash >>> R;
        hash *= HashMixer;
        return hash;
    }

    static int signSafeMod(long n, int Max) {
        return (int) (n & (Max - 1)) << 1;
    }

    private static int alignToPowerOfTwo(int n) {
        return (int) Math.pow(2, 32 - Integer.numberOfLeadingZeros(n - 1));
    }

    private static class MyFasterList<V> extends FasterList<V> {
        private final V[] emptyArray;

        public MyFasterList(int size, V[] emptyArray) {
            super(emptyArray);
            this.emptyArray = emptyArray;
        }

        @Override
        protected V[] newArray(int newCapacity) {
            return Arrays.copyOf(emptyArray, newCapacity);
        }
    }
}