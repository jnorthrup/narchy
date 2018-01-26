/*
 * Copyright (C) 2011 Clearspring Technologies, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jcog.data.map;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * MODIFIED FROM stream-lib's ConcurrentStreamSummary UNTESTED
 *
 * Based on the <i>Space-Saving</i> algorithm and the <i>Stream-Summary</i>
 * data structure as described in:
 * <i>Efficient Computation of Frequent and Top-k Elements in Data Streams</i>
 * by Metwally, Agrawal, and Abbadi
 * <p/>
 * Ideally used in multithreaded applications, otherwise see {@link StreamSummary}
 *
 * @param <V> type of data in the stream to be summarized
 * @author Eric Vlaanderen
 *
 *
 */
public class ConcurrentMapStream<K, V> extends ConcurrentHashMap<K, ConcurrentMapStream.RankedItem<V>> {

    private final int capacity;
    private final AtomicReference<RankedItem<V>> minVal;
    private final AtomicLong size;
    private final AtomicBoolean reachCapacity;

    public ConcurrentMapStream(final int capacity) {
        this.capacity = capacity;
        this.minVal = new AtomicReference<>();
        this.size = new AtomicLong(0);
        this.reachCapacity = new AtomicBoolean(false);
    }

    public static class RankedItem<T> extends AtomicDouble implements Comparable<RankedItem<T>> {

        //private final AtomicLong error;
        //private final AtomicBoolean newItem;
        public final T the;

        public RankedItem(T item) {
            super(Double.NaN);
            this.the = item;
            //this.error = new AtomicLong(error);
            //this.newItem = new AtomicBoolean(true);
        }


        public double addAndGetCount(double delta) {
            return this.addAndGet(delta);
        }
//
//        //public void setError(long newError) {
//            this.error.set(newError);
//        }
//
//        //public long getError() {
//            return this.error.get();
//        }
//


//        public boolean isNewItem() {
//            return this.newItem.get();
//        }
//
//        public long getCount() {
//            return this.count.get();
//        }
//
        @Override
        public int compareTo(RankedItem<T> o) {
            return Double.compare(o.doubleValue(), doubleValue());
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.the);
            sb.append(':');
            sb.append(this.doubleValue());
//            sb.append(", Error: ");
//            sb.append(this.error);
//            sb.append(", object: ");
//            sb.append(super.toString());
            return sb.toString();
        }

        public boolean isNewItem() {
            double d = get();
            return d!=d;
        }

//        public void setNewItem(boolean newItem) {
//            this.newItem.set(newItem);
//        }
    }


    /** returns the existing value or null if it was inserted */
    public V put(final K key, final V element, final double incrementCount) {
        //double val = incrementCount;
        V result;
        RankedItem<V> value = new RankedItem<>(element);
        RankedItem<V> oldVal = putIfAbsent(key, value);
        if (oldVal != null) {
            oldVal.addAndGetCount(incrementCount);
            result = oldVal.the;
        } else if (reachCapacity.get() || size.incrementAndGet() > capacity) {
            reachCapacity.set(true);

            RankedItem<V> oldMinVal = minVal.getAndSet(value);
            remove(oldMinVal.the);

            while (oldMinVal.isNewItem()) {
                // Wait for the oldMinVal so its error and value are completely up to date.
                // no thread.sleep here due to the overhead of calling it - the waiting time will be microseconds.
            }
            double count = oldMinVal.doubleValue();

            value.addAndGetCount(count);
            //value.setError(count);
            result = null;
        } else {
            result = element;
        }

        value.set(incrementCount);
        minVal.set(getMinValue());

        return result;
    }

    private RankedItem<V> getMinValue() {
        RankedItem<V> minVal = null;
        for (RankedItem<V> entry : values()) {
            if (minVal == null || (!entry.isNewItem() && entry.doubleValue() < minVal.doubleValue())) {
                minVal = entry;
            }
        }
        return minVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (RankedItem entry : values()) {
            sb.append('(').append(entry.doubleValue()).append(": ").append(entry.the).append("),");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(']');
        return sb.toString();
    }

//    public List<V> peek(final int k) {
//        List<V> toReturn = new ArrayList<V>(k);
//        List<RankedItem<V>> values = peekWithScores(k);
//        for (RankedItem<V> value : values) {
//            toReturn.add(value.the);
//        }
//        return toReturn;
//    }

//    public List<RankedItem<V>> peekWithScores(final int k) {
//        List<RankedItem<V>> values = new ArrayList<RankedItem<V>>();
//        for (Map.Entry<K, RankedItem<V>> entry : itemMap.entrySet()) {
//            RankedItem<V> value = entry.getValue();
//            values.add(new RankedItem<V>(value.the, value.doubleValue(), value.getError()));
//        }
//        Collections.sort(values);
//        values = values.size() > k ? values.subList(0, k) : values;
//        return values;
//    }
}
