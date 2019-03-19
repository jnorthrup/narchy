package nars.link;

import jcog.TODO;
import jcog.pri.OverflowDistributor;
import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.op.PriMerge;
import nars.Param;
import nars.concept.Concept;
import nars.term.Term;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class TaskLinkBag extends BufferedBag.SimpleBufferedBag<TaskLink, TaskLink> {

    public TaskLinkBag(Bag<TaskLink, TaskLink> activates) {
        super(activates, new TaskLinkBuffer(Param.tasklinkMerge));
    }

    public String id(boolean in, boolean out) {
        if(out && !in)
            return id;
        else
            throw new TODO();
    }


    private static class TaskLinkBuffer extends PriBuffer<TaskLink> {

        public TaskLinkBuffer(PriMerge merge) {
            super(merge, PriBuffer.newMap());
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
    public final Term atomTangent(Concept src, byte punc, Predicate<TaskLink> filter, long now, int minUpdateCycles, Random rng) {
        return TermLinks.tangent(this,
                src, punc, filter,
                false, true,
                now, minUpdateCycles, rng);
    }


    private static final AtomicInteger ids = new AtomicInteger(0);

    /** each tasklink will have a unique id, allowing Atom concepts to store TangentConcepts instances from multiple TaskLink bags, either from the same or different NAR */
    public final String id = /*TaskLinkBag.class.getSimpleName() */ "T" + ids.incrementAndGet();

}
