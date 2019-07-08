package nars.task.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.time.Tense.TIMELESS;

/** utility for finding the most extreme TaskRegion by reducing an iteration through a comparator.
 *  caches the temporal and evidence (or conf) component of current winner for fast compare.
 *
 * @param X the target type sought
 * @param Y is an extracted derived object (can be = X)
 * */
abstract public class ExtremeRegion<X,Y> implements Consumer<X>, Supplier<X> {

    private @Nullable X most = null;
    long when = TIMELESS;
    double how = Double.NaN;

    /** return null to cancel */
    @Nullable abstract protected Y the(X x);

    /** return TIMELESS to cancel */
    abstract protected long when(Y t);

    /** return NaN to cancel */
    abstract protected double how(Y t);

    abstract protected boolean replace(long whenCurrent, double howCurrent, long whenIncoming, double howIncoming);

    @Override
    public final void accept(X incoming) {
        if (most == incoming) return; //already here

        Y i = the(incoming);
        if (i == null) return;

        double h = how(i);
        if (h!=h) return;

        long w = when(i);
        if (w == TIMELESS) return;

        if (most == null || replace(when, how, w, h)) {
            most = incoming;
            when = w; how = h;
        }
    }

    @Nullable final public X get() {
        return most;
    }

    public final boolean isEmpty() {
        return most==null;
    }

    public final boolean accepted(X m) {
        accept(m);
        return most == m;
    }

}
