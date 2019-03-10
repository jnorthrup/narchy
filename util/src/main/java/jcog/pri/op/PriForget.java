package jcog.pri.op;

import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * decreases priority at a specified rate which is diminished in proportion to a budget's quality
 * so that high quality results in slower priority loss
 */
public enum PriForget { ;

    public static final class PriMult<P extends Prioritizable> implements Consumer<P> {

        public static final float FORGET_TEMPERATURE_DEFAULT = 1f;

        public final float mult;

        public PriMult(float factor) {
            this.mult = factor;
        }

        @Override
        public void accept(P x) {
            x.priMult(mult);
        }
    }


    /**
     *
     * @param pressure bag pressure released
     * @param mass bag mass remaining
     * @param size items
     * @param cap  item capacity
     *
     * @param temperature decay rate per item, opposite of elitism/retention. any non-negative value
     * @param leak in percentage rate per item. any non-negative value
     *         TODO until time is considered, use only very small values for this like 0.001
     */
    @Nullable public static Consumer<? extends Prioritizable> forgetPressure(float temperature, float leak, int size, int cap, float pressure, float mass) {

        if (pressure > Float.MIN_NORMAL) {



            //float decayRate = temperature * Util.unitize(pressure / (pressure + mass));
            //float decayRate = temperature * Util.unitize(pressure / (pressure + mass));
            float decayRate = pressure * temperature / mass;

            float factor = leak + decayRate;

            //Util.unitize(pressure * temperature / mass);

            if (factor > cap * ScalarValue.EPSILON) {
                return new PriMult<>(1-decayRate);
            }
        }
        return null;
    }

    @Nullable
    public static Consumer<? extends Prioritizable> forgetIdeal(float rate, float idealPri, int size, int cap, float pressure, float mass) {
        float excess = pressure +
                Math.max(0,
                    mass - (size * idealPri)
                )
        ;
        float eachMustForgetPct =
                rate * (excess / (excess + mass));

            if (eachMustForgetPct * mass / size > ScalarValue.EPSILONsqrt) {
                return new PriMult<>(1-eachMustForgetPct);
            }

        return null;
    }


}