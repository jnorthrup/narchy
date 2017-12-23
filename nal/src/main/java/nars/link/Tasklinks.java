package nars.link;

import jcog.bag.Bag;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.op.PriForget;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.TemporalBeliefTable;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static nars.truth.TruthFunctions.w2cSafe;

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
    public static void linkTask(Task t, float _pri, /*Task*/Concept cc, @Nullable NAR nar) {

        final float inPri = Math.max(_pri, Pri.EPSILON);

        boolean activate = nar!=null;

        MutableFloat overflow = activate ? new MutableFloat() : null;
        linkTask(t, inPri, cc.tasklinks(), overflow);

        if (activate) {
            float o = overflow.floatValue();
            assert(o >= 0);
            float efPri = Math.max(0, inPri - o); //efective priority between 0 and pri


            {

                //activation is the ratio between the effective priority and the input priority, a value between 0 and 1.0
                //it is a measure of the 'novelty' of a task as reduced by the priority of an equivalent existing tasklink

                float activation = inPri > Float.MIN_NORMAL ? efPri / inPri : 0;
                if (activation >= Param.ACTIVATION_THRESHOLD)
                    nar.eventTask.emit(t);

                if (activation > Float.MIN_NORMAL)
                    ((TaskConcept) cc).value(t, activation, nar);
            }

            nar.emotion.onActivate(t);

            float conceptActivation = efPri * nar.amp(t.cause());
            if (conceptActivation > 0) {
                nar.activate(cc, conceptActivation);
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
        private final int total;

        public TaskLinkForget(float r, long now, int dur, int total) {
            super(r);
            this.now = now;
            this.dur = dur;
            this.total = Math.max(1, total);
        }

        @Override
        public void accept(PriReference<Task> b) {
            Task t = b.get();
            float rate =
                  t.isBeliefOrGoal() ?
                        1f - w2cSafe(TemporalBeliefTable.temporalTaskPriority(t, now, now, dur))/total :
                        1f;
            b.priSub(priRemoved * rate);
        }
    }
}
