package nars.control;

import jcog.Texts;
import jcog.data.atomic.AtomicFloat;

import java.util.concurrent.atomic.DoubleAdder;

/** concurrent traffic accumulator;
 *  concurrent updates (ie. addAt) but expects a synchronous commit
 *  to sample after each cycle.
 *
 *  the AtomicFloat which this subclasses holds the accumulating value
 *  that safely supports multiple concurrent accumulators */
public class Traffic extends AtomicFloat {

    /** value at last commit */
    public volatile float last;

    DoubleAdder total = null;

//    /** fully atomic commit */
//    public final void commit() {
//        zero(this::commit);
//    }
    /** partially atomic commit, faster than full atomic commit;
     * should be ok for single thread modes */
    public final void commit() {
        float next = getAndZero();
        this.last = next;

        DoubleAdder t = this.total;
        if (t !=null)
            t.add(next);
    }


//    private void commit(float cur) {
//        //noinspection NonAtomicOperationOnVolatileField
//        this.total += (this.last = cur);
//    }

    @Override
    public String toString() {
        return Texts.n4(last) + '/' + (total!=null ? Texts.n4(total()) : "?");
    }

    public final double total() {
        if (total == null) {
            //lazy instantiate the totaller
            total = new DoubleAdder();
            return 0;
        }
        return total.doubleValue();
    }
}
