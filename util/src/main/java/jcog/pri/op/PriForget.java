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
public enum PriForget { ;

    public static final class PriMult<P extends Prioritizable> implements Consumer<P> {

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
    public static @Nullable Consumer<? extends Prioritizable> forgetPressure(float temperature, float leak, int size, int cap, float pressure, float mass) {

        if (pressure > Float.MIN_NORMAL) {



            //float decayRate = temperature * Util.unitize(pressure / (pressure + mass));
            //float decayRate = temperature * Util.unitize(pressure / (pressure + mass));
            float decayRate = pressure * temperature / mass;

            float factor = leak + decayRate;

            //Util.unitize(pressure * temperature / mass);

            if (factor > (float) cap * ScalarValue.Companion.getEPSILON()) {
                return new PriMult<>(1.0F -decayRate);
            }
        }
        return null;
    }

    public static @Nullable Consumer forgetIdeal(double rate, double idealPri, int size, int cap, double pressure, double mass) {
        double excess = pressure +
                Math.max((double) 0,
                    mass - ((double) cap /*size*/ * idealPri)
                )
        ;
        double eachMustForgetPct =
                rate * (excess / (mass+excess));

            if (eachMustForgetPct >= (double) ScalarValue.Companion.getEPSILON()) {
                return new PriMult<>((float)Util.unitize(1.0 -eachMustForgetPct));
            }

        return null;
    }


}