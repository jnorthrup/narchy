package nars.control;

import jcog.Texts;
import jcog.data.atomic.AtomicFloat;

/** concurrent traffic accumulator;
 *  concurrent updates (ie. addAt) but expects a synchronous commit
 *  to sample after each cycle.
 *
 *  the AtomicFloat which this subclasses holds the accumulating value
 *  that safely supports multiple concurrent accumulators */
public class Credit extends /*DoubleAdder*/ AtomicFloat {

    /** value at last commit */
    public volatile float current;

//    final DoubleAdder total = new DoubleAdder();

//    /** fully atomic commit */
//    public final void commit() {
//        zero(this::commit);
//    }
    /** partially atomic commit, faster than full atomic commit;
     * should be ok for single thread modes */
    public final void commit() {
        //float next = (float) sumThenReset();
        this.current = getAndSet(0);

//        DoubleAdder t = this.total;
//        if (t !=null)
//            t.add(next);
    }


//    private void commit(float cur) {
//        //noinspection NonAtomicOperationOnVolatileField
//        this.total += (this.last = cur);
//    }

    @Override
    public String toString() {
        return Texts.n4(current);
    }

//    public final double total() {
//        return total.doubleValue();
//    }
}
