package jcog.pri;


/**
 * default mutable prioritized implementation
 * float 32 bit
 *
 * range is not bounded; for 0..1.0 limit use UnitPri
 */
public class Pri implements Priority {

    protected volatile float pri;

    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        priSet(p);
    }

    /** default: pri=0 */
    public Pri() {

    }

    @Override
    public float priSet(float p) {
        return this.pri = p;
    }

    @Override
    public final float pri() {
        return pri;
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
        float p = pri;
        if (p==p) {
            pri = (Float.NaN);
            return true;
        }
        return false;
    }

}
