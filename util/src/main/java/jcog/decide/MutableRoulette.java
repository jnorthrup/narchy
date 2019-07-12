package jcog.decide;

import jcog.TODO;
import jcog.Util;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

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
public class MutableRoulette {

    private final static float EPSILON = MIN_NORMAL;
    /**
     * weights of each choice
     */
    private float[] w;
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

    private MutableRoulette(int count, IntToFloatFunction initialWeights, FloatToFloatFunction weightUpdate, Random rng) {
        this(Util.map(count, initialWeights), weightUpdate, rng);
    }

    private MutableRoulette(float[] w, FloatToFloatFunction weightUpdate, Random rng) {
        this.rng = rng;

        this.weightUpdate = weightUpdate;
        reset(w);
    }

    public void reset(float[] w) {
        this.w = w;
        reweigh();
    }

    private void realloc(int newSize) {
        throw new TODO();
    }

    public MutableRoulette reweigh(IntToFloatFunction initializer) {
        return reweigh(w.length, initializer);
    }

    public MutableRoulette reweigh(int n, IntToFloatFunction initializer) {
        assert(n>0);
        if (w.length!=n)
            realloc(n);

        for (int i = 0; i < n; i++)
            w[i] = initializer.valueOf(i);

        return reweigh();
    }

    public MutableRoulette reweigh() {
        int n = w.length;
        float s = 0;

        final int nn = n;
        for (int i = 0; i < nn; i++) {
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
//            this.direction = rng.nextBoolean();
//            this.i = rng.nextInt(l);

            int r = rng.nextInt(); //using only one RNG call
            this.direction = r >= 0;
            this.i = (r & 0b01111111111111111111111111111111) % l;
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

            //TODO optimized n=2 case

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

    public final int next() {

        switch (remaining) {
            case 0:
                return -1;
            case 1:
                return next1();
            default:
                return nextN();
        }

    }

    private int nextN() {
        float distance = EPSILON + rng.nextFloat() * weightSum;

        float[] w = this.w;
        int i = this.i;
//            int idle = 0;
        float wi;
        int count = w.length;

        do {
            wi = w[i = Util.next(i, direction, count)];
            distance -= wi;
//                    if (idle++ == count + 1)
//                        return -1; //emergency bailout: WTF
        } while (distance > 0);


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

    private int next1() {
        float[] w = this.w;
        int count = w.length;
        for (int x = 0; x < count; x++) {
            float wx = w[x];
            if (wx >= EPSILON) {
                float wy = weightUpdate.valueOf(wx);
                if (wy < EPSILON) {
                    w[x] = 0;
                    remaining = 0;
                } else {
                    w[x] = wy;
                }
                return x;
            }
        }

        throw new RuntimeException();
    }

    /** weight sum */
    public float weightSum() {
        return weightSum;
    }

    public int size() {
        return w.length;
    }

}
