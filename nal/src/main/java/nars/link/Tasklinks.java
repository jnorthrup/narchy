package nars.link;

import jcog.bag.Bag;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.op.PriForget;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.TemporalBeliefTable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class Tasklinks {

    public static void linkTask(Task t, Concept cc, NAR nar) {
        float p = t.pri();
        if (p == p)
            linkTask(t, p, cc, nar);
    }


    public static void linkTask(Task t, float pri, Concept cc) {
        linkTask(t, pri, cc, null);
    }

    public static void linkTask(Task x, float p, Bag b) {
        linkTask(x, p, b, null);
    }

    public static void linkTask(Task x, float p, Bag<Task,PriReference<Task>> b, @Nullable MutableFloat overflow) {
        PLinkUntilDeleted<Task> l = new PLinkUntilDeleted<>(x, p);
        if (overflow!=null)
            b.put(l, overflow);
        else
            b.putAsync(l);
    }

    /** if NAR is null, then only inserts tasklink.  otherwise it proceeds with activation */
    public static void linkTask(Task t, float pri, /*Task*/Concept cc, @Nullable NAR nar) {

        boolean activate = nar!=null;

        MutableFloat overflow = activate ? new MutableFloat() : null;
        linkTask(t, pri, cc.tasklinks(), overflow);

        if (activate) {
            pri -= overflow.floatValue();

            if (pri >= Prioritized.EPSILON_VISIBLE) {
                nar.eventTask.emit(t);
            }

            float conceptActivation = pri;
            if (conceptActivation > 0) {
                nar.activate(cc, conceptActivation);
                nar.emotion.onActivate(t, conceptActivation, (TaskConcept) cc, nar);
            }
        }
    }

    public static void linkTask(Task task, Collection<Concept> targets) {
        int numSubs = targets.size();
        if (numSubs == 0)
            return;

        float tfa = task.priElseZero();
        float tfaEach = tfa / numSubs;


        for (Concept target : targets) {

            linkTask(task, tfaEach, target);
//                target.termlinks().putAsync(
//                        new PLink(task.term(), tfaEach)
//                );


        }
    }

    public static class TaskLinkForget extends PriForget<PriReference<Task>> {
        private final long now;
        private final int dur;

        public TaskLinkForget(float r, long now, int dur) {
            super(r);
            this.now = now;
            this.dur = dur;
        }

        @Override
        public void accept(PriReference<Task> b) {
            Task t = b.get();
            float rate =
                  t.isBeliefOrGoal() ?
                        1f - TemporalBeliefTable.temporalTaskPriority(t, now, now, dur) :
                        1f;
            b.priSub(priRemoved * rate);
        }
    }
}
