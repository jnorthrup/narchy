package nars.concept.action.curiosity;

import jcog.Util;
import nars.$;
import nars.concept.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

final class UniformRandomCuriosity extends CuriosityMode {

    @Override
    public @Nullable Truth get(AbstractGoalActionConcept action, Curiosity curiosity) {
        float u = curiosity.game.nar().random().nextFloat();
        float uu = Util.round(u, action.resolution().floatValue());
        return $.t(uu, curiosity.conf.floatValue());
    }
}
