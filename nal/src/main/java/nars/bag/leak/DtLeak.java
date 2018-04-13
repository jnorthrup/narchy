package nars.bag.leak;

import jcog.bag.Bag;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

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

    float RATE_THRESH = 1f;

    public final MutableFloat rate /* base rate items per dt */;


    private volatile long lastLeak = ETERNAL;
    private volatile float lastBudget;

    protected DtLeak(@NotNull Bag<X, Y> bag, @NotNull MutableFloat rate) {
        super(bag);
        this.rate = rate;
    }


    private final AtomicBoolean busy = new AtomicBoolean(false);

    public float commit(long now, int dur, float forgetRate, float work) {

        if (!busy.compareAndSet(false, true))
            return 0;

        try {

            if (!bag.commit(bag.forget(forgetRate)).isEmpty()) {

                long last = this.lastLeak;
                if (last == ETERNAL) {
                    last = now - dur;
                }

                return commit(now, last, dur, work);

            }
        } finally {
            busy.set(false);
        }

        return 0;
    }

    public float commit(long now, long last, int dur, float work) {

        //durations delta
        float durDT = Math.max(0, (now - last) / ((float) dur));

        float nextBudget = work * rate.floatValue() * durDT + lastBudget;
        //System.out.println(this + " " + rate + " " + durDT + " " + nextBudget + " { " + lastBudget );

        if (nextBudget < RATE_THRESH) {
            return 0; //wait longer
        }

        this.lastLeak = now;

        final float[] budget = {nextBudget};

        Random rng = random();

        bag.sample(rng, (Bag.BagCursor<Y>)((v) -> {

            float cost = receive(v);
            budget[0] -= cost;

            float remain = budget[0];

            if (remain < 1) {
                if (remain <= 0 || rng.nextFloat() > remain)
                    return Bag.BagSample.RemoveAndStop;
            }

            return Bag.BagSample.Remove; //continue
        }));

        this.lastBudget = Math.min(0, budget[0]); //only store surplus, which will be added to the next. otherwise if positive is also stored, it can explode

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
