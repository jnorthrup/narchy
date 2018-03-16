package nars.link;

import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Priority;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.table.TaskTable;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ByteLongPair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

public interface TaskLink extends Priority, Termed {

    /**
     * resolves a task in the provided NAR
     */
    Task get(NAR n);

    /** after it has been deleted, give an opportunity to re-insert
     * any forwarded task in a bag where it was just removed
     */
    void reincarnate(TaskLinkCurveBag taskLinks);

    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends PLink<Pair<Term, ByteLongPair>> implements TaskLink {

        private final int hash;

//        public GeneralTaskLink(Task template, NAR nar, float pri) {
//            this(template.term().concept(), template.punc(), Tense.dither(template.mid(), nar), pri);
//        }

        public GeneralTaskLink(Task template, float pri) {
            this(
                 template.term().negIf(template.isBeliefOrGoal() && template.isNegative()),
                 template.punc(), template.mid(), pri);
        }

        public GeneralTaskLink(Term t, byte punc, long when, float pri) {
            super(Tuples.pair(t, PrimitiveTuples.pair(punc, when)), pri);
            this.hash = super.hashCode();
        }

        @Override
        public String toString() {
            return toBudgetString() + " " + term() + punc() + ":" + when();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object that) {
            return (this == that) || (that instanceof GeneralTaskLink) &&
                    id.equals(((GeneralTaskLink) that).id);
        }

        public Term term() {
            return id.getOne();
        }

        public byte punc() {
            return id.getTwo().getOne();
        }

        public long when() {
            return id.getTwo().getTwo();
        }

        @Override
        public void reincarnate(TaskLinkCurveBag taskLinks) {
            //TODO
        }

        @Override
        public Task get(NAR n) {

            Term t = term().unneg();
            Concept c = n.concept(t);
            if (c == null) {
                delete();
                return null;
            }
            TaskTable table = c.table(punc());
            Task result = table.match(when(), t, n);
            if (result == null || result.isDeleted())
                return null;
            return result;
        }
    }

    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {

        public DirectTaskLink(Task id, float p) {
            super(id, p);
        }

        @Override
        public Term term() {
            Task t = get();
            if (t == null)
                return null;
            return t.term();
        }

        @Override
        public Task get(NAR n) {
            //TODO may want to check if the task is still active in the NAR
            return get();
        }

        @Override
        public void reincarnate(TaskLinkCurveBag bag) {
            float p = this.priBeforeDeletion;
            if (p == p) {
                // this link was deleted due to the referent being deleted,
                // not because the link was deleted.
                // so see if a forwarding exists

                Task x = this.get();
                Task px = x;
                Task y;

                //TODO maybe a hard limit should be here for safety in case anyone wants to create loops of forwarding tasks
                int hopsRemain = Param.MAX_TASK_FORWARD_HOPS;
                do {
                    y = x.meta("@");
                    if (y != null)
                        x = y;
                } while (y != null && --hopsRemain > 0);

                if (x != px && !x.isDeleted()) {
                    Tasklinks.linkTask(x, p, bag);
                }
            }

        }
    }

}
