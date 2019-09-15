package nars.exe.impl;

import nars.NAR;
import nars.attention.What;
import nars.exe.Exec;

import static java.lang.System.nanoTime;

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
        long timesliceNS = timeSliceNS();
        for (What w : n.what) {
            if (w.isOn()) {
                if (timesliceNS == Long.MIN_VALUE)
                    w.nextSynch();
                else
                    w.next(nanoTime(), timesliceNS);

                //for (How h : n.how) {
                    //if (h.isOn()) {
                       // h.next(w, () -> false);
                    //}
                //}
            }
        }
    }

    /* absolute stop time (in systemNS) for the next executed What; return Long.MIN_VALUE to use immediate */
    protected long timeSliceNS() {
        return Long.MIN_VALUE;
    }

}
