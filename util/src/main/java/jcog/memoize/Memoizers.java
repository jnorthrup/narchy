package jcog.memoize;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** manager for multiple (pre-registered) memoize instances, each with its own key/value type.
 *
 *  TODO each separate memoizer can be managed centrally
 *      * memory consumption
 *      * access patterns
 *
 *  TODO shutdown hook print stats
 *
 *  TODO labels for each memoization function
 *
 *  TODO byte key convenience method
 * */

public class Memoizers {

    /** static instance */
    public static final Memoizers the = new Memoizers();

    private final CopyOnWriteArrayList<MemoizationStatistics> memoize = new CopyOnWriteArrayList<>();

    public Memoizers() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::print));
    }

    protected void print() {
        for (MemoizationStatistics m : memoize)
            m.print();
    }

    public final <X,Y> Function<X,Y> memoize(Function<X,Y> computation) {
        return memoize(computation.toString(), computation);
    }

    @Deprecated public <X,Y> Function<X,Y> memoize(String id, Function<X,Y> computation) {
        return memoize(id, 64*1024, computation);
    }

    /** registers a new memoizer with a default memoization implementation */
    public <X,Y> Function<X,Y> memoize(String id, int capacity, Function<X,Y> computation) {
        Memoize<X, Y> m = memoizer(computation, capacity);
        return add(id, m);
    }

    public <X, Y, M extends Memoize<X,Y>> Function<X, Y> add(String id, M m) {
        memoize.add(new MemoizationStatistics(id, m));
        return m::apply;
    }

    /** provides default memoizer implementation */
    private <X, Y> Memoize<X,Y> memoizer(Function<X, Y> computation, int capacity) {
        return new HijackMemoize<>(computation, capacity, 3);
    }

    private static class MemoizationStatistics {
        public final String name;
        public final Memoize memoize;

        MemoizationStatistics(String name, Memoize memoize) {
            this.name = name;
            this.memoize = memoize;
        }

        public void print() {
            System.out.println(name + "\n" + memoize.summary() + "\n");
        }
    }
}
