package jcog.pri;


/**
 * default mutable prioritized implementation
 * float 32 bit
 *
 * range is not bounded; for 0..1.0 limit use UnitPri
 */
public class Pri extends ScalarValue.AtomicScalarValue implements Priority {


    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        pri(p);
    }

    /**
     * default: pri=0
     */
    public Pri() {
        pri(0);
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

    /** override to implement a value post-filter */
    public float v(float x) {
        return x;
    }
}