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

import static nars.time.Tense.ETERNAL;

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


    private static final AtomicInteger ids = new AtomicInteger(0);

    public final String id = /*TaskLinkBag.class.getSimpleName() */ "T" + ids.incrementAndGet();


    private static class TangentConcepts {

        final static int minUpdateCycles = 1;

        private /*volatile*/ long updated = ETERNAL;
        @Nullable private volatile TaskLink[] links;

        public TangentConcepts(long now) {
            this.updated = now - minUpdateCycles;
        }

        public boolean refresh(Atomic x, TaskLinkBag bag, long now) {
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
                                int cap = Math.max(2, (int) Math.ceil(Math.sqrt(bag.size()))); //heuristic
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

        public Term sample(Atomic srcTerm, TaskLink except, Random rng) {
            TaskLink l;
            TaskLink[] ll = links;
            if (ll == null)
                l = except;
            else {
                switch (ll.length) {
                    case 0:
                        l = except;
                        break;
                    case 1:
                        l = ll[0];
                        break;
                    case 2:
                        TaskLink a = ll[0], b = ll[1];
                        l = a.equals(except) ? b : a;
                        break;
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

            return l != null ? l.other(srcTerm) : null;


        }

        public Term sample(Atomic srcTerm, TaskLinkBag taskLinks, TaskLink except, long now, Random rng) {
            return refresh(srcTerm, taskLinks, now) ? sample(srcTerm, except, rng) : null;
        }
    }

    /**
     * acts as a virtual tasklink bag associated with an atom concept allowing it to otherwise act as a junction between tasklinking compounds which share it
     */
    @Nullable
    public Term atomTangent(NodeConcept src, TaskLink except, long now, Random rng) {
        Reference<TangentConcepts> matchRef = src.meta(id);
        TangentConcepts match = matchRef != null ? matchRef.get() : null;
        if (match == null) {
            match = new TangentConcepts(now);
            src.meta(id, new SoftReference<>(match));
        }

        return match.sample((Atomic) src.term, this, except, now, rng);
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
