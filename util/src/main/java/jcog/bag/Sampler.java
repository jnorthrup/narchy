package jcog.bag;

import jcog.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static jcog.bag.Sampler.SampleReaction.*;

public interface Sampler<V> {


    /* sample the bag, optionally removing each visited element as decided by the visitor's
     * returned value */
    void sample(Random rng, Function<? super V, SampleReaction> each);


    /**
     * lowest-common denominator basic sample one-instance-at-a-time;
     * not necessarily most efficient for a given impl.
     * gets the next value without removing changing it or removing it from any index.  however
     * the bag is cycled so that subsequent elements are different.
     */
    @Nullable
    default V sample(Random rng) {
        Object[] result = new Object[1];
        sample(rng, ((Predicate<? super V>) (x) -> {
            result[0] = x;
            return false;
        }));
        return (V) result[0];
    }


    default Sampler<V> sample(Random rng, Predicate<? super V> each) {
        sample(rng, (Function<V,SampleReaction>)(x -> each.test(x) ? Next : Stop));
        return this;
    }

    default Sampler<V> sample(Random rng, int max, Consumer<? super V> each) {
        sampleOrPop(rng, false, max, each);
        return this;
    }

    default Sampler<V> pop(Random rng, int max, Consumer<? super V> each) {
        sampleOrPop(rng, true, max, each);
        return this;
    }

    default Sampler<V> sampleOrPop(Random rng, boolean pop, int max, Consumer<? super V> each) {
        if (max == 0)
            return this;

        final int[] count = {max};
        sample(rng, (Function<V,SampleReaction>)(x -> {
            each.accept(x);
            return ((--count[0]) > 0) ? (pop ? Remove : Next) : (pop ? RemoveAndStop : Stop);
        }));
        return this;
    }

    /**
     * convenience macro for using sample(BagCursor).
     * continues while either the predicate hasn't returned false and
     * < max true's have been returned
     */
    default Sampler<V> sample(Random rng, int max, Predicate<? super V> kontinue) {
        if (max == 0)
            return this;

        final int[] count = {max};
        sample(rng, (Function<V,SampleReaction>)(x ->
                (kontinue.test(x) && ((--count[0]) > 0)) ?
                        Next : Stop));
        return this;
    }

    /**
     * action returned from bag sampling visitor indicating what to do with the current
     * item
     */
    enum SampleReaction {
        Next(false, false),
        Remove(true, false),
        Stop(false, true),
        RemoveAndStop(true, true);

        public final boolean remove;
        public final boolean stop;

        SampleReaction(boolean remove, boolean stop) {
            this.remove = remove;
            this.stop = stop;
        }
    }


    static class RoundRobinSampler<X> extends FasterList<X> implements Sampler<X> {

        @Override
        public void sample(Random rng, Function<? super X, SampleReaction> each) {
            int limit;
            X[] ii;
            restart: while ((limit = Math.min(size, (ii=items).length)) > 0) {

                if (limit == 0)
                    return;
                int next = rng.nextInt(limit);
                int missesBeforeRestart = 4, misses = 0;
                X n;
                do {
                    do {
                        next++;
                        if (next == limit)
                            next = 0; //loop
                        n = ii[next];
                        if (n == null) {
                            if (misses++ >= missesBeforeRestart) {
                                break restart;
                            }
                        } else {
                            misses = 0;
                        }
                    } while (n == null);

                } while (!each.apply(n).stop);
                return;
            }
        }
    }

}
