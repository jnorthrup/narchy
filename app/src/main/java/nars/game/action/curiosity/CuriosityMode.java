package nars.game.action.curiosity;

import jcog.pri.UnitPri;
import nars.game.action.AbstractGoalActionConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** priority corresponds to the relative proportion used in roulette select */
abstract public class CuriosityMode extends UnitPri {

    public CuriosityMode() {
        this(0);
    }

    public CuriosityMode(float pri) {
        super(pri);
    }

    /** fabricated goal truth overriding motor output, or null if not curious */
    @Nullable
    abstract public Truth get(AbstractGoalActionConcept action, Curiosity curiosity);

}
