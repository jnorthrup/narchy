package jcog.data.list;

/*
 * Conversant Disruptor
 * modified for jcog
 * see also: https://www.codeproject.com/articles/153898/yet-another-implementation-of-a-lock-free-circular
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

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import jcog.TODO;
import jcog.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * modified from Conversant Disruptor's PushPullConcurrentQueue
 *
 * WARNING: do not use the .get(int) method.  instead use peek(int) which determines the
 * correct buffer position.  this is a consequence of extending AtomicReferenceArray for
 * efficiency purposes.  SORRY in advance for any confusion this might cause
 *
 * Originally:
     * Tuned version of Martin Thompson's push pull queue
     * <p>
     * Transfers from a single thread writer to a single thread reader are orders of nanoseconds (3-5)
     * <p>
     * This code is optimized and tested using a 64bit HotSpot JVM on an Intel x86-64 environment.  Other
     * environments should be carefully tested before using in production.
     * <p>
     * Created by jcairns on 5/28/14.
 */
public class MetalConcurrentQueue<X> extends AtomicReferenceArray<X> implements ConcurrentQueue<X> {


    // the sequence number of the start of the queue
    // TODO use AtomicCycle ?
    final AtomicInteger
            head = new AtomicInteger(),
            nextHead = new AtomicInteger(),
            tail = new AtomicInteger(),
            nextTail = new AtomicInteger()
    ;






    public void forEach(Consumer<? super X> each) {

        int h = head();
        int s = tail() - h;
        if (s > 0) {
            int c = capacity();
            h = i(h, c);
            while(true) {
                X next = getOpaque(h);
                if (next != null)
                    each.accept(next);
                /*else
                    break; //assume we reached the end? */

                if (--s <= 0) break;

                if (++h == c) h = 0; //wrap
            }
        }
    }

    public final int tail() { return tail.getAcquire(); }

    public final int head() {
        return head.getAcquire();
    }

    public final int headNext() {
        return nextHead.getAcquire();
    }


    /**
     * Construct a blocking queue of the given fixed capacity.
     * <p>
     * Note: actual capacity will be the next power of two
     * larger than capacity.
     *
     * @param capacity maximum capacity of this queue
     */
    public MetalConcurrentQueue(final int capacity) {
        super(capacity);
    }

    public final boolean push(X x) {
        return push(x, 0);
    }

    public final boolean push(X x, int retries) {
        return push(x, Thread::onSpinWait, retries);
    }

    public boolean push(X x, Runnable wait, int retries) {
        boolean pushed;
        while (!(pushed = offer(x)) && retries-- > 0) {
            wait.run();
        }
        return pushed;
    }


    @Override public boolean offer(X x) {
        int spin = 0;

        int cap = capacity();
        for (; ; ) {
            final int tail = tail();
            // never offer onto the slot that is currently being polled off

            // will this sequence exceed the capacity
            if (head() > tail - cap) {
                // does the sequence still have the expected
                // value
                if (canTake(tail, 1)) {

                    // tailSeq is valid and got access without contention
                    set(i(tail, cap), x);

                    take(tail, 1);

                    return true;

                } // else - sequence misfire, somebody got our spot, try again
            } else
                // exceeded capacity
                return false;


            Util.pauseSpin(spin++);
        }
    }





    private int i(int x) {
        return i(x, capacity());
    }

    public int i(int x, int cap) {
         //return x & mask;
        return x % cap;
    }



    @Override
    @Nullable
    public X poll() {
        int spin = 0;

        do {
            final int head = this.head();
            // is there data for us to poll
            if (tail() <= head)
                return null; // do not notify - additional capacity is not yet available

            // check if we can update the sequence
            if (canPut(head, 1)) {

                int cap = capacity();

                final X pollObj = getAndSet(i(head, cap), null);

                put(head, 1);

                return pollObj;

            } // else - somebody else is reading this spot already: retry

            // this is the spin waiting for access to the queue
            Util.pauseSpin(spin++);

        } while (true);
    }



    @Override
    public final X peek() {
        return peek(0);
    }

    public final X peek(int delta) {
        return peek(head(), delta);
    }

    public final X peek(int head, int delta) {
        return getOpaque(i( head + delta) );
    }

    /** oldest element */
    public final X first() {
        return peek();
    }

    /** newest element */
    public final X last() {
        int i = size() - 1;
        return i >= 0 ? peek(i) : null;
    }

    @Override
    public int remove(final X[] x) {
        return remove(x, x.length);
    }

    //TODO
//    public int remove(final FasterList<X> x, IntToIntFunction availToDemand) {
//
//    }

    public int remove(final FasterList<X> x, int maxElements) {
        if (maxElements == 1) {
            X xx = poll();
            if (xx == null)
                return 0;
            else {
                x.add(xx);
                return 1;
            }
        } else {
            int initialSize = x.size();
            final int[] i = {initialSize};
            X[] a = x.array();
            int tryToGet = Math.min(a.length - initialSize, maxElements);
            int drained = clear(y -> a[i[0]++] = y, tryToGet);
            x.setSize(drained);
            return drained;
        }
    }

    // drain the whole queue at once
    /** This employs a "batch" mechanism to load all objects from the ring
     * in a single update.    This could have significant cost savings in comparison
     * with poll
     */
    public int remove(final X[] x, int maxElements) {

        throw new TODO("impl with clear(..)");

//        maxElements = Math.min(x.length, maxElements);
//        assert(maxElements > 0);
//
//        int spin = 0;
//
//        for (; ; ) {
//            final int head = head(); // prepare to qualify?
//            // is there data for us to poll
//            // note we must take a difference in values here to guard against
//            // integer overflow
//            int tail = tail();
//            if (tail == head) return 0; //empty
//
//            final int r = Math.min((tail - head), maxElements);
//            if (r > 0) {
//                // if we still control the sequence, update and return
//                if(canPut(head, r)) {
//
//                    int cap = capacity();
//                    int n = i(head, cap);
//                    for (int i = 0; i < r; i++) {
//                        x[i] = getAndSet(n, null);
//                        if (++n == cap) n = 0;
//                    }
//
//                    put(head, r);
//                    return r;
//                } else {
//                    spin = progressiveYield(spin); // wait for access
//                }
//
//
//            } else {
//                // nothing to read now
//                return 0;
//            }
//
//        }
    }

    private boolean canTake(int tail, int n) {
        return this.nextTail.weakCompareAndSetAcquire(tail, tail + n);
    }

    private boolean canPut(int head, int n) {
        return this.nextHead.weakCompareAndSetAcquire(head, head + n);
    }

    private void take(int tail, int n) {
        this.tail.setRelease(tail + n);
    }

    private void put(int head, int n) {
        //TODO attempt to set head=tail=0 if size is now zero. this returns the queue to a canonical starting position and might improve cpu caching
        this.head.setRelease(head + n);
    }

    public int clear(Consumer<X> each) {
        return clear(each, Integer.MAX_VALUE);
    }

    /** TODO make a custom clear impl, avoiding lambda */
    public <Y> int clear(BiConsumer<X,Y> each, Y param) {
        return clear((x)->each.accept(x, param));
    }

    /** TODO clearWith(IntObjectConsumer... */
    public int clear(final Consumer<X> each, int limit) {
        assert(limit > 0);

        int cap = capacity();

        int spin = 0;
        int k = 0;
        main: while (true) {
            int head = head(); // prepare to qualify?
            // is there data for us to poll
            // we must take a difference in values here to guard against integer overflow
            int tail = tail();
            int s = tail - head;
            if (s <= 0)
                return k; //empty

            int headLocal = i(head, cap);

            while (head < tail) {

                if (canPut(head, 1)) {
                    //get, nullify, and advance
                    X next = getAndSet(headLocal, null);
                    put(head, 1);

                    //callback
                    if (next!=null) {
                        each.accept(next);

                        //done?
                        if (++k >= limit)
                            return k;
                    }

                    if (++headLocal == cap) headLocal = 0;
                    head++;
                    spin = 0; //reset spin now that we are reading again

                } else {

                    Util.pauseSpin(spin++); // wait for access

                    continue main; //restart
                }
            }


        }
    }

    /**
     * This implemention is known to be broken if preemption were to occur after
     * reading the tail pointer.
     * <p>
     * Code should not depend on size for a correct result.
     *
     * @return int - possibly the size, or possibly any value less than capacity()
     */
    @Override
    public final int size() {
        // size of the ring
        // note these values can roll from positive to
        // negative, this is properly handled since
        // it is a difference
        return Math.max((tail() - head()), 0);
    }

    @Override
    public final int capacity() {
        return length();
    }

    @Override
    public final boolean isEmpty() {
        return head()==tail();
    }

    @Override
    public void clear() {
        clear((x) -> {}, Integer.MAX_VALUE);

        //throw new TODO("review");
//        int spin = 0;
//        int cap = capacity();
//        for (; ; ) {
//            final int head = this.head.getAcquire();
//            if (headCursor.weakCompareAndSetAcquire(head, head + 1)) {
//            //if (headCursor.weakCompareAndSetAcquire(head, head + 1)) {
//                for (; ; ) {
//                    final int tail = this.tail.getAcquire();
//                    if (tailCursor.weakCompareAndSetAcquire(tail, tail + 1)) {
//                    //if (tailCursor.weakCompareAndSetVolatile(tail, tail + 1)) {
//
//                        // we just blocked all changes to the queue
//
//                        // remove leaked refs
//                        for (int i = 0; i < cap; i++)
//                            set(i, null);
//
//                        // advance head to same location as current end
//                        this.tail.setRelease(tail+1);
//                        this.head.addAndGet(tail - head + 1);
//                        headCursor.setRelease(tail + 1);
//
//                        return;
//                    }
//                    spin = progressiveYield(spin);
//                }
//            }
//            spin = progressiveYield(spin);
//        }
    }

    @Override
    public final boolean contains(Object o) {
        int s = size();
        if (s > 0) {
            int cap = capacity();

            //TODO use fast iteration method
            int h = head();
            for (int i = 0; i < s; i++) {
                final int slot = (i(h + i, cap));
                X b = getOpaque(slot);
                if (b != null && b.equals(o)) return true;
            }
        }
        return false;
    }

//    int sumToAvoidOptimization() {
//        return p1+p2+p3+p4+p5+p6+p7+a1+a2+a3+a4+a5+a6+a7+a8+r1+r2+r3+r4+r5+r6+r7+c1+c2+c3+c4+c5+c6+c7+c8+headCache+tailCache;
//    }


    public int available() {
        return Math.max(0, capacity() - size());
    }

    public float availablePct() {
        return availablePct(capacity());
    }

    public float availablePct(int targetCapacity) {
        return availablePct(size(), targetCapacity);
    }

    public static float availablePct(int size, int targetCapacity) {
        return /*Util.unitize*/(1f - ((float) size) / targetCapacity);
    }


    public Stream<X> stream() {
        return IntStream.range(0, size()).mapToObj(this::peek).filter(Objects::nonNull);
    }

    public boolean isFull() {
        return isFull(0);
    }
    public boolean isFull(int afterAdding) {
        return size() + afterAdding >= capacity();
    }

    public void add(X x) {
        add(x, (xx)->{
            throw new RuntimeException(this + " overflow on add: " + x);
        });
    }

    public void add(X x, Consumer<X> ifBlocked) {
        if (!offer(x)) {
            ifBlocked.accept(x);
        }
    }

    public void add(X x, Function<X, Predicate<X>> continueWaiting, TimeUnit waitUnit, int timePeriods) {
        throw new TODO();
    }
}
