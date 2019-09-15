package nars.game.action.curiosity;

import jcog.Util;
import nars.game.action.AbstractGoalActionConcept;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

final class UniformRandomCuriosity extends CuriosityMode {

    @Override
    public @Nullable Truth get(AbstractGoalActionConcept action, Curiosity curiosity) {
        float u = curiosity.game.nar().random().nextFloat();
        float uu = Util.round(u, action.resolution().floatValue());
        return PreciseTruth.byEvi(uu, curiosity.evi.floatValue());
    }
}
