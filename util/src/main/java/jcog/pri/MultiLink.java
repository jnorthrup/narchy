package jcog.pri;

import jcog.Util;
import jcog.decide.Roulette;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/** groups a set of items into one link which are selected from
 * on .get() invocations.
 * a transducer is applied so that a type transformation
 * of the input can allow shared inputs to multiple different link output types.
 */
public class MultiLink<X extends Prioritized,Y> extends AbstractPLink<Y> {

    private final int hash;
    private final X[] x;
    private final Function<X, Y> transduce;

    public MultiLink(@NotNull X[] x, Function<X,Y> transduce, float pSum) {
        super(pSum / x.length);
        this.x = x;
        this.transduce = transduce;
        hash = Util.hashCombine(transduce.hashCode(), Arrays.hashCode(x));
    }

    @Override
    public float priAdd(float a) {
        return super.priAdd(a/x.length);
    }

    @Override
    public float priMult(float factor) {
        return super.priMult( Util.lerp(1f/x.length, 1, factor));
    }

    @Override
    public void priMax(float max) {
        super.priMax( Util.lerp(1/x.length, priElseZero(), max));
    }

    @Override
    public void priMin(float min) {
        super.priMin( Util.lerp(1/x.length, priElseZero(), min));
    }


    @Override
    public final boolean equals(@NotNull Object that) {
        return this == that || hash==that.hashCode() || (that instanceof MultiLink && ((MultiLink)that).transduce.equals(transduce) && Arrays.equals(x, ((MultiLink)that).x));
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Nullable
    @Override
    public Y get() {
        float priSum = 0;
        int deleted = 0;
        int c = x.length;
        float[] pri = new float[c];
        for (int i = 0; i < c; i++) {
            float p = x[i].pri();
            if (p!=p) {
                deleted++;
                p = 0;
            } else {
                priSum += p;
            }
            pri[i] = p;
        }
        if (deleted == c) {
            super.delete();
            return null;
        }

        int s = Roulette.decideRoulette(c, i -> pri[i], ThreadLocalRandom.current());
        return transduce.apply(x[s]);
    }

    @Override
    public boolean delete() {
        return false;
    }

}
