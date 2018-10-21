package nars.concept.action.curiosity;

import nars.concept.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

final class NullCuriosity extends CuriosityMode {

    @Override
    public @Nullable Truth get(AbstractGoalActionConcept action, Curiosity curiosity) {
        return null;
    }
}
