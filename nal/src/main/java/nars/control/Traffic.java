package nars.control;

import jcog.Texts;
import jcog.data.atomic.AtomicFloat;

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

    /** fully atomic commit */
    public final void commit() {
        zero(this::commit);
    }
    /** partially atomic commit, faster than full atomic commit;
     * should be ok for single thread modes */
    public final void commitFast() {
        commit(getAndZero());
    }

    private void commit(float cur) {
        //noinspection NonAtomicOperationOnVolatileField
        this.total += (this.last = cur);
    }

    @Override
    public String toString() {
        return Texts.n4(last) + "/" + Texts.n4(total);
    }
}
