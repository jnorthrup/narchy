package jcog.pri;

import jcog.data.map.NonBlockingHashMap;
import jcog.exe.Exe;
import jcog.pri.op.PriMerge;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
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
public class PriBuffer<Y> {


    /** pending Y activation collation */
    public final Map<Y, Prioritizable> items;
            //new ConcurrentHashMapUnsafe<>(512);
            //new java.util.concurrent.ConcurrentHashMap(512);

    public final PriMerge merge;

    public PriBuffer(PriMerge merge) {
        this(merge, newMap());
    }

//    public PriBuffer(PriMerge merge, boolean concurrent) {
//        this(merge, concurrent ?
//                newConcurrentMap():
//                new LinkedHashMap<>(0, 0.5f) );
//    }

    public PriBuffer(PriMerge merge, Map<Y, Prioritizable> items) {
        this.merge = merge;
        this.items = items;
    }

    /** returns concurrent map for use in bags and buffers */
    public static Map newMap() {
        if (Exe.concurrent()) {
            return
            new NonBlockingHashMap();
            //new java.util.concurrent.ConcurrentHashMap<>(0, 0.5f);
            //new org.eclipse.collections.impl.map.mutable.ConcurrentHashMap(0, 0.5f);
            //new CustomConcurrentHashMap(64);
            //new org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe<>(0);
        } else {
             return
                 //new LinkedHashMap();
                 new UnifiedMap();
                 //new HashMap();
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

    public Y put(Y x, float pri) {
        return put(x, pri, null);
    }

    public Y put(Y y, float pri, @Nullable OverflowDistributor<Y> overflow) {
        return putRaw(y, pri, overflow);
    }

    private Y putRaw(Y x, float pri, @Nullable OverflowDistributor<Y> overflow) {
        if (pri != pri)
            return null;

        Prioritizable y = items.compute(x, x instanceof Prioritizable ?
                (xx, px) -> px != null ? px : (Prioritizable) xx :
                (xx, px) -> px != null ? px : new UnitPri(Float.NaN)
        );

        if (y != x) {
            merge(y, x, pri, overflow);
            return (Y) y;
        }
        return x;
    }

    protected void merge(Prioritizable existing, Y incoming, float pri, OverflowDistributor<Y> overflow) {
        if (overflow!=null) {
            overflow.merge(incoming, existing, pri, merge);
        } else {
            merge.merge(existing, pri);
        }
    }

    /** drains the bufffer while performing an operation on it */
    public void update(ObjectFloatProcedure<Y> each) {

        items.entrySet().removeIf(e->{
            float pp = e.getValue().pri();
            if (pp == pp)
                each.value(e.getKey(), pp);
            return true;
        });

//        Iterator<Map.Entry<Y, Prioritizable>> ii = items.entrySet().iterator();
//        while (ii.hasNext()) {
//            Map.Entry<Y, Prioritizable> e = ii.next();
//        }

//        removeIf(a -> {
//            n.Ys.activate(a.get(), a.pri());
//            return true;
//        });

        //if (!isEmpty()) {
            //deferred
//        if (deferredOrInline) {
//            n.input(this);
//        } else {
            //inline
//            ITask.run(this, n);
//        }

    }

    public final void put(OverflowDistributor<Y> overflow, Random random) {
        overflow.shuffle(random).redistribute(this::put);
    }

    public final <Z extends Prioritizable> void put(Z x) {
        put((Y)x, x.pri());
    }

    public void clear() {
        items.values().removeIf(i -> {
            i.delete();
            return true;
        });
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
