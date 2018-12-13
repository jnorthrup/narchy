package jcog.pri;


import static jcog.data.atomic.AtomicFloatFieldUpdater.iNaN;

/**
 * default mutable prioritized implementation
 * float 32 bit
 *
 * range is not bounded; for 0..1.0 limit use UnitPri
 */
public class Pri extends AtomicPri implements Prioritizable {


    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        if (p == p) {
            pri(p);
        } else {
            //start deleted
            PRI.INT.getAndSet(this, iNaN); //HACK
        }
    }

    /**
     * default: pri=0
     */
    public Pri() {
        super();
        //no further initialization required;  the initial value is 0.0f
    }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @Override
    public String toString() {
        return getBudgetString();
    }


}