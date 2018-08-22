package nars.task.util.series;

import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;
import nars.table.dynamic.SeriesBeliefTable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.math.LongInterval.ETERNAL;
import static jcog.math.LongInterval.TIMELESS;

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


    /** binary search */
    public int indexOf(long when) {
        int low = 0;
        int high = size()-1;

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

    /** binary search */
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

    /** TODO obey exactRange flag */
    @Override public boolean whileEach(long minT, long maxT, boolean exactRange, Predicate<? super T> x) {
        assert(minT!=ETERNAL);

        /*if (exactRange)*/ {
            long s = start(), e = end();
            if (s == TIMELESS || maxT < s) {
                T f = first();
                return f!=null ? x.test(f) : true; //OOB
            }

            if (maxT!=minT) {
                if (e == TIMELESS || minT > e) {
                    T l = last();
                    return l!=null ? x.test(l) : true; //OOB
                }
            }

            boolean point = maxT == minT;
            int b = indexOf(Math.min(e, maxT));
            if (b != -1) {

                int a = point ? b : indexOf(Math.max(s, minT));
                if (a!=-1) {

                    if (a == b) {
                        T aa = q.peek(a);
                        if (aa!=null)
                            return x.test(aa);
                    } else {
                        return q.whileEach(x, a, b+1);
                    }
                }
            }

            return true;

        }
//        else {
//            throw new TODO();
//        }
    }


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
