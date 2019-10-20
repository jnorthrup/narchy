package jcog.decide;

import jcog.TODO;
import jcog.Util;
import jcog.pri.ScalarValue;
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

    private static final float EPSILON = MIN_NORMAL;
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

    public MutableRoulette(int count, IntToFloatFunction initialWeights, FloatToFloatFunction weightUpdate, Random rng) {
        this(Util.map(count, initialWeights), weightUpdate, rng);
    }

    private MutableRoulette(float[] w, FloatToFloatFunction weightUpdate, Random rng) {
        this.rng = rng;
        this.weightUpdate = weightUpdate;
        reset(w);
    }

    /** constructs and runs entirely in constructor */
    public MutableRoulette(float[] weights, FloatToFloatFunction weightUpdate, Random rng, IntPredicate choose) {
        this(weights, weightUpdate, rng);
        while (next(choose)) { }
    }

    public void reset(float[] w) {
        this.w = w;
        reweigh();
    }

    private static void realloc(int newSize) {
        throw new TODO();
    }

    public MutableRoulette reweigh(IntToFloatFunction initializer) {
        return reweigh(w.length, initializer);
    }

    public MutableRoulette reweigh(int n, IntToFloatFunction initializer) {
        assert(n>0);

        if (w.length!=n)
            realloc(n);

        float[] w = this.w;
        for (int i = 0; i < n; i++)
            w[i] = initializer.valueOf(i);

        return reweigh();
    }

    public MutableRoulette reweigh() {
        int n = w.length;
        float s = (float) 0;

        int nn = n;
        float[] w = this.w;
        for (int i = 0; i < nn; i++) {
            float wi = w[i];
            if (wi < (float) 0 || !Float.isFinite(wi))
                throw new RuntimeException("invalid weight: " + wi);

            if (wi < EPSILON) {
                w[i] = (float) 0;
                n--;
            } else {
                s += wi;
            }
        }

        if (n == 0 || s < (float) n * EPSILON) {
            Arrays.fill(w, ScalarValue.EPSILON);
            s = ScalarValue.EPSILON * (float) n;
            n = w.length;
        }

        this.remaining = n;
        this.weightSum = s;

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


        return this;
    }


    /**
     * weight array may be modified
     */
    public static void run(float[] weights, Random rng, FloatToFloatFunction weightUpdate, IntPredicate choose) {
        if (weights.length > 1)
            runN(weights, rng, weightUpdate, choose);
        else if (weights.length == 1)
            run1(weights[0], weightUpdate, choose);
        //TODO optimized 2-ary case
        else
            throw new UnsupportedOperationException();
    }

    public static void runN(float[] weights, Random rng, FloatToFloatFunction weightUpdate, IntPredicate choose) {
        new MutableRoulette(weights, weightUpdate, rng, choose);
    }

    public static void run1(float weight, FloatToFloatFunction weightUpdate, IntPredicate choose) {
        float theWeight = weight;
        while (choose.test(0) && ((theWeight = weightUpdate.valueOf(theWeight)) > EPSILON)) {
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
        float distance = rng.nextFloat() * weightSum;

        float[] w = this.w;
        int i = this.i;
        float wi;
        int count = w.length;

        int idle = count+1; //to count eextra step
        do {
            wi = w[i = Util.next(i, direction, count)];
            distance -= wi;
            if (--idle < 0)
                return -1; //emergency bailout
        } while (distance > (float) 0);


        float nextWeight = weightUpdate.valueOf(wi);
        if (!validWeight(nextWeight)) {
            w[i] = (float) 0;
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
                float nextWeight = weightUpdate.valueOf(wx);
                if (!validWeight(nextWeight)) {
                    w[x] = (float) 0;
                    remaining = 0;
                } else {
                    w[x] = nextWeight;
                }
                return x;
            }
        }

        throw new RuntimeException();
    }

    private static boolean validWeight(float nextWeight) {
        return nextWeight==nextWeight /*!NaN*/ && nextWeight >= EPSILON;
    }

    /** weight sum */
    public float weightSum() {
        return weightSum;
    }

    public int size() {
        return w.length;
    }

}
