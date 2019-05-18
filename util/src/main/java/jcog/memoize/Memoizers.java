package jcog.memoize;

import jcog.memoize.byt.ByteHijackMemoize;
import jcog.memoize.byt.ByteKeyExternal;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** manager for multiple (pre-registered) memoizeByte instances, each with its own key/value type.
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


    public static final int DEFAULT_HIJACK_REPROBES = 3;
    public static final int DEFAULT_MEMOIZE_CAPACITY;
    static {
        //1gb -> 64k?
        DEFAULT_MEMOIZE_CAPACITY = (int) (Runtime.getRuntime().maxMemory()/(11*1024));
    }

    /** static instance */
    public static final Memoizers the = new Memoizers();

    private final CopyOnWriteArrayList<MemoizationStatistics> memoize = new CopyOnWriteArrayList<>();

    private Memoizers() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::print));
    }

    protected void print() {
        for (MemoizationStatistics m : memoize)
            m.print();
    }

    public <X,B extends ByteKeyExternal,Y> Function<X,Y> memoizeByte(String id, Function<X,B> byter, Function<B,Y> computation, int capacity) {
        Function<B, Y> c = memoizeByte(id, capacity, computation::apply);
        return (X x) -> c.apply(byter.apply(x));
    }

    /** registers a new memoizer with a default memoization implementation */
    public <X,Y> Function<X,Y> memoize(String id, int capacity, Function<X,Y> computation) {
        return add(id, memoizer(computation, capacity));
    }

    public <X, Y, M extends Memoize<X,Y>> Function<X, Y> add(String id, M m) {
        synchronized (memoize) {
            //HACK just use a map
            for (MemoizationStatistics ii : memoize) {
                if (ii.name.equals(id))
                    return ((M) ii.memoize)::apply;
            }
            memoize.add(new MemoizationStatistics(id, m));
            return m::apply;
        }
    }

    /** provides default memoizer implementation */
    private <X, Y> Memoize<X,Y> memoizer(Function<X, Y> computation, int capacity) {
        return new HijackMemoize<>(computation, capacity, DEFAULT_HIJACK_REPROBES);

        //return new CollisionMemoize<>(capacity, computation);
    }

    public <X extends ByteKeyExternal, Y> ByteHijackMemoize<X, Y> memoizeByte(String id, int capacity, Function<X, Y> computation) {
        ByteHijackMemoize<X, Y> c = new ByteHijackMemoize<>(computation, capacity, DEFAULT_HIJACK_REPROBES, false);
        add(id, c);
        return c;

//        CollisionMemoize<X, ByteKey.ByteKeyInternal<Y>> c = CollisionMemoize.byteKey(capacity, computation);
//        add(id, c);
//        return (x)->c.apply(x).get();
    }

    private static class MemoizationStatistics {
        public final String name;
        public final WeakReference<Memoize> memoize;

        MemoizationStatistics(String name, Memoize memoize) {
            this.name = name;
            this.memoize = new WeakReference<>(memoize);
        }

        public void print() {
            Memoize m = memoize.get();
            if (m!=null)
                System.out.println(name + '\n' + m.summary() + '\n');
//            else
//                System.out.println(name + " DELETED");
        }
    }
}
