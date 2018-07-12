package jcog.pri;


/**
 * default mutable prioritized implementation
 * float 32 bit
 *
 * range is not bounded; for 0..1.0 limit use UnitPri
 */
public class Pri extends Priority.AtomicScalarValue implements Priority {


    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        pri(p);
    }

    /** default: pri=0 */
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

    public boolean delete() {
        float p = pri();
        if (p==p) {
            priDirect(Float.NaN);
            return true;
        }
        return false;
    }

    /** allows subclasses to bypass their own overridden pri() methods */
    protected final void priDirect(float x) {
        super.pri(x);
    }


}
