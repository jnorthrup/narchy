package nars.link;

import jcog.TODO;
import jcog.math.FloatSupplier;
import jcog.pri.OverflowDistributor;
import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.concept.NodeConcept;
import nars.term.Term;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskLinkBag extends BufferedBag.SimpleBufferedBag<TaskLink, TaskLink> {

    private final FloatSupplier forgetRate;

    public TaskLinkBag(Bag<TaskLink, TaskLink> activates, FloatSupplier forgetRate, boolean concurrent) {
        super(activates, new TaskLinkBuffer(Param.tasklinkMerge, concurrent));
        this.forgetRate = forgetRate;
    }

    public void forget(NAR nar) {

        commit(nar.attn.forgetting.forget(this, 1f, forgetRate.asFloat()));

    }

    public String id(boolean in, boolean out) {
        if(out && !in)
            return id;
        else
            throw new TODO();
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
    public final Term atomTangent(NodeConcept src, TaskLink except, long now, int minUpdateCycles, Random rng) {
        return TermLinks.tangent(this,
                src, except,
                false, true,
                now, minUpdateCycles, rng);
    }


    private static final AtomicInteger ids = new AtomicInteger(0);

    /** each tasklink will have a unique id, allowing Atom concepts to store TangentConcepts instances from multiple TaskLink bags, either from the same or different NAR */
    public final String id = /*TaskLinkBag.class.getSimpleName() */ "T" + ids.incrementAndGet();

}
