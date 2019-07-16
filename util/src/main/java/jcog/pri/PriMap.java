package jcog.pri;

import jcog.Paper;
import jcog.exe.Exe;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.util.FloatConsumer;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
//import java.util.concurrent.ConcurrentHashMap;

/** accumulates/buffers/collates a stream of Y activations and termlinkages
 *  to be applied in a batch as a batch
 *
 *  this task instance represents the drainage operation
 *  which is recyclable and will be recycled, and is thread-safe for simultaneous drainage from multiple threads.
 *
 *  it can be drained while being populated from different threads.
 *
 *  TODO use non-UnitPri entries and then allow this to determine a global amplitude factor via adaptive dynamic range compression of priority
 *  TODO abstract to different impl
 * */
public class PriMap<Y> {

    /** pending Y activation collation */
    public final Map<Y, Prioritizable> items;

    public PriMerge merge;

    public PriMap(PriMerge merge) {
        this(merge, newMap(true));
    }


    public PriMap(PriMerge merge, Map<Y, Prioritizable> items) {
        this.items = items;
        merge(merge);
    }


    /** returns concurrent map for use in bags and buffers
     *
     * TODO expand into smart implementation chooser based on context,
     *      profiling history using current stack context as a clue, etc
     *
     * */
    @Paper
    public static <X,Y> Map<X,Y> newMap(boolean linked) {
        float load = 0.5f;
        if (Exe.concurrent()) {
            return linked ?
                new java.util.concurrent.ConcurrentHashMap<>(0, load, Runtime.getRuntime().availableProcessors())
                :
                new java.util.concurrent.ConcurrentHashMap<>(0, load, Runtime.getRuntime().availableProcessors())//
                //new org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe<>(0)
                //new NonBlockingHashMap() //<- not working right
                //new org.eclipse.collections.impl.map.mutable.ConcurrentHashMap(0, 0.5f)
                //new CustomConcurrentHashMap()
                ;
        } else {
             return
                 linked ?
                    //new LinkedHashMap(0, load)
                    new HashMap(0, load)
                    :
                    //new UnifiedMap(0, load)
                    new HashMap(0, load)
             ;
        }
    }


//    /** implements a plus merge (with collected refund)
//     * TODO detect priority clipping (@1.0) statistic
//     * */
//    public void linkPlus(Y source, Term target, float pri, @Nullable NumberX refund) {
//        float overflow = termlink.computeIfAbsent(new TermLinkage(source, target), (cc)-> cc)
//                .priAddOverflow(pri);
//        if (overflow > Float.MIN_NORMAL && refund!=null)
//            refund.addAt(overflow);
//    }

    public boolean isEmpty() {
        return items.isEmpty(); /* && termlink.isEmpty();*/
    }

    public final Y put(Y x, float pri, PriMerge merge, @Nullable FloatConsumer pressurizable) {
//        return putRaw(x, pri, merge, null);
//    }
//
//    private Y putRaw(Y x, float pri, PriMerge merge, @Nullable OverflowDistributor<Y> overflow) {
        if (pri != pri)
            return null;

        Prioritizable y = items.compute(x, x instanceof Prioritizable ?
                (xx, px) -> px != null ? px : (Prioritizable) xx :
                (xx, px) -> px != null ? px : new UnitPri(Float.NaN)
        );

        if (y != x) {
            float delta = merge(y, x, pri, merge);
            if (pressurizable!=null && Math.abs(delta) > Float.MIN_NORMAL)
                pressurizable.accept(delta);

            return (Y) y;
        }
        return x;
    }

    /** return the delta of the priority */
    public float merge(Prioritizable existing, Y incomingKey, float pri, PriMerge merge) {
        return merge.merge(existing, pri, PriReturn.Delta);
    }


    public void drain(Consumer<Y> each) {

        Iterator<Y> ii = items.keySet().iterator();
        while (ii.hasNext()) {
            Y e = ii.next();
            ii.remove();
            each.accept(e);
        }
    }

    /** drains the bufffer while applying a transformation to each item */
    public <X> void drain(Consumer<X> each, ObjectFloatToObjectFunction<Y,X> f) {
        Iterator<Map.Entry<Y, Prioritizable>> ii = items.entrySet().iterator();
        while (ii.hasNext()) {
            Map.Entry<Y, Prioritizable> e = ii.next();
            ii.remove();

            float pp = e.getValue().pri();
            if (pp == pp) {
                X x = f.valueOf(e.getKey(), pp);
                if (x!=null)
                    each.accept(x);
            }
        }
    }



    public final <Z extends Prioritizable> void put(Z x, PriMerge merge) {
        put((Y)x, x.pri(), merge, null);
    }

    public void clear() {
        items.values().removeIf(i -> {
            i.delete();
            return true;
        });
    }

    public int size() {
        return items.size();
    }

    /** sets the merge function */
    public void merge(PriMerge merge) {
        this.merge = merge;
    }

    //    private static final class TermLinkage extends UnitPri implements Comparable<TermLinkage> {
//
//        public final static Comparator<TermLinkage> preciseComparator = Comparator
//            .comparing((TermLinkage x)->x.Y.target())
//            .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
//            .thenComparingInt((TermLinkage x)->x.hashTarget) //at this point the order doesnt matter so first decide by hash
//            .thenComparing((TermLinkage x)->x.target);
//
//        /** fast and approximately same semantics of the sort as the preciseComparator:
//         *     soruce Y -> pri -> target
//         */
//        public final static Comparator<TermLinkage> sloppyComparator = Comparator
//                .comparingInt((TermLinkage x)->x.hashSource)
//                .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
//                .thenComparingInt((TermLinkage x)->x.hashTarget) //at this point the order doesnt matter so first decide by hash
//                .thenComparing(System::identityHashCode);
//
//        public final Y Y;
//        public final Term target;
//        public final int hashSource, hashTarget;
//
//        TermLinkage(Y source, Term target) {
//            this.Y = source;
//            this.target = target;
//            this.hashSource = source.hashCode();
//            this.hashTarget = target.hashCode();
//        }
//
//        @Override
//        public int hashCode() {
//            return hashSource ^ hashTarget;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) return true;
//            TermLinkage x = (TermLinkage) obj;
//            return x.hashSource == hashSource && x.hashTarget == hashTarget && x.target.equals(target) && x.Y.equals(Y);
//
//        }
//
//        @Override
//        public String toString() {
//            return "termlink(" + Y + ',' + target + ',' + pri() + ')';
//        }
//
//
//
//        @Override
//        public int compareTo(Activator.TermLinkage x) {
//            //return comparator.compare(this, x);
//            return sloppyComparator.compare(this, x);
//        }
//    }


//    @Override
//    public ITask next(NAR nar) {
//
//
//
////        int n = termlink.size();
////        if (n > 0) {
////            //drain at most n items from the concurrent map to a temporary list, sort it,
////            //then insert PLinks into the Y termlinks bag as they will be sorted into sequences
////            //of the same Y.
////            SortedList<TermLinkage> l = drainageBuffer(n);
////
////
////            Iterator<TermLinkage> ii = termlink.keySet().iterator();
////            while (ii.hasNext() && n-- > 0) {
////                TermLinkage x = ii.next();
////                ii.remove();
////
////                l.addAt(x);
////
////            }
////
////
////            //l.clearReallocate(1024, 8);
////            l.clear();
////        }
//
//        return null;
//    }

//    final static ThreadLocal<SortedList<TermLinkage>> drainageBuffers = ThreadLocal.withInitial(()->new SortedList<>(16));
//
//    /** provide a list to be used as a pre-insertion drainage buffer */
//    protected static SortedList<TermLinkage> drainageBuffer(int n) {
//        SortedList<TermLinkage> b = drainageBuffers.get();
//        b.ensureCapacity(n);
//        return b;
//    }


}
