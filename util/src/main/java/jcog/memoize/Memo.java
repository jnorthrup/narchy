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

public class Memo {

    /** static instance */
    public static final Memo the = new Memo();

    private final CopyOnWriteArrayList<Memoize> memoize = new CopyOnWriteArrayList<>();

    public Memo() {

    }

    /** registers a new memoizer with a default memoization implementation */
    public <X,Y> Function<X,Y> memoize(Function<X,Y> computation) {
        Memoize<X, Y> m = memoizer(computation);
        memoize.add(m);
        return m::apply;
    }

    /** provides default memoizer implementation */
    private <X, Y> Memoize<X,Y> memoizer(Function<X, Y> computation) {
        return new HijackMemoize<>(computation, 64*1024, 3);
    }
}
