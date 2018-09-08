package nars.op.stm;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.TaskService;
import nars.term.Termed;


/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public class STMLinkage extends TaskService {

    public final MetalConcurrentQueue<Task> stm;

    final FloatRange strength = new FloatRange(1f, 0f, 1f);
//    private final Cause cause;


    public STMLinkage(NAR nar, int capacity) {
        super(nar);

        strength.set(1f / capacity);

        stm = //Util.blockingQueue(capacity + 1 );
                new MetalConcurrentQueue<>(capacity);

//        cause = nar.newCause(this);
    }

    public static void link(Termed ta, float pri, Termed tb/*, short cid*/, NAR nar) {


        /** current task's... */
        Concept a = nar.conceptualize(ta);
        if (a != null) {
            Concept b = nar.conceptualize(tb);
            if (b != null) {
                if (a!=b) {


                    b.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink(a.term(), pri/*, cid*/));
                    a.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink(b.term(), pri/*, cid*/));
                    //ca.termlinks().putAsync(new CauseLink.PriCauseLink(cb.term(), pri, cid));




                } else {
                    a.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink<>(a.term(), pri/*, cid*/));
                    //ca.termlinks().putAsync(new CauseLink.PriCauseLink(ca.term(), pri, cid));
                }
            }
        }

    }


    public boolean hold(Task x) {
        return x.isInput();
    }

    public boolean target(Task x) {
        return x.isInput();
    }

    @Override
    public void clear() {
        stm.clear();
    }

    @Override
    public final void accept(NAR nar, Task t) {

        if (!t.isBeliefOrGoal() || t.isEternal())
            return;


        if (target(t)) {
            float strength = this.strength.floatValue();
            float p = strength * t.priElseZero();
            stm.forEach(u -> link(t, p * u.priElseZero(), u/*, cause.id*/, nar));
        }

        if (hold(t)) {
            if (stm.isFull(1))
                stm.poll();
            stm.offer(t);
        }
    }
}