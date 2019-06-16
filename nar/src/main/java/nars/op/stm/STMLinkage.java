package nars.op.stm;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.concept.Concept;
import nars.control.NARPart;
import nars.link.AtomicTaskLink;
import nars.task.proxy.ImageTask;
import nars.term.util.Image;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * Short-target Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 *
 * TODO make this 'What'-local
 */
public class STMLinkage extends NARPart {


    public final FloatRange strength = new FloatRange(0.5f, 0f, 1f);

//    private final Cause cause;

    private final MetalConcurrentQueue<Pair<Task, Concept>> stm;

    @Deprecated
    public STMLinkage(NAR nar, int capacity) {
        this(capacity);
        nar.start(this);
    }

    public STMLinkage(int capacity) {
        super();

        stm = //Util.blockingQueue(capacity + 1 );
                new MetalConcurrentQueue<>(capacity);

//        cause = nar.newCause(this);
    }

    @Override
    protected void starting(NAR nar) {
        whenDeleted(
            nar.onTask(this::accept, BELIEF, GOAL),
            nar.eventClear.on((Runnable)stm::clear)
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
        ((TaskLinkWhat) nar.what()).links.link(new AtomicTaskLink(task.term(), target.term()).priSet(task.punc(), task.priElseZero() * factor));
    }


    public boolean keep(Task x) {
        return x.isInput();
    }

    public boolean filter(Task x) {
        return x.isInput() && !(x instanceof ImageTask);
    }


    public final void accept(Task _y) {


        if (filter(_y)) {
            Task y = Image.imageNormalizeTask(_y);

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
            stm.forEach(z -> link(y, yc, z, factor, nar));

            if (keep(y)) {
                if (stm.isFull(1))
                    stm.poll();

                stm.offer(pair(y, yc));
            }
        }


    }
}