package jcog.pri.op;

import jcog.Util;
import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public static Consumer<? extends Prioritizable> forgetPressure(float temperature, int cap, float pressure, float mass) {
        //            float idealPri = 1 - temperature; //headroom median balanced
        //            float totalQuell = (mass + pressure ) - (s * idealPri);
        //            float eachMustForgetPct =
        //                        Util.unitize(totalQuell / s);

        if (pressure > Float.MIN_NORMAL) {

            float eachMustForgetPct =
                    temperature * Util.unitize(pressure / mass);

            //temperature * Util.unitize(pressure / (pressure + mass));
            //Util.unitize(pressure * temperature / mass);

            if (eachMustForgetPct > cap * ScalarValue.EPSILON) {
                return new PriForget<>(eachMustForgetPct);
            }
        }
        return null;
    }

    @Nullable
    public static Consumer<? extends Prioritizable> forgetIdeal(float rate, float idealPri, int size, int cap, float pressure, float mass) {
        float excess = pressure + Math.max(0, mass  - (size * idealPri));
        float eachMustForgetPct =
                rate * Util.unitize(excess / mass);

            if (eachMustForgetPct > cap * ScalarValue.EPSILON) {
                return new PriForget<>(eachMustForgetPct);
            }

        return null;
    }

    @Override
    public void accept(P x) {
        x.priMult(mult);
    }

}