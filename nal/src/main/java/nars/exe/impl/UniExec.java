package nars.exe.impl;

import nars.NAR;
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
    public void input(Object t) {
        executeNow(t);
    }

    @Override
    public int concurrency() {
        return 1;
    }

    protected void cycle(NAR nar) {
        nar.time.schedule(this::executeNow);

        /*
        simplest possible implementation: flat 1 work unit per each what
        */
        for (How h : nar.how) {
            if (h.isOn())
                nar.what.forEach(w -> {
                    if (w.isOn())
                        h.next(w, ()->false);
                });
        }
    }

    /** forces concurrent() even for 1-thread execution */
    public static class Concurrent extends UniExec {
        @Override
        public boolean concurrent() {
            return true;
        }
    }
}
