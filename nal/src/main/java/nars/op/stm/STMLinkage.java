package nars.op.stm;

import jcog.Util;
import jcog.math.FloatParam;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.Tasklinks;
import nars.control.TaskService;
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

    public STMLinkage(@NotNull NAR nar, int capacity) {
        this(nar, capacity, false);
    }

    public STMLinkage(@NotNull NAR nar, int capacity, boolean allowNonInput) {
        super(nar);

        this.allowNonInput = allowNonInput;
        strength.set(1f / capacity);

        stm = Util.blockingQueue(capacity + 1 /* extra slot for good measure */);
//        for (int i = 0; i < capacity+1; i++)
//            stm.add(null); //fill with nulls initially


    }

    static boolean stmLinkable(Task newEvent, boolean allowNonInput) {
        return (!newEvent.isEternal() && (allowNonInput || newEvent.isInput()));
    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(NAR nar, Task t) {

        if (!t.isBeliefOrGoal())
            return;
        if (!STMLinkage.stmLinkable(t, allowNonInput))
            return;

        float strength = this.strength.floatValue();
        float tPri = t.priElseZero();
        if (tPri == 0)
            return;

        float p = strength * tPri;
        for (Task u : stm) {
            if (u == null) continue; //skip null's and dummy's
            link(t, p * u.priElseZero(), u, nar);
        }

        stm.poll();
        stm.offer(t);
    }


    protected static void link(Task ta, float pri, Task tb, NAR nar) {


        /** current task's... */
        float interStrength = pri;
        if (interStrength >= Prioritized.EPSILON) {
            Concept ca = ta.concept(nar, true);
            if (ca != null) {
                Concept cb = tb.concept(nar, true);
                if (cb != null && !cb.equals(ca)) { //null or same concept?

                    //TODO handle overflow?
                    cb.termlinks().putAsync(new PLink(ca.term(), interStrength));
                    ca.termlinks().putAsync(new PLink(cb.term(), interStrength));

                    //tasklinks, not sure:
                    Tasklinks.linkTask(ta, interStrength, cb);
                    Tasklinks.linkTask(tb, interStrength, ca);
                }
            }
        }
    }
}