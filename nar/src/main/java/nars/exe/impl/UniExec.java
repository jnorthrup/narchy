package nars.exe.impl;

import nars.NAR;
import nars.attention.What;
import nars.control.How;
import nars.exe.Exec;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
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
        for (What w : n.what) {
            if (w.isOn()) {
                for (How h : n.how) {
                    if (h.isOn()) {
                        h.next(w, () -> false);
                    }
                }
            }
        }
    }

}
