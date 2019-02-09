package nars.link;

import jcog.decide.Roulette;
import jcog.math.FloatSupplier;
import jcog.pri.OverflowDistributor;
import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.op.PriMerge;
import jcog.sort.TopN;
import nars.NAR;
import nars.Param;
import nars.concept.NodeConcept;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskLinkBag extends BufferedBag.SimpleBufferedBag<TaskLink, TaskLink> {

    private final FloatSupplier forgetRate;

    public TaskLinkBag(Bag<TaskLink, TaskLink> activates, FloatSupplier forgetRate, boolean concurrent) {
        super(activates, new TaskLinkBuffer(Param.tasklinkMerge, concurrent));
        this.forgetRate = forgetRate;
    }

    @Override
    protected final TaskLink keyInternal(TaskLink c) {
        return c;
    }

    public void forget(NAR nar) {

        commit(nar.attn.forgetting.forget(this, 1f, forgetRate.asFloat()));

    }


    private static class TaskLinkBuffer extends PriBuffer<TaskLink> {

        public TaskLinkBuffer(PriMerge merge, boolean concurrent) {
            super(merge, concurrent);
        }

        @Override
        protected void merge(Prioritizable existing, TaskLink incoming, float pri, OverflowDistributor<
                TaskLink> overflow) {
            //super.merge(existing, incoming, pri, overflow);
            ((TaskLink) existing).merge(incoming, merge);
        }
    }




    /**
     * acts as a virtual tasklink bag associated with an atom concept allowing it to otherwise act as a junction between tasklinking compounds which share it
     */
    @Nullable
    public Term atomTangent(NodeConcept src, TaskLink except, long now, int minUpdateCycles, Random rng) {
        Reference<TangentConcepts> matchRef = src.meta(id);
        TangentConcepts match = matchRef != null ? matchRef.get() : null;
        if (match == null) {
            match = new TangentConcepts(now, minUpdateCycles);
            src.meta(id, new SoftReference<>(match));
        }

        return match.sample((Atomic) src.term, this, except, now, minUpdateCycles, rng);
    }



    private static final AtomicInteger ids = new AtomicInteger(0);

    /** each tasklink will have a unique id, allowing Atom concepts to store TangentConcepts instances from multiple TaskLink bags, either from the same or different NAR */
    private final String id = /*TaskLinkBag.class.getSimpleName() */ "T" + ids.incrementAndGet();

    private static final class TangentConcepts {

        private volatile long updated;
        @Nullable private volatile TaskLink[] links;

        public TangentConcepts(long now, int minUpdateCycles) {
            this.updated = now - minUpdateCycles;
        }

        public boolean refresh(Atomic x, TaskLinkBag bag, long now, int minUpdateCycles) {
            if (now - updated >= minUpdateCycles) {

                TopN<TaskLink> match = null;

                for (TaskLink t : bag) {
                    if (t == null) continue; //HACK
                    float xp = t.priElseZero();
                    if (match == null || xp > match.minValueIfFull()) {
                        Term y = atomOther(x, t);
                        if (y != null) {
                            if (match == null) {
                                //TODO pool
                                int cap = Math.max(3, (int) Math.ceil(Math.sqrt(bag.size()))); //heuristic
                                match = new TopN<>(new TaskLink[cap], TaskLink::priElseZero);
                            }
                            match.add(t);
                        }
                    }
                }

                links = match!=null ? match.toArrayOrNullIfEmpty() : null;
                updated = now;

            }

            return links != null;
        }

        public Term sample(TaskLink except, Random rng) {
            TaskLink l;
            TaskLink[] ll = links;
            if (ll == null)
                l = except;
            else {
                switch (ll.length) {
                    case 0:
                        l = except; //only option (rare)
                        break;
                    case 1:
                        l = ll[0]; //only option
                        break;
                    case 2:
                        TaskLink a = ll[0], b = ll[1];
                        l = a.equals(except) ? b : a; //choose the one which is not 'except'
                        break;
                    //TODO optimized case 3
                    default:
                        l = ll[Roulette.selectRouletteCached(ll.length, (int i) -> {
                            TaskLink t = ll[i];
                            if (t.equals(except))
                                return 0f;
                            else
                                return t.priElseZero();
                        }, rng)];
                        break;
                }
            }

            return l != null ? l.target() : null;
        }

        public Term sample(Atomic srcTerm, TaskLinkBag taskLinks, TaskLink except, long now, int minUpdateCycles, Random rng) {
            return refresh(srcTerm, taskLinks, now, minUpdateCycles) ?
                    sample(except, rng) : null;
        }

        @Nullable
        static private Term atomOther(Term include, TaskLink t) {
            Term tSrc = t.source();
            if (include.equals(tSrc)) {
                Term y = t.target();
                return y; //!t.isSelf() ? y : null;
            }
//        if (include.equals(t.target())) {
//            Term y = tSrc;
//            return y; //!t.isSelf() ? y : null;
//        }
            return null;
        }
    }

}
