package nars.exe.impl;

import nars.NAR;
import nars.attention.What;
import nars.exe.Exec;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * single thread executor used for testing
 */
public class UniExec extends Exec {

    public UniExec() {
        this(1);
    }

    protected UniExec(int concurrencyMax) {
        super(concurrencyMax);
    }

    @Override
    public final int concurrency() {
        return 1;
    }

    @Override
    public final boolean concurrent() {
        return false;
    }

    protected void next() {

        schedule(this::executeNow);

        NAR n = this.nar;

        /*
        simplest possible implementation: flat 1 work unit per each what
        */
        BooleanSupplier runUntil = runUntil();
        for (What w : n.what) {
            if (w.isOn()) {
                if (runUntil == null)
                    w.nextSynch();
                else
                    w.next(runUntil);

                //for (How h : n.how) {
                    //if (h.isOn()) {
                       // h.next(w, () -> false);
                    //}
                //}
            }
        }
    }

    /* stop condition for each executed What */
    @Nullable
    protected BooleanSupplier runUntil() {
        return null;
    }

}
