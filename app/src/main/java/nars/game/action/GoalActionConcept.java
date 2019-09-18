package nars.game.action;

import nars.NAR;
import nars.game.Game;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


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
    protected @Nullable Truth updateAction(@Nullable Truth beliefTruth, @Nullable Truth actionTruth,Game g) {
        return this.motor.apply(beliefTruth, actionTruth);
    }

}
