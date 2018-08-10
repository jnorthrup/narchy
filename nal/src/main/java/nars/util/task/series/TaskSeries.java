package nars.util.task.series;

import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.dynamic.DynTruth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * fixed size buffer of Ts
 */
public interface TaskSeries<T extends Task> {

    /** the provided truth value should already be dithered */
    T add(Term term, byte punc, long start, long end, Truth nextValue, int dur, NAR nar);

    @Nullable DynTruth truth(long start, long end, long dur, NAR nar);

    int size();

    default void forEach(long minT, long maxT, boolean exactRange, Consumer<? super T> x) {
        if (!isEmpty())
            whileEach(minT, maxT, exactRange, (t) -> { x.accept(t); return true; } );
    }

    /** returns false if the predicate ever returns false; otherwise returns true even if empty.  this allows it to be chained recursively to other such iterators */
    boolean whileEach(long minT, long maxT, boolean exactRange, Predicate<? super T> x);

    void clear();

    Stream<T> stream();

    int forEach(long start, long end, int limit, Consumer<T> target);


    void forEach(Consumer<? super T> action);

    long start();
    long end();

    default boolean isEmpty() {
        return size()==0;
    }

    /** returns false if there is some data which occurrs inside the given interval */
    boolean isEmpty(long start, long end);
}
