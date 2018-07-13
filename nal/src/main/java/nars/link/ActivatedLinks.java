package nars.link;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.sort.SortedList;
import jcog.util.NumberX;
import nars.NAR;
import nars.concept.Concept;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/** accumulates/buffers a stream of activations to be applied later as a batch
 *  re-usable. it can be drained while being populated from different threads.
 * */
public class ActivatedLinks extends AbstractTask {


    final ConcurrentHashMap<TermLinkage, TermLinkage> termlink = new ConcurrentHashMap();

    public void link(Concept c, Term target, float pri, @Nullable NumberX refund) {
        float overflow = termlink.computeIfAbsent(new TermLinkage(c, target), (cc)-> cc)
                .priAddOverflow(pri);
        if (overflow > Float.MIN_NORMAL && refund!=null)
            refund.add(overflow);
    }

    public boolean isEmpty() {
        return termlink.isEmpty();
    }

    static final class TermLinkage extends Pri implements Comparable<TermLinkage> {

        public final static Comparator<TermLinkage> comparator = Comparator
            .comparing((TermLinkage x)->x.concept.term())
            .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
            .thenComparingInt((TermLinkage x)->x.hash) //at this point the order doesnt matter so first decide by hash
            .thenComparing((TermLinkage x)->x.target);

        public final Concept concept;
        public final Term target;
        public final int hash;

        TermLinkage(Concept concept, Term target) {
            this.concept = concept;
            this.target = target;
            this.hash = Util.hashCombine(concept, target);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return (this == obj)
                    ||
                    ((TermLinkage)obj).hash == hash &&
                    ((TermLinkage)obj).target.equals(target) &&
                    ((TermLinkage)obj).concept.equals(concept);

        }

        @Override
        public String toString() {
            return "termlink(" + concept + "," + target + "," + pri() + ")";
        }

        public PLink<Term> link() {
            return new PLink<>(target, pri());
        }

        @Override
        public int compareTo(@NotNull ActivatedLinks.TermLinkage x) {
            return comparator.compare(this, x);
        }
    }


    @Override
    public ITask next(NAR nar) {


        int n = termlink.size();
        if (n > 0) {
            //drain at most n items from the concurrent map to a temporary list, sort it,
            //then insert PLinks into the concept termlinks bag as they will be sorted into sequences
            //of the same concept.
            SortedList<TermLinkage> l = drainageBuffer(n);


            Iterator<TermLinkage> ii = termlink.keySet().iterator();
            while (ii.hasNext() && n-- > 0) {
                TermLinkage x = ii.next();
                ii.remove();

                l.add(x);

            }

            //l.sortThis(TermLinkage.comparator);

            for (TermLinkage x : l) {
                x.concept.termlinks().put(x.link());
            }

            //l.clearReallocate(1024, 8);
            l.clear();
        }

        return null;
    }

    final static ThreadLocal<SortedList<TermLinkage>> drainageBuffers = ThreadLocal.withInitial(()->new SortedList<>(16));

    /** provide a list to be used as a pre-insertion drainage buffer */
    protected SortedList<TermLinkage> drainageBuffer(int n) {
        SortedList<TermLinkage> b = drainageBuffers.get();
        b.ensureCapacity(n);
        return b;
    }
}
