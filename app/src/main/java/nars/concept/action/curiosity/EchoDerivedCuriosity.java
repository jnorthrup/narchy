package nars.concept.action.curiosity;

import nars.concept.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** echos the last derived goal, if one exists */
final class EchoDerivedCuriosity extends CuriosityMode {

    @Override
    public @Nullable Truth get(AbstractGoalActionConcept action, Curiosity curiosity) {

        return action.curiDex;
    }
}
