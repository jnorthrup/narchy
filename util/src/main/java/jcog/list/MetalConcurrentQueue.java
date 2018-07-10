package jcog.list;

/*
 * Conversant Disruptor
 * modified for jcog
 *
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;

import java.util.function.Consumer;

/**
 * modified from Conversant Disruptor's PushPullConcurrentQueue
 * Tuned version of Martin Thompson's push pull queue
 *
 * Transfers from a single thread writer to a single thread reader are orders of nanoseconds (3-5)
 *
 * This code is optimized and tested using a 64bit HotSpot JVM on an Intel x86-64 environment.  Other
 * environments should be carefully tested before using in production.
 *
 * Created by jcairns on 5/28/14.
 */
public class MetalConcurrentQueue<E> extends MultithreadConcurrentQueue<E> { //implements ConcurrentQueue<E> {

    public MetalConcurrentQueue(int cap) {
        super(cap);
    }

    public int clear(Consumer<E> each) {
        return clear(each, -1);
    }

    public int clear(Consumer<E> each, int limit) {
        if (limit == 0)
            return 0;
        int count = 0;
        int s = limit >= 0 ? Math.min(limit, size()) : size();
        E next;
        while ((s-- > 0) && (next=poll())!=null) {
            each.accept(next);
            count++;
        }
        return count;
    }


//    final int size;
//
//    final int mask;
//
//    final AtomicInteger tail = new AtomicInteger();
//    final AtomicInteger head = new AtomicInteger();
//
////    long p1, p2, p3, p4, p5, p6, p7;
////    //@sun.misc.Contended
////    long tailCache = 0L;
////    long a1, a2, a3, a4, a5, a6, a7, a8;
////
//    final E[] buffer;
////
////    long r1, r2, r3, r4, r5, r6, r7;
////    //@sun.misc.Contended
////    long headCache = 0L;
////    long c1, c2, c3, c4, c5, c6, c7, c8;
//
//
//    public MetalConcurrentQueue(final int size) {
//        int rs = 1;
//        while(rs < size) rs <<= 1;
//        this.size = rs;
//        this.mask = rs-1;
//
//        buffer = (E[])new Object[this.size];
//    }
//
//
//    public int clear(Consumer<E> each) {
//        int count = 0;
//        int s = size();
//        E next;
//        while ((s-- > 0) && (next=poll())!=null) {
//            each.accept(next);
//            count++;
//        }
//        return count;
//    }
//
//    @Override
//    public boolean offer(final E e) {
//        if(e != null) {
//            final int tail = this.tail.get();
//            final int queueStart = tail - size;
//            int headCache = head.get();
//            if (headCache > queueStart) {
//                final int dx = (this.tail.getAndIncrement() & mask);
//                buffer[dx] = e;
//                return true;
//            } else {
//                return false;
//            }
//        } else {
//            throw new NullPointerException("Invalid element");
//        }
//    }
//
//    @Override
//    public E poll() {
//        final int headCache;
//        if ((headCache = this.head.get()) < tail.get()) {
//
//            final int dx = (this.head.getAndIncrement() & mask);
//
//            final E e = buffer[dx];
//
//            buffer[dx] = null;
//
//            return e;
//        } else {
//            return null;
//        }
//    }
//
//    @Override
//    public int remove(final E[] e) {
//        throw new TODO();
////        int n = 0;
////
////        int headCache = this.head.get();
////
////        final int nMax = e.length;
////        for(int i = headCache, end = tail.get(); n<nMax && i<end; i++) {
////            final int dx = (i & mask);
////            e[n++] = buffer[dx];
////            buffer[dx] = null;
////        }
////
////        this.head.addAndGet(n);
////
////        return n;
//    }
//
//    @Override
//    public void clear() {
//
//        clear((x)->{ });
//
////        Arrays.fill(buffer, null);
////        head.addAndGet(tail.get()-head.get());
//    }
//
//
//    @Override
//    public final E peek() {
//        return buffer[(head.get() & mask)];
//    }
//
//    /**
//     * This implemention is known to be broken if preemption were to occur after
//     * reading the tail pointer.
//     *
//     * Code should not depend on size for a correct result.
//     *
//     * @return int - possibly the size, or possibly any value less than capacity()
//     */
//    @Override
//    public final int size() {
//        return Math.max(tail.get() - head.get(), 0);
//    }
//
//    @Override
//    public int capacity() {
//        return size;
//    }
//
//    @Override
//    public final boolean isEmpty() {
//        return size()==0;
//    }
//
//    @Override
//    public final boolean contains(Object o) {
//        //if(o != null) {
//            for(int i = head.get(), end = tail.get(); i<end; i++) {
//                final E e = buffer[i & mask];
//                if(o.equals(e))
//                    return true;
//            }
//        //}
//        return false;
//    }
//
////    long sumToAvoidOptimization() {
////        return p1+p2+p3+p4+p5+p6+p7+a1+a2+a3+a4+a5+a6+a7+a8+r1+r2+r3+r4+r5+r6+r7+c1+c2+c3+c4+c5+c6+c7+c8+headCache+tailCache;
////    }
}
