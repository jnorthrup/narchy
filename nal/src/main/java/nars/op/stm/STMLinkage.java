package nars.op.stm;

import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.NARService;
import nars.task.proxy.SpecialOccurrenceTask;
import nars.term.Termed;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public class STMLinkage extends NARService {

    public final MetalConcurrentQueue<Task> stm;

    final FloatRange strength = new FloatRange(1f, 0f, 1f);

    private boolean eternalize = true;

//    private final Cause cause;


    public STMLinkage(NAR nar, int capacity) {
        super();


        strength.set(1f / capacity);

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

    public static void link(Termed ta, float pri, Termed tb/*, short cid*/, NAR nar) {

        if (ta.term().equals(tb.term()))
            return;

        /** current task's... */
        Concept a = nar.concept(ta);
        if (a != null) {
            Concept b = nar.concept(tb);
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

    public final void accept(Task y) {

        if (target(y)) {

            if (y.isEternal()) {
                if (eternalize) {
                    //project to present moment
                    long now = nar.time();
                    int dur = nar.dur();
                    y = new SpecialOccurrenceTask(y, now - dur / 2, now + dur / 2);
                } else {
                    return;
                }

            }

            float py = this.strength.floatValue() * y.priElseZero();
            Task yy = y;
            stm.forEach(x -> link(yy,
                    py * x.priElseZero(),
                    x/*, cause.id*/, nar));
        }

        if (hold(y)) {
            if (stm.isFull(1))
                stm.poll();
            stm.offer(y);
        }
    }
}