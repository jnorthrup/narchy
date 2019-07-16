package nars.link;

import jcog.TODO;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.SimpleBufferedBag;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import nars.NAL;

import java.util.concurrent.atomic.AtomicInteger;

public class TaskLinkBag extends SimpleBufferedBag<TaskLink, TaskLink> {

    public TaskLinkBag(Bag<TaskLink, TaskLink> activates) {
        super(activates, new TaskLinkBuffer(NAL.tasklinkMerge));
    }

    public String id(boolean in, boolean out) {
        if(out && !in)
            return id;
        else
            throw new TODO();
    }

    private static class TaskLinkBuffer extends PriMap<TaskLink> {

        TaskLinkBuffer(PriMerge merge) {
            super(merge);
        }

        @Override
        protected float merge(Prioritizable existing, TaskLink incoming, float pri, PriMerge merge) {
            return ((TaskLink) existing).merge(incoming, merge, PriReturn.Delta);
        }
    }



    private static final AtomicInteger ids = new AtomicInteger(0);

    /** each tasklink will have a unique id, allowing Atom concepts to store TangentConcepts instances from multiple TaskLink bags, either from the same or different NAR */
    public final String id = /*TaskLinkBag.class.getSimpleName() */ "T" + ids.incrementAndGet();

    static class TaskLinkArrayBag extends ArrayBag<TaskLink, TaskLink> {

        public TaskLinkArrayBag(int initialCapacity, PriMerge merge) {
            super(merge, initialCapacity, PriMap.newMap(false));
        }

        @Override
        protected float merge(TaskLink existing, TaskLink incoming, float incomingPri) {
            return existing.merge(incoming, merge(), PriReturn.Overflow);
        }
        //        @Override
//        protected float sortedness() {
//            return 0.33f;
//        }

        @Override
        public TaskLink key(TaskLink value) {
            return value;
        }

    }
}
