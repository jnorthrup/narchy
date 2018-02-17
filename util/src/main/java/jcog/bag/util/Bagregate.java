package jcog.bag.util;

import jcog.bag.impl.ConcurrentArrayBag;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * a bag which wraps another bag, accepts its value as input but at a throttled rate
 * resulting in containing effectively the integrated / moving average values of the input bag
 * TODO make a PLink version of ArrayBag since quality is not used here
 */
public class Bagregate<X extends Prioritized> extends ConcurrentArrayBag<X, PriReference<X>> {

    private final Iterable<X> src;
    private final MutableFloat scale;
    final AtomicBoolean busy = new AtomicBoolean();

    public Bagregate(@NotNull Stream<X> src, int capacity, float scale) {
        this(src::iterator, capacity, scale);
    }

    public Bagregate(@NotNull Iterable<X> src, int capacity, float scale) {
        super(PriMerge.plus, capacity);

        this.src = src;
        this.scale = new FloatRange(scale);

    }

    public boolean update() {
        if (src==null || !busy.compareAndSet(false, true))
            return false;

        try {

            commit();

            float scale = this.scale.floatValue();

            src.forEach(x -> {
                if (include(x)) {
                    float pri = x.pri();
                    if (pri==pri)
                        putAsync(new PLink<>(x, pri * scale));
                }
            });


        } finally {
            busy.set(false);
        }
        return true;
    }

    /**
     * can be overridden to filter entry
     */
    protected boolean include(X x) {
        return true;
    }


    @Nullable
    @Override
    public X key(PriReference<X> x) {
        return x.get();
    }
}
