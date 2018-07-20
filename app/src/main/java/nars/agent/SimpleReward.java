package nars.agent;

import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import nars.$;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class SimpleReward extends Reward {

    public final Signal concept;
    private final FloatFloatToObjectFunction<Truth> truther;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        concept = new Signal(id, new FloatNormalized(
            () -> reward, -1, +1, true), nar());
        truther = ((prev, next) -> next == next ? $.t(next, nar().confDefault(BELIEF)) : null);
        agent.alwaysWantEternally(concept.term, nar().confDefault(GOAL));
    }

    @Override
    public void run() {
        super.run();
        concept.update(agent.last, agent.now(), truther, nar());
    }
}
