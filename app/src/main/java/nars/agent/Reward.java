package nars.agent;

import jcog.math.FloatSupplier;
import nars.NAR;

public abstract class Reward implements Runnable {

    protected final NAgent agent;
    private final FloatSupplier rewardFunc;

    protected transient volatile float reward = Float.NaN;

    public Reward(NAgent a, FloatSupplier r) {
        this.agent = a;
        this.rewardFunc = r;
    }

    public NAR nar() { return agent.nar(); }

    @Override
    public void run() {
        reward = rewardFunc.asFloat();
    }

}
