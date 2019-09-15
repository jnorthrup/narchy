package nars.game.action.curiosity;

import nars.game.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

final class EchoNegatedDerivedCuriosity extends CuriosityMode {

    @Override
    public @Nullable Truth get(AbstractGoalActionConcept action, Curiosity curiosity) {
        Truth t = action.lastNonNullActionDex;
        return t != null ? t.neg() : null;
    }
}
