package nars.concept.action.curiosity;

import jcog.math.FloatRange;
import nars.concept.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

abstract public class CuriosityMode {

    /** relative proportion used in roulette select */
    public final FloatRange weight = new FloatRange(0, 0, 1);

    /** fabricated goal truth overriding motor output, or null if not curious */
    @Nullable
    abstract public Truth get(AbstractGoalActionConcept action, Curiosity curiosity);

    public final CuriosityMode weight(float weight) {
        this.weight.set(weight);
        return this;
    }
}
