package jcog.pri.bag;

import jcog.pri.bag.util.ProxyBag;

import java.util.Random;
import java.util.function.Function;

/** wraps a mutable bag.  but provides an overridden sampling behavior that selects
 * with some configurable share of the probability from an implementable source
 */
public class ParametricBag<K,V> extends ProxyBag<K,V> {

    private final Sampler<V> coSampler;

    /** balance determines probability of sampling from the co-sampler rather than the delegate bag.
     *  0 means never,1.0 means always */
    protected volatile float balance = 0.5f;

    public ParametricBag(Bag<K, V> delegate, Sampler<V> coSampler) {
        super(delegate);
        this.coSampler = coSampler;
    }

    @Override
    public void sample(Random rng, Function<? super V, SampleReaction> each) {
        //assert(coSampler.size() > 0);
        boolean done = false;
        do {
            Sampler<V> next = (bag.size() > 0 && rng.nextFloat() >= balance) ? bag : coSampler;
            V n = next.sample(rng);
            if (next == null)
                continue;
            SampleReaction reaction = each.apply(n);
            if (reaction.stop)
                done = true;

        } while (!done);
    }
}
