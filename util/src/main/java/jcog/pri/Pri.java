package jcog.pri;


import jcog.Util;
import jcog.bag.impl.HijackBag;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * default mutable prioritized implementation
 * float 32 bit
 */
public class Pri implements Priority {

    protected /*volatile*/ float pri;

    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        priSet(p);
    }


    @Override
    public final float pri() {
        return pri;
    }

    @Override
    public boolean delete() {
        float p = pri;
        if (p==p) {
        //if (!isDeleted()) { //dont call isDeleted it may be overridden in a cyclical way
            this.pri = Float.NaN;
            return true;
        }
        //logger.warn("alredy deleted");
//            throw new RuntimeException("Already Deleted");
        return false;
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

    @Override
    public final float priSet(float p) {
        return this.pri = Util.unitize(p);
    }

}
