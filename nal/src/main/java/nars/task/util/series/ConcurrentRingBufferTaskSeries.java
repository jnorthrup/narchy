package nars.task.util.series;

import jcog.TODO;
import jcog.WTF;
import jcog.data.list.MetalConcurrentQueue;
import jcog.math.Longerval;
import nars.Param;
import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.TIMELESS;
import static nars.time.Tense.ETERNAL;

public class ConcurrentRingBufferTaskSeries<T extends Task> extends AbstractTaskSeries<T> {

    final MetalConcurrentQueue<T> q;

    public ConcurrentRingBufferTaskSeries(int capacity) {
        super(capacity);
        this.q = new MetalConcurrentQueue<>(capacity); // + 1 /* for safety? */
    }


    @Override
    public final void push(T t) {
        long last = end();
        if (last!=TIMELESS && last >= t.start()) {
            if (Param.DEBUG)
                throw new RuntimeException(ConcurrentRingBufferTaskSeries.class + " only supports appending in linear, non-overlapping time sequence");
            else
                return; //?
        }

        if (!q.offer(t)) {
            pop();
            if (!q.offer(t)) {
                throw new WTF(); //TODO handle better
            }
        }
    }

    @Nullable
    @Override
    protected T pop() {
        T t = q.poll();
        if (t!=null) {
            t.delete();
        }
        return t;
    }


    /**
     * binary search
     */
    public int indexOf(long when) {
        int low = 0;
        int high = size() - 1;

        int closest = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = q.peek(mid);
            if (midVal == null)
                return closest;
            else
                closest = mid; //store in case we arrive at internal empty location

            if (midVal == null)
                break;

            long a = midVal.start();
            long b = midVal.end();
            if (when >= a && when <= b)
                return mid; //found

            if (when > b)
                low = mid + 1;
            else
                high = mid - 1;
        }

        return closest;
    }

    /**
     * binary search
     */
    public int[] indexOf(long start, long end) {
        throw new TODO();
//        int low = 0;
//        int high = size()-1;
//
//        int closest = -1;
//        while (low <= high) {
//            int mid = (low + high) >>> 1;
//            T midVal = q.get(mid);
//            if (midVal == null)
//                return closest;
//            else
//                closest = mid; //store in case we arrive at internal empty location
//
//            if (midVal == null)
//                break;
//
//            long a = midVal.start();
//            long b = midVal.end();
//            if (when >= a && when <= b)
//                return mid; //found
//
//            if (when > b)
//                low = mid + 1;
//            else
//                high = mid - 1;
//        }
//
//        return -1; //not found
    }

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


        if (minT != ETERNAL && minT != TIMELESS) {
            boolean point = maxT == minT;

            int b = indexOf(Math.min(e, maxT));
            if (b == -1)
                return true; //b = size() - 1;

            int a = point ? b : indexOf(Math.max(s, minT));
            if (a == -1)
                return true; //a = 0;

            int center = (a + b) / 2;

            boolean fullyDisjoint = Longerval.intersectLength(minT, maxT, s, e) == -1;

            int cap = this.cap;
            int r = 0, capacityRadius = cap / 2 + 1;
            T u, v;
            int supplied = 0;
            long suppliedMin = Long.MAX_VALUE, suppliedMax = Long.MIN_VALUE;
            do {

                int yi = center + r;
                v = q.peek(yi);
                if (v!=null && (!exactRange || v.intersects(minT, maxT))) {
                    if (!whle.test(v))
                        return false;
                    supplied++; if (!fullyDisjoint) { long zs = v.start(), ze = v.end(); if (zs < suppliedMin) suppliedMin = zs; if (ze > suppliedMax) suppliedMax = ze; }
                } else {
                    v = null;
                }


                r++;

                int ui = center - r;
                {
                    int uui = ui; if (uui < 0) uui += cap; //HACK prevent negative value
                    u = q.peek(uui);
                }
                if (u!=null && (!exactRange || u.intersects(minT, maxT))) {
                    if (!whle.test(u))
                        return false;
                    supplied++; if (!fullyDisjoint) { long zs = u.start(), ze = u.end(); if (zs < suppliedMin) suppliedMin = zs; if (ze > suppliedMax) suppliedMax = ze; }
                } else {
                    u = null;
                }

                if (!exactRange && supplied > 0 && (fullyDisjoint || ((suppliedMin <= minT) && (suppliedMax >= maxT))))
                    break; //early exit heuristic

            } while ((u!=null || v!=null) && r <= capacityRadius);

        } else {


            //just return the latest items while it keeps asking
            //TODO iterate from oldest to newest if the target time is before or near series start
            int qs = q.size();
            for (int i = qs - 1; i >= 0; i--) {
                T qi = q.peek(i);
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
