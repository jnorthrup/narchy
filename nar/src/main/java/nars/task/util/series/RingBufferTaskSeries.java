package nars.task.util.series;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.LongInterval;
import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.math.LongInterval.TIMELESS;
import static nars.time.Tense.ETERNAL;

/** fully concurrent. implemented with MetalConcurrentQueue */
public class RingBufferTaskSeries<T extends Task> extends AbstractTaskSeries<T> {

    public final MetalConcurrentQueue<T> q;

    public RingBufferTaskSeries(int capacity) {
        super(capacity);
        this.q = new MetalConcurrentQueue<>(capacity); // + 1 /* for safety? */
    }

    @Override
    public final boolean isEmpty() {
        return q.isEmpty();
    }

    /** TODO HACK
     *  use a better testing for containment which may span multiple signals,
     *  rather than this naive intersection could still match if only, ex: 1% is covered by signal and the non-signal would be discarded */
    @Override public final boolean isEmpty(LongInterval l) {
        if (q.isEmpty())
            return true;

        long ls = l.start();
        if (ls == ETERNAL)
            return false;

        long le = l.end();

        int head = q.head();
        int mid = indexNear(head, (ls + le) / 2);
        if (mid != -1) {
            Task t = q.peek(head, mid);
            if (t != null && t.intersects(l))
                return false;
        }


        return super.isEmpty(ls, le);
    }

    @Override
    public final void push(T t) {
//        long last = end();
//        if (last!=TIMELESS && last > t.start()) {
////            if (Param.DEBUG)
////                throw new RuntimeException(RingBufferTaskSeries.class + " only supports appending in linear, non-overlapping time sequence");
////            else
//                return; //?
//        }

        //q.add(t);
        q.offer(t);
    }

    @Override
    protected @Nullable T pop() {
        T t = q.poll();
        if (t!=null)
            t.delete();

        return t;
    }


    /**
     * binary search
     */
    public int indexNear(int head, long when) {
        int s = size();
        switch (s) {
            case 0:
                return -1;
            case 1:
                return 0;
            case 2: {
                T a = q.peek(head, 0), b = q.peek(head, 1);
                if (a == null) return 1;
                else if (b == null) return 0;
                long at = a.meanTimeTo(when), bt = b.meanTimeTo(when);
                if (at < bt) return 0;
                else if (at > bt) return 1;
                else return ThreadLocalRandom.current().nextBoolean() ? 0 : 1;
            }
            default: {
                int len = q.length();
                int low = 0, high = s - 1, mid = -1;
                while (low <= high) {

                    mid = (low + (high + 1)) / 2;
                    T midVal = q.peek(head, mid, len);
                    if (midVal == null) {
                        low = mid + 1; ///oops ?
                        continue;
                    }

                    long a = midVal.start(), b = midVal.end();
                    if (when >= a && when <= b)
                        break; //found
                    else if (when > b) {
                        //assert(when>a);
                        low = mid + 1;
                    } else {//if (when < a)
                        //assert(when<a);
                        high = mid - 1;
                    }
                }
                return mid;
            }
        }

    }

//    /**
//     * binary search
//     */
//    public int[] indexOf(long start, long end) {
//        throw new TODO();
////        int low = 0;
////        int high = size()-1;
////
////        int closest = -1;
////        while (low <= high) {
////            int mid = (low + high) >>> 1;
////            T midVal = q.get(mid);
////            if (midVal == null)
////                return closest;
////            else
////                closest = mid; //store in case we arrive at internal empty location
////
////            if (midVal == null)
////                break;
////
////            long a = midVal.start();
////            long b = midVal.end();
////            if (when >= a && when <= b)
////                return mid; //found
////
////            if (when > b)
////                low = mid + 1;
////            else
////                high = mid - 1;
////        }
////
////        return -1; //not found
//    }

    /**
     * TODO obey exactRange flag
     */
    @Override
    public boolean whileEach(long minT, long maxT, boolean exactRange, Predicate<? super T> whle) {
        assert (minT != ETERNAL && minT != TIMELESS);


//        /*if (exactRange)*/ {
        long s = start();
        if (s == TIMELESS)
            return true; //nothing

        long e = end();
        if (e == TIMELESS)
            e = s; //HACK
//
//            if (maxT!=minT) {
//                if (e == TIMELESS || minT > e) {
//                    T l = last();
//                    return l == null || x.test(l); //OOB
//                }
//            }


        if (exactRange) {
            if ((e < minT) || (s > maxT)) //disjoint
                return true; //nothing
        }

        int len = q.length();
        int head = q.head();

        if (minT != ETERNAL && minT != TIMELESS) {
            long T = (minT+maxT)/2;

            int center = indexNear(head, T);

            int size = this.size();
            int r = 0;

            long lastLow = Long.MAX_VALUE, lastHigh = Long.MIN_VALUE;
            boolean increase = true, decrease = true;
            do {

                T u = null, v = null;
                long um = TIMELESS, vm = TIMELESS;

                if (increase) {
                    v = q.peek(head, center + r, len);

                    if (v!=null) {
                        vm = v.mid();
                        if (vm < lastHigh || (exactRange && lastHigh >= maxT)){
                            //wrap-around or OOB: stop
                            increase = false;
                        } else
                            lastHigh = vm;
                    }
                }

                r++;

                if (decrease) {
                    u = q.peek(head, center - r, len);

                    if (u!=null) {
                        um = u.mid();
                        if (um > lastLow || (exactRange && lastLow <= minT)) {
                            //wrap-around or OOB: stop
                            decrease = false;
                        } else
                            lastLow = um;
                    }
                }


                if (exactRange) {
                    if (u != null && !u.intersectsRaw(minT, maxT))
                        u = null;
                    if (v != null && !v.intersectsRaw(minT, maxT))
                        v = null;
                }

                if (u!=null && v!=null) {
                    //swap to the closest one to try first because it may be the last
                    if (Math.abs(T - vm) < Math.abs(T - um)) {
                        T uv = u;
                        u = v;
                        v = uv;
                    }
                } else if (u == null && v == null)
                    break;

                if (u!=null && !whle.test(u))
                    return false;
                if (v!=null && !whle.test(v))
                    return false;

            } while ((r < size) && (increase || decrease));

        } else {


            //just return the latest items while it keeps asking
            //TODO iterate from oldest to newest if the target time is before or near series start
            //            int offset = ThreadLocalRandom.current().nextInt(qs);
            return IntStream.iterate(q.size() - 1, i -> i >= 0, i -> i - 1).mapToObj(i -> q.peek(head, i, len)).noneMatch(qi -> qi != null && !whle.test(qi));
        }


        return true;

    }
//        else {
//            throw new TODO();
//        }


    @Override
    public int size() {
        return q.size();
    }

    @Override
    public T first() {
        return q.first();
    }

    @Override
    public final T last() {
        return q.last();
    }

    @Override
    public void clear() {
        q.clear();
    }

    @Override
    public Stream<T> stream() {
        return q.stream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        q.forEach(action);
    }

    @Override
    public long start() {
        T x = first(); //TODO check whether head/tail
        return x != null ? x.start() : TIMELESS;
    }

    @Override
    public long end() {
        T x = last(); //TODO check whether head/tail
        return x != null ? x.end() : TIMELESS;
    }


}
