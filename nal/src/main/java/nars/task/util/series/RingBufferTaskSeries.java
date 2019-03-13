package nars.task.util.series;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.LongInterval;
import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
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

    @Override
    public final boolean isEmpty(LongInterval l) {
        return !q.isEmpty() && super.isEmpty(l);
    }

    @Override
    public final void push(T t) {
        long last = end();
        if (last!=TIMELESS && last > t.start()) {
//            if (Param.DEBUG)
//                throw new RuntimeException(RingBufferTaskSeries.class + " only supports appending in linear, non-overlapping time sequence");
//            else
                return; //?
        }

        q.add(t);
    }

    @Nullable
    @Override
    protected T pop() {
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
        if (s == 0)
            return -1;
        else if (s == 1)
            return 0;
        else if (s == 2) {
            T a = q.peek(head, 0), b = q.peek(head, 1);
            if (a==null) return 1; else if (b == null) return 0;
            long at = a.meanTimeTo(when), bt = b.meanTimeTo(when);
            if (at < bt) return 0;
            else if (at > bt) return 1;
            else return ThreadLocalRandom.current().nextBoolean() ? 0 : 1;
        }

        int low = 0, high = s - 1, mid = -1;
        while (low <= high) {

            mid = (low + (high+1)) / 2;
            T midVal = q.peek(head, mid);
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
        //assert (minT != ETERNAL);


//        /*if (exactRange)*/ {
        long s = start();
        if (s == TIMELESS)
            return true; //nothing

        long e = end();
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

        int head = q.head();
        if (minT != ETERNAL && minT != TIMELESS) {
            boolean point = maxT == minT;

            int center;
            int b = indexNear(head, Math.min(e, maxT));
            if (b == -1)
                return true; //b = size() - 1;

            if (point) {
                int a = point ? b : indexNear(head, Math.max(s, minT));
                if (a == -1)
                    return true; //a = 0;

                center = (a + b) / 2;
            } else {
                center = b;
            }


            int size = this.size();
            int r = 0, rad = size / 2 + 1;

            do {

                int vv = center + r;
                T v = vv < size ? q.peek(head, vv) : null;
                if (v!=null && (!exactRange || v.intersects(minT, maxT))) {
                    if (!whle.test(v))
                        return false;
                } else {
                    v = null;
                }


                r++;

                int uu = center - r; //if (uu < 0) uu += cap; //HACK prevent negative value
                T u = uu >= 0 ? q.peek(head, uu) : null;
                if (u!=null && (!exactRange || u.intersects(minT, maxT))) {
                    if (!whle.test(u))
                        return false;
                } else {
                    u = null;
                }
//                if (!exactRange && supplied > 0)
//                    break; //early exit heuristic

                if (u == null && v == null)
                    break;

            } while (r <= rad);

        } else {


            //just return the latest items while it keeps asking
            //TODO iterate from oldest to newest if the target time is before or near series start
            int qs = q.size();
            for (int i = qs - 1; i >= 0; i--) {
                T qi = q.get(i);
                if (qi!=null && !whle.test(qi))
                    return false;
            }
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
    public T last() {
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
