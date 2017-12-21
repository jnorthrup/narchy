package nars.concept;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.signal.Signal;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * Action Concept which acts on belief state directly (no separate feedback involved)
 */
public class BeliefActionConcept extends ActionConcept {



    private final Consumer<Truth> action;
    private final Signal feedback;

    private final float curiosity = 0.1f;


    public BeliefActionConcept(@NotNull Term term, @NotNull NAR n, Consumer<Truth> action) {
        super(term, n);

        this.feedback = new Signal(BELIEF, resolution).pri(() -> n.priDefault(BELIEF));

        this.action = action;
    }

//    @Override
//    public @Nullable Task curiosity(float conf, long next, NAR nar) {
//        return ActionConcept.curiosity(term(), BELIEF, conf, next, nar);
//    }

    @Override
    public Stream<ITask> update(long now, int dur, NAR nar) {

        long nowStart = now;
        long nowEnd = now + dur;

        Truth belief;
        if (nar.random().nextFloat() < curiosity) {
            float f = nar.random().nextFloat();
            float c = nar.confDefault(BELIEF);
            nar.believe(term(), Tense.Present, f, c);
            belief = $.t(f, c);
        } else {

            belief =
                    this.beliefs().truth(nowStart, nowEnd, nar);
            //beliefIntegrated.commitAverage();
        }

//        Truth goal =
//                this.goals().truth(nowStart, nowEnd, nar);
////                //goalIntegrated.commitAverage();
//        if (goal!=null) {
//            if (belief!=null)
//                belief = Revision.revise(belief, goal,1f, 0f);
//            else
//                belief = goal;
//
//        }

        Task x;
        if (belief!=null) {
            x = feedback.set(this, belief, nar.time::nextStamp, nowStart, dur, nar);
        } else {
            x = feedback.get();
        }

        action.accept(x == null ? null : x.truth());

        return Stream.of(x);
    }


}
