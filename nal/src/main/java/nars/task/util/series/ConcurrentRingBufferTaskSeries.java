package nars.task.util.series;

import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;
import nars.table.dynamic.SeriesBeliefTable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.TIMELESS;
import static nars.time.Tense.ETERNAL;

abstract public class ConcurrentRingBufferTaskSeries<T extends SeriesBeliefTable.SeriesTask> extends AbstractTaskSeries<T> {

    final MetalConcurrentQueue<T> q;

    public ConcurrentRingBufferTaskSeries(int capacity) {
        super(capacity);
        this.q = new MetalConcurrentQueue<>(capacity); // + 1 /* for safety? */
    }


    @Override
    protected void push(T t) {
        q.offer(t);
    }

    @Override
    protected T pop() {
        return q.poll();
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
            int cap = this.cap;
            int r = 0, radius = cap / 2 + 1;
            T u, v;
            do {

                int yi = center + r;
                v = q.peek(yi);
                if (v!=null && (!exactRange || v.intersects(minT, maxT))) {
                    if (!whle.test(v))
                        return false;
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
                } else {
                    u = null;
                }

            } while (u!=null && v!=null && r < radius);

            return true;
//                    if (a == b) {
//                        T aa = q.peek(a);
//                        if (aa!=null)
//                            return x.test(aa);
//                    } else {
//                        return q.whileEach(x, a, b+1);
//                    }


        } else {


            //just return the latest items while it keeps asking
            //TODO iterate from oldest to newest if the target time is before or near series start
            int qs = q.size();
            for (int i = qs - 1; i >= 0; i--) {
                T qi = q.peek(i);
                if (qi == null)
                    continue; //should only occurr at the ends
                if (!whle.test(qi))
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
