package nars.bag.leak;

import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import nars.NAR;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.BooleanSupplier;

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


    DtLeak(@NotNull Bag<X, Y> bag) {
        super(bag);
    }

    public void commit(NAR nar, BooleanSupplier kontinue) {

        float forgetRate = nar.forgetRate.floatValue();

        if (!bag.commit(bag.forget(forgetRate)).isEmpty()) {
            commit(kontinue);
        }

    }


    private void commit(BooleanSupplier kontinue) {


        Random rng = random();

        bag.sample(rng, ((Y v) -> {
            float cost = receive(v);

            return kontinue.getAsBoolean() ? Sampler.SampleReaction.Remove : Sampler.SampleReaction.RemoveAndStop;
        }));

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
