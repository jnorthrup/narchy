package jcog.pri.op;

import jcog.pri.Prioritizable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public class PriForget<P extends Prioritizable> implements Consumer<P> {

    public static final float FORGET_TEMPERATURE_DEFAULT = 1f;

    final float mult;

    public PriForget(float pctToRemove) {
        this.mult = 1 - pctToRemove;
    }

    @Override
    public void accept(P x) {
        x.priMult(mult);
    }

}