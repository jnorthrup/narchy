package nars.concept.action;

import nars.NAR;
import nars.Task;
import nars.table.dynamic.SignalBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.func.NALTruth;

import java.util.Objects;
import java.util.stream.Stream;

import static nars.Op.GOAL;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends ActionConcept {


    private final SignalBeliefTable feedback;

    private final MotorFunction motor;

    private volatile float curiosity = 0;


    public GoalActionConcept(Term c, NAR n, MotorFunction motor) {
        super(c, n);


        this.feedback = (SignalBeliefTable) beliefs();


        this.motor = motor;


    }


    @Override
    public Stream<ITask> update(long pPrev, long pNow, NAR nar) {


        Truth goal;

        long gStart, gEnd;
//        gStart = pNow; gEnd = pNow;
//        goal = this.goals().truth(pNow, pNow, nar);
//        if (goal == null) {
//            //HACK expand radius - this should be done by the truthpolation impl

        int dur = nar.dur();
        gStart = pNow - dur / 2; gEnd = pNow + dur / 2;
        //gStart = pNow; gEnd = pNow + Math.max(0, dur-1);

        goal = this.goals().truth(gStart, gEnd, nar);
//        }


        boolean curi;
        if (nar.random().nextFloat() < curiosity) { // * (1f - (goal != null ? goal.conf() : 0))) {


//            float curiConf =
//
//
//                    Math.min(Param.TRUTH_MAX_CONF, nar.confMin.floatValue()
//
//                            * 8
//                    );
//
//
//            curi = true;
//
//
//            goal = Truth.theDithered(nar.random().nextFloat(), c2w(curiConf), nar);

            goal = NALTruth.Curiosity.apply(null, null, nar, 0);
            curi = true;

        } else {
            curi = false;


        }


        Truth belief = this.beliefs().truth(gStart, gEnd, nar);

        Truth feedback = this.motor.apply(belief, goal);

        Task feedbackBelief = feedback != null ?
                this.feedback.add(feedback, gStart, gEnd, this, nar) : null;

        Task curiosityGoal = null;
        if (curi && feedbackBelief != null) {
            curiosityGoal = curiosity(nar,
                    goal,

                    term, gStart, gEnd, nar.time.nextStamp());
        }

        this.feedback.clean(nar);

        return Stream.of(feedbackBelief, (ITask) curiosityGoal).filter(Objects::nonNull);


    }


    static SignalTask curiosity(NAR nar, Truth goal, Term term, long pStart, long pEnd, long curiosityStamp) {
        long now = nar.time();

        SignalTask curiosity = new SignalTask(term, GOAL, goal, now, pStart, pEnd, curiosityStamp);

        curiosity.pri(nar.priDefault(GOAL));


        return curiosity;
    }


    public void curiosity(float c) {
        this.curiosity = c;
    }
}
