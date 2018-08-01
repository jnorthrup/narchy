package nars.agent;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;

public abstract class Reward implements Runnable {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    protected final NAgent agent;
    private final FloatSupplier rewardFunc;

    protected transient volatile float reward = Float.NaN;

    public Reward(NAgent a, FloatSupplier r) {
    //TODO
    //public Reward(NAgent a, FloatSupplier r, float confFactor) {
        this.agent = a;
        this.rewardFunc = r;
    }

    public NAR nar() { return agent.nar(); }

    @Override
    public void run() {
        reward = rewardFunc.asFloat();
    }

    protected FloatFloatToObjectFunction<Truth> truther() {
        return (prev, next) -> (next == next) ?
                $.t(Util.unitize(next), nar().confDefault(BELIEF)) : null;
    }

    public float summary() {
        return reward;
    }
}
