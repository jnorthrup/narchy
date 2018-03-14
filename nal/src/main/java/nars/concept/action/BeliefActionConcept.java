package nars.concept.action;

import nars.NAR;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Action Concept which acts on belief state directly (no separate feedback involved)
 */
public class BeliefActionConcept extends ActionConcept {

    private final Consumer<Truth> action;

    //private final Signal feedback;

    //private final float curiosity = 0.1f;


    public BeliefActionConcept(@NotNull Term term, @NotNull NAR n, Consumer<Truth> action) {
        super(term, n);

        //this.feedback = new Signal(BELIEF, resolution).pri(() -> n.priDefault(BELIEF));

        this.action = action;
    }

//    @Override
//    public @Nullable Task curiosity(float conf, long next, NAR nar) {
//        return ActionConcept.curiosity(term(), BELIEF, conf, next, nar);
//    }

    @Override
    public Stream<ITask> update(long start, long end, int dur, NAR nar) {

        long nowStart =
                //now;
                start - dur/2;
        long nowEnd =
                //now + dur;
                start + dur/2;

        Truth belief = this.beliefs().truth(nowStart, nowEnd, nar);

//        if (nar.random().nextFloat() < curiosity) {
//            float f = nar.random().nextFloat();
//            float c = nar.confDefault(BELIEF);
//            nar.believe(term(), Tense.Present, f, c);
//            belief = $.t(f, c);
//        } else {


            //beliefIntegrated.commitAverage();
//        }

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

        //Task x;
        //if (belief!=null) {
            //x = feedback.set(this, belief, nar.time::nextStamp, nowStart, dur, nar);
//        } else {
//            x = feedback.get(); //latch
//        }

        action.accept(belief == null ? null : belief.truth());

        return Stream.empty();
    }


}
