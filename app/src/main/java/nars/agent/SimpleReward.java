package nars.agent;

import jcog.math.FloatSupplier;
import nars.concept.sensor.Signal;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.GOAL;

public class SimpleReward extends Reward {

    public final Signal concept;

    private final FloatFloatToObjectFunction<Truth> truther;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        concept = new Signal(id, () -> reward, nar());
        truther = truther();
        agent.alwaysWantEternally(concept.term, nar().confDefault(GOAL));
    }

    @Override
    public void run() {
        super.run();
        concept.update(agent.last, agent.now(), truther, nar());
    }
}
