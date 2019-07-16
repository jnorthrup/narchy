package nars.task.util.series;

import jcog.TODO;
import jcog.math.LongInterval;
import nars.Task;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * fixed size buffer of Ts
 */
public interface TaskSeries<T extends Task> {



//    @Nullable
//    default DynStampEvi truth(long start, long end, Predicate<Task> filter) {
//
//        int size = size();
//        if (size == 0)
//            return null;
//
//        int limit = Answer.TASK_LIMIT_DEFAULT;
//
//        DynStampEvi d = new DynStampEvi(Math.min(size, limit));
//
//        RankedTopN<Task> inner = TopN.pooled(Answer.topTasks, Math.min(size, limit), filter != null ?
//                (Task t) -> filter.test(t) ? -t.minTimeTo(start, end) : Float.NaN
//                :
//                (Task t) -> -t.minTimeTo(start, end));
//
//        try {
//
//
//            forEach(start, end, true, inner::addRanked);
//
//
//            int l = inner.size();
//            if (l > 0) {
//                Object[] ii = inner.items;
//                int i;
//                for (i = 0; i < l; i++)
//                    d.addAt((Task) ii[i]);
//
//                return d;
//            }
//
//            return null;
//        } finally {
//            TopN.unpool(Answer.topTasks, inner);
//        }
//
//    }

    int size();

    default void forEach(long minT, long maxT, boolean exactRange, Consumer<? super T> x) {
        if (!isEmpty()) {
//            if (minT == ETERNAL) {
//                T l = last();
//                if(x!=null)
//                    x.accept(l);
//                return;
//            }

            whileEach(minT, maxT, exactRange, (t) -> {
                x.accept(t);
                return true;
            });
        }
    }

    /**
     * returns false if the predicate ever returns false; otherwise returns true even if empty.  this allows it to be chained recursively to other such iterators
     */
    boolean whileEach(long minT, long maxT, boolean exactRange, Predicate<? super T> x);

    void clear();

    Stream<T> stream();



    void forEach(Consumer<? super T> action);

    /** returns Tense.TIMELESS (Long.MAX_VALUE) if empty */
    long start();

    /** returns Tense.TIMELESS (Long.MAX_VALUE) if empty */
    long end();

    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * returns false if there is some data which occurrs inside the given interval
     */
    default boolean isEmpty(long start, long end) {
        return whileEach(start, end, true, (x)->{
            //keep looking
            return !x.intersects(start, end); //found
        });
    }

    /** returns time density filled by the series for the given interval */
    default float density(long start, long end) {
        throw new TODO();
    }

    default boolean isEmpty(LongInterval l) {
        return isEmpty(l.start(), l.end());
    }

    T first();
    T last();



}
