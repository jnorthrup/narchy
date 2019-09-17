package nars.op.stm;

import jcog.math.FloatRange;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.concept.Concept;
import nars.control.NARPart;
import nars.link.AtomicTaskLink;
import org.eclipse.collections.api.tuple.Pair;
import org.jctools.queues.MpmcArrayQueue;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;

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

    private final MpmcArrayQueue<Pair<Task, Concept>> stm;
    private final int capacity;

    public STMLinkage(NAR nar, int capacity) {
        this(capacity);
        nar.start(this);
    }

    private STMLinkage(int capacity) {
        super();

        this.capacity = capacity;
        stm = //  new MetalConcurrentQueue<>(capacity);
                new MpmcArrayQueue<>(Math.max(2, capacity));
        for (int i = 0; i < capacity; i++)
            stm.offer(pair(null,null));

//        cause = nar.newCause(this);
    }

    @Override
    protected void starting(NAR nar) {
        on(
            nar.onTask(this::accept, BELIEF, GOAL),
            nar.eventClear.on(stm::clear)
        );
    }

    public static void link(Task at, Concept ac, Pair<Task, Concept> b/*, short cid*/, float factor, NAR nar) {

        Concept bc = b.getTwo();
        if (ac == bc || bc == null)
            return;

        link(ac, b.getOne(), factor, nar);
        link(bc, at, factor, nar);
    }

    static void link(Concept target, Task task, float factor, NAR nar) {
        ((TaskLinkWhat) nar.what()).links.link(AtomicTaskLink.link(task.term(), target.term()).priSet(task.punc(), task.priElseZero() * factor));
    }


    public boolean keep(Task x) {
        return x.isInput();
    }

    public boolean filter(Task x) {
        return x.isInput();
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

            if (capacity == 1) {
                //optimized 1-ary case
                Pair<Task, Concept> z = stm.peek();
                link(y, yc, z, factor, nar);
            } else {
                int i = 0;
                for (Pair<Task, Concept> z : stm) {
                    link(y, yc, z, factor, nar);
                    if (++i == capacity) break;
                }
            }

            if (keep(y)) {
                stm.poll();
                if (!stm.offer(pair(y, yc))) {
                    if (NAL.DEBUG)
                        throw new BufferOverflowException();
                }
            }
        }


    }
}