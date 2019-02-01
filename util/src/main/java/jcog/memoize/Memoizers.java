package jcog.memoize;

import jcog.memoize.byt.ByteHijackMemoize;
import jcog.memoize.byt.ByteKey;

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

    public static final int DEFAULT_HIJACK_REPROBES = 3;
    public static final int DEFAULT_MEMOIZE_CAPACITY;
    static {
        //1gb -> 32k
        DEFAULT_MEMOIZE_CAPACITY = (int) (Runtime.getRuntime().maxMemory()/(32*1024));
    }


    private final CopyOnWriteArrayList<MemoizationStatistics> memoize = new CopyOnWriteArrayList<>();

    public Memoizers() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::print));
    }

    protected void print() {
        for (MemoizationStatistics m : memoize)
            m.print();
    }

//    public final <X,Y> Function<X,Y> memoize(Function<X,Y> computation) {
//        return memoize(computation.toString(), computation);
//    }
//
//    @Deprecated public <X,Y> Function<X,Y> memoize(String id, Function<X,Y> computation) {
//        return memoize(id, DEFAULT_MEMOIZE_CAPACITY, computation);
//    }

    public <X,B extends ByteKey,Y> Function<X,Y> memoize(String id, Function<X,B> byter, Function<B,Y> computation, int capacity) {
        Function<B, Y> b = add(id, memoizeByte(computation, capacity));
        return x -> b.apply(byter.apply(x));
    }



    /** registers a new memoizer with a default memoization implementation */
    public <X,Y> Function<X,Y> memoize(String id, int capacity, Function<X,Y> computation) {
        return add(id, memoizer(computation, capacity));
    }

    public <X, Y, M extends Memoize<X,Y>> Function<X, Y> add(String id, M m) {
        synchronized (memoize) {
            //HACK just use a map
            int n = memoize.size();
            for (int i = 0; i < n; i++) {
                MemoizationStatistics ii = memoize.get(i);
                if (ii.name.equals(id)) {
                    return ((M)ii.memoize)::apply;
                }
            }
            memoize.add(new MemoizationStatistics(id, m));
            return m::apply;
        }
    }

    /** provides default memoizer implementation */
    private <X, Y> Memoize<X,Y> memoizer(Function<X, Y> computation, int capacity) {
        //return new HijackMemoize<>(computation, capacity, DEFAULT_HIJACK_REPROBES);
        return new CollisionMemoize<>(capacity, computation);
    }
    private <X extends ByteKey, Y> Memoize<X, Y> memoizeByte(Function<X, Y> computation, int capacity) {
        return new ByteHijackMemoize<>(computation, capacity, DEFAULT_HIJACK_REPROBES, false);
        //return CollisionMemoize.get(capacity, computation);
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
