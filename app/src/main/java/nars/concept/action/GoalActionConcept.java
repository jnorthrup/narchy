package nars.concept.action;

import nars.NAR;
import nars.term.Term;
import nars.truth.Truth;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends AbstractGoalActionConcept {

    private final MotorFunction motor;

    public GoalActionConcept(Term term, NAR n, MotorFunction motor) {
        super(term,
                n);


        this.motor = motor;

    }

    @Override
    public void sense(long prev, long now, NAR n) {

        super.sense(prev, now, n);

        Truth goal = actionTruth;

        Truth fb = this.motor.apply(null,
                //curi!=null ? (goal!=null ? Revision.revise(curi, goal) : curi) : goal
                goal
        );

        in.input(
            feedback(fb, prev, now, n)
        );

    }




}
