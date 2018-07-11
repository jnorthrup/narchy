package jcog.decide;

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

    /** with no weight modification */
    public MutableRoulette(int count, IntToFloatFunction initialWeights, Random rng) {
        this(count, initialWeights, (x -> x), rng);
    }

    public MutableRoulette(int count, IntToFloatFunction initialWeights, FloatToFloatFunction weightUpdate, Random rng) {
        this(Util.map(count, initialWeights), weightUpdate, rng );
    }

    public MutableRoulette(float[] w, FloatToFloatFunction weightUpdate, Random rng) {
        this.w = w;
        int n = w.length;
        this.remaining = n;
        this.weightUpdate = weightUpdate;
        this.rng = rng;

        reweigh();
        this.i = n == 1 ? 0 : rng.nextInt(n);
    }

    public MutableRoulette reweigh() {
        int n = w.length;
        float s = 0;

        for (int i = 0; i < n; i++) {
            float wi = w[i];
            if (!(wi >= 0 && Float.isFinite(wi)))
                throw new RuntimeException("invalid weight: " + wi);

            if (wi < EPSILON) {
                w[i] = 0;
                remaining--;
            } else {
                s += wi;
            }
        }

        if (remaining == 0 || s < (n) * EPSILON) {

            Arrays.fill(w, 1);
            s = remaining = n;
        }

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
            
            

            for (int x = 0; x < count; x++)
                if (w[x] > 0) {
                    
                    w[x] = 0;
                    remaining = 0;
                    return x;
                }

            throw new RuntimeException();
        } else {

            float distance = rng.nextFloat() * weightSum;
            

            int i = this.i;
            int idle = 0;
            float wi;
            while ((distance = distance - (wi = w[i])) > EPSILON) {
                
                if (++i == count) i = 0;
                
                
                

                if (idle++ == count+1)
                    return -1; 

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

}
