package nars.attention;

import jcog.math.FloatRange;
import jcog.pri.OverflowDistributor;
import jcog.pri.ScalarValue;
import jcog.pri.UnitPri;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.term.Termed;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
//import java.util.concurrent.ConcurrentHashMap;

/** accumulates/buffers/collates a stream of concept activations and termlinkages
 *  to be applied in a batch as a batch
 *
 *  this task instance represents the drainage operation
 *  which is recyclable and will be recycled, and is thread-safe for simultaneous drainage from multiple threads.
 *
 *  it can be drained while being populated from different threads.
 *
 *  TODO use non-UnitPri entries and then allow this to determine a global amplitude factor via adaptive dynamic range compression of priority
 * */
public class Activator  {

    public final FloatRange conceptActivationRate = new FloatRange(0.5f, ScalarValue.EPSILONsqrt, 2f);



    /** pending concept activation collation */
    final Map<Concept, UnitPri> concepts =
            new ConcurrentHashMapUnsafe<>(1024);
            //new java.util.concurrent.ConcurrentHashMap(1024);

//    /** pending termlinking collation */
//    final ConcurrentHashMap<TermLinkage, TermLinkage> termlink = new ConcurrentHashMap(1024);

    public Activator() {

    }

//    /** implements a plus merge (with collected refund)
//     * TODO detect priority clipping (@1.0) statistic
//     * */
//    public void linkPlus(Concept source, Term target, float pri, @Nullable NumberX refund) {
//        float overflow = termlink.computeIfAbsent(new TermLinkage(source, target), (cc)-> cc)
//                .priAddOverflow(pri);
//        if (overflow > Float.MIN_NORMAL && refund!=null)
//            refund.add(overflow);
//    }

    public boolean isEmpty() {
        return concepts.isEmpty(); /* && termlink.isEmpty();*/
    }

    public Concept activate(Termed x, float pri, NAR nar, @Nullable OverflowDistributor<Concept> overflow) {
        assert(pri == pri); //        if (pri!=pri)     return null;

        @Nullable Concept y = x instanceof Concept ? (Concept)x : nar.concept(x, true);
        if (y == null)
            return null;
        return activate(y, pri, overflow);
    }

    public Concept activate(Concept x, float pri) {
        return activate(x, pri, null);
    }

    private Concept activate(Concept x, float pri, @Nullable OverflowDistributor<Concept> overflow) {
        return activateRaw(x, pri * conceptActivationRate.floatValue(), overflow);
    }

    private Concept activateRaw(Concept x, float pri) {
        return activateRaw(x, pri, null);
    }

    private Concept activateRaw(Concept x, float pri, @Nullable OverflowDistributor<Concept> overflow) {
        if (pri!=pri)
            return null;

        UnitPri a = concepts.computeIfAbsent(x, t -> new UnitPri());

        if (overflow!=null)
            overflow.merge(x, a, pri, Param.conceptMerge);
        else
            a.priAdd(pri);

        return x;
    }

    public void update(NAR n) {


        Iterator<Map.Entry<Concept, UnitPri>> ii = concepts.entrySet().iterator();
        while (ii.hasNext()) {
            Map.Entry<Concept, UnitPri> a = ii.next();
            Concept c = a.getKey();
            UnitPri p = a.getValue();
            ii.remove();

            n.concepts.activate(c, p.priGetAndDelete());
        }

//        removeIf(a -> {
//            n.concepts.activate(a.get(), a.pri());
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

    public final void activate(OverflowDistributor<Concept> overflow, Random random) {
        overflow.shuffle(random).redistribute(this::activateRaw);
    }

//    private static final class TermLinkage extends UnitPri implements Comparable<TermLinkage> {
//
//        public final static Comparator<TermLinkage> preciseComparator = Comparator
//            .comparing((TermLinkage x)->x.concept.term())
//            .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
//            .thenComparingInt((TermLinkage x)->x.hashTarget) //at this point the order doesnt matter so first decide by hash
//            .thenComparing((TermLinkage x)->x.target);
//
//        /** fast and approximately same semantics of the sort as the preciseComparator:
//         *     soruce concept -> pri -> target
//         */
//        public final static Comparator<TermLinkage> sloppyComparator = Comparator
//                .comparingInt((TermLinkage x)->x.hashSource)
//                .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
//                .thenComparingInt((TermLinkage x)->x.hashTarget) //at this point the order doesnt matter so first decide by hash
//                .thenComparing(System::identityHashCode);
//
//        public final Concept concept;
//        public final Term target;
//        public final int hashSource, hashTarget;
//
//        TermLinkage(Concept source, Term target) {
//            this.concept = source;
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
//            return x.hashSource == hashSource && x.hashTarget == hashTarget && x.target.equals(target) && x.concept.equals(concept);
//
//        }
//
//        @Override
//        public String toString() {
//            return "termlink(" + concept + ',' + target + ',' + pri() + ')';
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
////            //then insert PLinks into the concept termlinks bag as they will be sorted into sequences
////            //of the same concept.
////            SortedList<TermLinkage> l = drainageBuffer(n);
////
////
////            Iterator<TermLinkage> ii = termlink.keySet().iterator();
////            while (ii.hasNext() && n-- > 0) {
////                TermLinkage x = ii.next();
////                ii.remove();
////
////                l.add(x);
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
