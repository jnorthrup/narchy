/*
 * Copyright (C) 2011 Clearspring Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jcog.pri.bag.util;

import java.util.*;

/**
 * https:
 */
public class SampleSet<T> {

    private final Map<T, Node<T>> sampleMap;
    private int size;
    private long count;
    private final Random random;

    /**
     * Element with the highest frequency in the set
     */
    private Node<T> head;

    /**
     * Element with the lowest frequency in the set
     */
    private Node<T> tail;

    public SampleSet() {
        this(7);
    }

    public SampleSet(int capacity) {
        this(capacity, new Random());
    }

    public SampleSet(int capacity, Random random) {
        sampleMap = new HashMap<T, Node<T>>(capacity);
        this.random = random;
    }

    public T peek() {
        return (head != null) ? head.element : null;
    }

    public List<T> peek(int k) {
        List<T> topK = new ArrayList<T>(k);
        for (Node<T> itr = head; itr != null && topK.size() < k; itr = itr.next) {
            topK.add(itr.element);
        }
        return topK;
    }

    public long put(T element) {
        return put(element, 1);
    }

    public long put(T element, int incrementCount) {
        Node<T> n = sampleMap.compute(element, (e, node) -> {
            if (node != null) {
                node.count = node.count + incrementCount;
                promote(node);
            } else {
                node = new Node();
                node.element = element;
                node.count = incrementCount;
                node.prev = tail;
                if (tail != null) {
                    tail.next = node;
                }
                tail = node;
                if (head == null) {
                    head = node;
                }
                sampleMap.put(element, node);
                size++;
            }
            count++;
            return node;
        });
        return n.count;
    }

    public T removeRandom() {
        double p = random.nextDouble() * count;
        long weight = 0;
        for (Node<T> itr = head; itr != null; itr = itr.next) {
            weight += itr.count;
            if (p < weight) {
                itr.count--;
                count--;
                demote(itr);
                if (itr.count == 0) {
                    removeMin();
                }
                return itr.element;
            }
        }
        return null;
    }

    protected T removeMin() {
        if (tail == null) {
            return null;
        }
        size--;
        count -= tail.count;
        T minElement = tail.element;
        tail = tail.prev;
        if (tail != null) {
            tail.next = null;
        }
        sampleMap.remove(minElement);
        return minElement;
    }

    public int size() {
        return size;
    }

    public long count() {
        return count;
    }

    protected T peekMin() {
        return tail.element;
    }

    protected void promote(Node<T> node) {
        
        while (node.prev != null && node.count > node.prev.count) {
            
            
            Node<T> b = node.prev, c = node, d = node.next, a = (b == null) ? null : b.prev;

            
            if (a != null) {
                a.next = c;
            }
            c.prev = a;

            c.next = b;
            b.prev = c;

            b.next = d;
            if (d != null) {
                d.prev = b;
            }

            
            if (head == b) {
                head = c;
            }
            if (tail == c) {
                tail = b;
            }
        }
    }

    protected void demote(Node<T> node) {
        
        while (node.next != null && node.count < node.next.count) {
            
            
            Node<T> a = node.prev, b = node, c = node.next,
                    d = (c == null) ? null : c.next;

            
            if (a != null) {
                a.next = c;
            }
            c.prev = a;

            c.next = b;
            b.prev = c;

            if (d != null) {
                d.prev = b;
            }
            b.next = d;

            
            if (head == b) {
                head = c;
            }
            if (tail == c) {
                tail = b;
            }
        }
    }

    private static final class Node<E> {

        private Node next;
        private Node prev;
        private E element;
        private long count;
    }

    /*
     * Copyright (C) 2011 Clearspring Technologies, Inc.
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     * http:
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */



    /**
     * Estimates most frequently occurring items in a data stream
     * using a bounded amount of memory.
     * <p/>
     * Warning: this class is not thread safe.
     */
    public static class StochasticTopper<T>  {

        private final int sampleSize;
        private final SampleSet<T> sample;
        private final Random random;
        private long count;

        public StochasticTopper(int sampleSize) {
            this(sampleSize, null);
        }

        public StochasticTopper(int sampleSize, Long seed) {
            this.sample = new SampleSet<T>(sampleSize);
            this.sampleSize = sampleSize;

            if (seed != null) {
                random = new Random(seed);
            } else {
                random = new Random();
            }
        }

        public boolean offer(T item, int incrementCount) {
            count++;
            boolean taken = false;
            if (sample.count() < sampleSize) {
                sample.put(item, incrementCount);
                taken = true;
            } else if (random.nextDouble() < sampleSize / (double) count) {
                sample.removeRandom();
                sample.put(item, incrementCount);
                taken = true;
            }

            return taken;
        }

        public boolean offer(T item) {
            return offer(item, 1);
        }

        /**
         * Retrieve top k items
         */
        public List<T> peek(int k) {
            return sample.peek(k);
        }
    }
}