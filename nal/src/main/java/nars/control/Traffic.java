package nars.control;

import jcog.Texts;
import jcog.util.AtomicFloat;

/** concurrent traffic accumulator;
 *  concurrent updates (ie. add) but expects a synchronous commit
 *  to sample after each cycle.
 *
 *  the AtomicFloat which this subclasses holds the accumulating value
 *  that safely supports multiple concurrent accumulators */
public class Traffic extends AtomicFloat {

    /** value at last commit */
    public volatile float last;

    public volatile double total;

    public final void commit() {
        zero(this::commit);
    }

    protected final void commit(float cur) {
        this.total += (this.last = cur);
    }

    @Override
    public String toString() {
        return Texts.n4(last) + "/" + Texts.n4(total);
    }
}
