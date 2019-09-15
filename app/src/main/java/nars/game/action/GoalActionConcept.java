package nars.game.action;

import nars.NAR;
import nars.game.Game;
import nars.term.Term;
import nars.truth.Truth;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends AbstractGoalActionConcept {

    private final MotorFunction motor;

    public GoalActionConcept(Term term, MotorFunction motor, NAR n) {
        super(term, n);

        this.motor = motor;
    }

    @Override
    public void update(Game g) {

        super.update(g);

        Truth goal = actionTruth;

        Truth fb = this.motor.apply(null,
                //curi!=null ? (goal!=null ? Revision.revise(curi, goal) : curi) : goal
                goal
        );

        feedback(fb, causeArray, g);
    }




}
