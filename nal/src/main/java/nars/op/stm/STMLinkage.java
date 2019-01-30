package nars.op.stm;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.NARService;
import nars.link.TaskLink;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * Short-target Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public class STMLinkage extends NARService {


    public final FloatRange strength = new FloatRange(0.5f, 0f, 1f);

//    private final Cause cause;

    private final MetalConcurrentQueue<Pair<Task, Concept>> stm;

    public STMLinkage(NAR nar, int capacity) {
        super();

        stm = //Util.blockingQueue(capacity + 1 );
                new MetalConcurrentQueue<>(capacity);

//        cause = nar.newCause(this);

        nar.on(this);
    }

    @Override
    protected void starting(NAR nar) {
        on(
                nar.onTask(this::accept, BELIEF, GOAL)
        );
    }

    public static void link(Task at, Concept ac, Pair<Task, Concept> b/*, short cid*/, float factor, NAR nar) {

        //if (a==b) ta.target().equals(tb.target()))
        //return;

        /** current task's... */
        //Concept a = nar.concept(ta);
        //if (a != null) {
        //Concept b = nar.concept(tb);
        //if (b != null) {
        Concept bc = b.getTwo();
        if (ac != bc) {


            Task bt = b.getOne();
            link(ac, bt, factor, nar);
            link(bc, at, factor, nar);

        }

//                } else {
//                    a.termlinks().putAsync(/*new CauseLink.PriCauseLink*/new PLink<>(a.target(), pri/*, cid*/));
//                    //ca.termlinks().putAsync(new CauseLink.PriCauseLink(ca.target(), pri, cid));
//                }
        //  }
        // }

    }

    static void link(Concept target, Task task, float factor, NAR nar) {
        TaskLink.link(TaskLink.tasklink(target.term(), task,true,true, task.priElseZero() * factor, nar), nar, null);
    }


    public boolean keep(Task x) {
        return x.isInput();
    }

    public boolean filter(Task x) {
        return x.isInput();
    }

    @Override
    public void clear() {
        stm.clear();
    }

    public final void accept(Task y) {


        if (filter(y)) {
            @Nullable Concept yc;
            yc = nar.concept(y);
            if (yc == null)
                return;

//            if (y.isEternal()) {
//                if (eternalize) {
//                    //project to present moment
//                    long now = nar.time();
//                    int dur = nar.dur();
//                    y = new SpecialOccurrenceTask(y, now - dur / 2, now + dur / 2);
//                } else {
//                    return;
//                }
//
//            }


            float factor = this.strength.floatValue();
            stm.forEach(x -> link(y, yc, x, factor, nar));

            if (keep(y)) {
                if (stm.isFull(1))
                    stm.poll();

                stm.offer(pair(y, yc));
            }
        }


    }
}