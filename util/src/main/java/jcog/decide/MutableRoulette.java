package jcog.decide;

import jcog.Util;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Random;
import java.util.function.IntPredicate;

import static java.lang.Float.MIN_NORMAL;

/**
 * efficient embeddable roulette decision executor.
 * takes a weight-update function applied to the current
 * choice after it has been selected in order to influence
 * its probability of being selected again.
 * <p>
 * if the update function returns zero, then the choice is
 * deactivated and can not be selected again.
 * <p>
 * when no choices remain, it exits automatically.
 */
public final class MutableRoulette {

    private final static float EPSILON = MIN_NORMAL;
    /**
     * weights of each choice
     */
    private final float[] w;
    private final Random rng;
    /**
     * weight update function applied between selections to the last selected index's weight
     */
    private final FloatToFloatFunction weightUpdate;
    /**
     * current index (roulette ball position)
     */
    private int i;
    /**
     * current weight sum
     */
    private float weightSum;
    /**
     * current # entries remaining above epsilon threshold
     */
    private int remaining;
    private boolean direction;

    /**
     * with no weight modification
     */
    public MutableRoulette(int count, IntToFloatFunction initialWeights, Random rng) {
        this(count, initialWeights, (x -> x), rng);
    }

    public MutableRoulette(float[] w, FloatToFloatFunction weightUpdate, Random rng) {
        this.w = w;
        this.weightUpdate = weightUpdate;
        this.rng = rng;

        reweigh(null);
    }

    public MutableRoulette(int count, IntToFloatFunction initialWeights, FloatToFloatFunction weightUpdate, Random rng) {
        this.w = new float[count];
        this.weightUpdate = weightUpdate;
        this.rng = rng;

        reweigh(initialWeights);
    }

    public MutableRoulette reweigh(@Nullable IntToFloatFunction initialWeights) {
        int n = w.length;
        float s = 0;

        final int nn = n;
        for (int i = 0; i < nn; i++) {
            if (initialWeights!=null)
                w[i] = initialWeights.valueOf(i);
            float wi = w[i];
            if (wi < 0 || !Float.isFinite(wi))
                throw new RuntimeException("invalid weight: " + wi);

            if (wi < EPSILON) {
                w[i] = 0;
                n--;
            } else {
                s += wi;
            }
        }

        if (n == 0 || s < n * EPSILON) {
            Arrays.fill(w, 1);
            s = n = w.length;
        }

        int l = w.length;
        if (l > 1 && n > 1) {
            this.direction = rng.nextBoolean();
            this.i = rng.nextInt(l);
        } else {
            this.direction = true;
            this.i = 0;
        }

        this.remaining = n;
        this.weightSum = s;

        return this;
    }


    /**
     * weight array may be modified
     */
    public static void run(float[] weights, Random rng, FloatToFloatFunction weightUpdate, IntPredicate choose) {
        switch (weights.length) {
            case 0:
                throw new UnsupportedOperationException();

            case 1: {
                float theWeight = weights[0];
                while (choose.test(0) && ((theWeight = weightUpdate.valueOf(theWeight)) > EPSILON)) {
                }
                break;
            }

            default: {
                MutableRoulette r = new MutableRoulette(weights, weightUpdate, rng);
                while (r.next(choose)) {
                }
                break;
            }
        }
    }

    public boolean next(IntPredicate select) {
        int n = next();
        return n >= 0 && select.test(n) && remaining > 0;
    }

    public int next() {

        assert (remaining > 0);

        float[] w = this.w;

        int count = w.length;

        if (remaining == 1) {

            for (int x = 0; x < count; x++) {
                float wx = w[x];
                if (wx >= EPSILON) {
                    float wy = weightUpdate.valueOf(wx);
                    if (wx!=wy) {
                        if ((w[x] = wy) < EPSILON) {
                            w[x] = 0;
                            remaining = 0;
                        }
                    }
                    return x;
                }
            }

            throw new RuntimeException();
        } else {

            float distance = rng.nextFloat() * weightSum;


            int i = this.i;
            int idle = 0;
            float wi;
            while ((((wi = w[i = Util.next(i, direction, count)]) > EPSILON) && (distance = (distance - wi)) > EPSILON)) {
                if (idle++ == count + 1)
                    return -1; //emergency bailout: WTF
            }

            float nextWeight = weightUpdate.valueOf(wi);
            if (nextWeight < EPSILON) {
                w[i] = 0;
                weightSum -= wi;
                remaining--;
            } else if (nextWeight != wi) {
                w[i] = nextWeight;
                weightSum += nextWeight - wi;
            }

            return this.i = i;


        }

    }

    /** weight sum */
    public float weightSum() {
        return weightSum;
    }
}
