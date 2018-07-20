package nars.op.stm;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Cause;
import nars.control.TaskService;
import nars.term.Termed;


/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMLinkage extends TaskService {

    public final MetalConcurrentQueue<Task> stm;

    final FloatRange strength = new FloatRange(1f, 0f, 1f);
    private final boolean allowNonInput;
    private final Cause cause;


    public STMLinkage(NAR nar, int capacity, boolean allowNonInput) {
        super(nar);

        this.allowNonInput = allowNonInput;
        strength.set(1f / capacity);

        stm = //Util.blockingQueue(capacity + 1 );
                new MetalConcurrentQueue<>(capacity+1 /* extra slot for good measure */);



        cause = nar.newCause(this);
    }

    public static void link(Termed ta, float pri, Termed tb/*, short cid*/, NAR nar) {


        /** current task's... */
        Concept ca = nar.conceptualize(ta);
        if (ca != null) {
            Concept cb = nar.conceptualize(tb);
            if (cb != null) {
                if (!cb.equals(ca)) { 

                    
                    cb.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink(ca.term(), pri/*, cid*/));
                    ca.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink(cb.term(), pri/*, cid*/));
                    //ca.termlinks().putAsync(new CauseLink.PriCauseLink(cb.term(), pri, cid));




                } else {
                    ca.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink(ca.term(), pri/*, cid*/));
                    //ca.termlinks().putAsync(new CauseLink.PriCauseLink(ca.term(), pri, cid));
                }
            }
        }

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


        float p = strength * tPri;
        stm.forEach(u -> link(t, p * u.priElseZero(), u/*, cause.id*/, nar));

        stm.poll();
        stm.offer(t);
    }
}