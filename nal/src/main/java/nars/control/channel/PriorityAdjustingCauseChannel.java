package nars.control.channel;

import jcog.pri.Prioritizable;
import nars.control.Cause;

import java.util.function.Consumer;

public class PriorityAdjustingCauseChannel<X extends Prioritizable> extends CauseChannel<X> {

    /**
     * linear gain control
     */
    public float preBias, preAmp = 1;

    private final Consumer<X> target;

    public PriorityAdjustingCauseChannel(Cause cause, Consumer<X> target) {
        super(cause);
        this.target = target;
    }

    /*public PriorityAdjustingCauseChannel(short id, Object idObj, Consumer<X> target) {
        super(id, target);
    }*/

    @Override public void input(X x) {

        

        float p = x.pri();
        if (p == p && (preBias != 0 || preAmp != 1)) {
            x.pri(preBias + p * preAmp);
        }

        target.accept(x);
    }

    public PriorityAdjustingCauseChannel preBias(float bias) {
        this.preBias = bias;
        return this;
    }

    public PriorityAdjustingCauseChannel preAmp(float amp) {
        this.preAmp = amp;
        return this;
    }

    public PriorityAdjustingCauseChannel pre(float bias, float amplitude) {
        return preAmp(amplitude).preBias(bias);
    }

}
