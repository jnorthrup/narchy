package nars.bag.leak;

import jcog.bag.Bag;
import jcog.bag.Sampler;
import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

import static nars.time.Tense.ETERNAL;

/**
 * asynchronously controlled implementation of Leak which
 * decides demand according to time elapsed (stored as some 'long' value)
 * since a previous call, and a given rate parameter.
 * if the rate * elapsed dt will not exceed the provided maxCost
 * value, which can be POSITIVE_INFINITY (by default).
 * <p>
 * draining the input bag
 */
public abstract class DtLeak<X, Y> extends Leak<X, Y> {

    private final MutableFloat rate /* base rate items per dt */;
    private float RATE_THRESH = 1f;
    private volatile long lastLeak = ETERNAL;
    private volatile float lastBudget;

    DtLeak(@NotNull Bag<X, Y> bag, @NotNull MutableFloat rate) {
        super(bag);
        this.rate = rate;
    }

    public float commit(NAR nar, int iterations) {


        long now = nar.time();
        int dur = nar.dur();

        long last = this.lastLeak;
        if (last == ETERNAL) {
            this.lastLeak = last = now;
        }

        float forgetRate = nar.forgetRate.floatValue();
        if (!bag.commit(bag.forget(forgetRate)).isEmpty()) {

            
            float durDT = Math.max(0, (now - last) / ((float) dur));

            float nextBudget = iterations * rate.floatValue() * durDT + lastBudget;
            

            if (nextBudget >= RATE_THRESH) {
                this.lastLeak = now;

                return commit(nextBudget);
            }
        }


        return 0;
    }

    /** override to implement backpressure stop switch */
    boolean full() {
        return false;
    }

    private float commit(float nextBudget) {

        final float[] budget = {nextBudget};

        Random rng = random();

        bag.sample(rng, ((Y v) -> {

            float cost = receive(v);
            budget[0] -= cost;

            float remain = budget[0];

            if (remain < 1) {
                if (remain <= 0 || rng.nextFloat() > remain)
                    return Sampler.SampleReaction.RemoveAndStop;
            }

            return !full() ? Sampler.SampleReaction.Remove : Sampler.SampleReaction.RemoveAndStop;
        }));

        this.lastBudget = Math.min(0, budget[0]); 

        return nextBudget - budget[0];

    }

    abstract protected Random random();

    /**
     * returns a cost value, in relation to the bag sampling parameters, which is subtracted
     * from the rate each iteration. this can allow proportional consumption of
     * a finitely allocated resource.
     */
    abstract protected float receive(Y b);

    public void put(Y x) {
        bag.put(x);
    }
}
