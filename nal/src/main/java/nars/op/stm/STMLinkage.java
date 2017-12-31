package nars.op.stm;

import jcog.Util;
import jcog.math.FloatParam;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Cause;
import nars.control.TaskService;
import nars.link.CauseLink;
import nars.link.Tasklinks;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;


/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMLinkage extends TaskService {

    public final BlockingQueue<Task> stm;

    final FloatParam strength = new FloatParam(1f, 0f, 1f);
    private final boolean allowNonInput;
    private final Cause cause;


    public STMLinkage(@NotNull NAR nar, int capacity, boolean allowNonInput) {
        super(nar);

        this.allowNonInput = allowNonInput;
        strength.set(1f / capacity);

        stm = Util.blockingQueue(capacity + 1 /* extra slot for good measure */);
//        for (int i = 0; i < capacity+1; i++)
//            stm.add(null); //fill with nulls initially

        cause = nar.newCause(Cause::new);
    }

    boolean stmLinkable(Task newEvent) {
        return (!newEvent.isEternal() && (allowNonInput || newEvent.isInput()));
    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(NAR nar, Task t) {

        if (!t.isBeliefOrGoal() || !stmLinkable(t))
            return;

        float strength = this.strength.floatValue();
        float tPri = t.priElseZero();

//        short cid = cause.id;
        float p = strength * tPri;
        for (Task u : stm) {
            if (u == null) continue; //skip null's and dummy's
            link(t, p * u.priElseZero(), u, cause.id, nar);
        }

        stm.poll();
        stm.offer(t);
    }


    protected static void link(Task ta, float pri, Task tb, short cid, NAR nar) {


        /** current task's... */
        float interStrength = pri;
        if (interStrength >= Prioritized.EPSILON) {
            Concept ca = ta.concept(nar, true);
            if (ca != null) {
                Concept cb = tb.concept(nar, true);
                if (cb != null) {
                    if (!cb.equals(ca)) { //null or same concept?

                        //TODO handle overflow?
                        cb.termlinks().putAsync(new CauseLink.PriCauseLink(ca.term(), interStrength, cid));
                        ca.termlinks().putAsync(new CauseLink.PriCauseLink(cb.term(), interStrength, cid));

//                        //tasklinks, not sure:
//                        Tasklinks.linkTask(ta, interStrength, cb);
//                        Tasklinks.linkTask(tb, interStrength, ca);
                    } else {
                        //create a self-termlink
                        ca.termlinks().putAsync(new CauseLink.PriCauseLink(ca.term(), interStrength, cid));
                    }
                }
            }
        }
    }
}