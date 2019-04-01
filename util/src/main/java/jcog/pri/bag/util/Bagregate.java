package jcog.pri.bag.util;

import com.google.common.collect.Iterables;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * a bag which wraps another bag, accepts its value as input but at a throttled rate
 * resulting in containing effectively the integrated / moving average values of the input bag
 * TODO make a PLink version of ArrayBag since quality is not used here
 */
public class Bagregate<X> implements Iterable<PriReference<X>> {

    public final Bag<?, PriReference<X>> bag;
    private final Iterable<? extends X> src;

    public final FloatRange preAmp = new FloatRange(1, 0f, 1f);
    public final FloatRange forgetRate = new FloatRange(0.5f, 0, 1);

    public Bagregate(Stream<X> src, int capacity) {
        this(src::iterator, capacity);
    }

    public Bagregate(Iterable<? extends X> src, int capacity) {
        this.bag = new PLinkArrayBag<>(PriMerge.max /*PriMerge.replace*/, capacity) {
            @Override
            public void onRemove(PriReference<X> value) {
                Bagregate.this.onRemove(value);
            }
        };
        this.src = src;
    }

    protected void onRemove(PriReference<X> value) {

    }

    public boolean commit() {
        if (src==null /*|| !busy.compareAndSet(false, true)*/)
            return false;

        float preAmp = this.preAmp.floatValue();

        bag.commit(bag.forget(this.forgetRate.floatValue()));

        src.forEach(xx -> {
            if (include(xx)) {
                float pri = pri(xx);
                if (pri==pri)
                    bag.putAsync(new PLink<>(xx, pri*preAmp ));
            }
        });

        return true;
    }

    protected float pri(X xx) {
        return 1f;
    }

    /**
     * can be overridden to filter entry
     */
    private boolean include(X x) {
        return true;
    }

    @Override
    public final Iterator<PriReference<X>> iterator() {
            return bag.iterator();
    }

    @Override
    public final void forEach(Consumer<? super PriReference<X>> action) {
            bag.forEach(action);
    }

    public void clear() {
        bag.clear();
    }


    /** compose */
    public Iterable<X> iterable() {
        return Iterables.transform(Iterables.filter(bag, Objects::nonNull) /* HACK */, Supplier::get);
    }

    /** compose */
    public <Y> Iterable<Y> iterable(Function<X, Y> f) {
        return Iterables.transform(Iterables.filter(bag, Objects::nonNull) /* HACK */, (b)->f.apply(b.get()));
    }

    public final void setCapacity(int c) {
        bag.setCapacity(c);
    }
}
