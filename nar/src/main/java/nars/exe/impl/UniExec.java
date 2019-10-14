package nars.exe.impl;

import nars.NAR;
import nars.attention.What;
import nars.derive.Deriver;
import nars.derive.DeriverExecutor;
import nars.exe.Exec;

import java.util.function.BooleanSupplier;

import static java.lang.System.nanoTime;

/**
 * single thread executor used for testing
 */
public class UniExec extends Exec {

    private DeriverExecutor exe;

    public UniExec() {
        this(1);
    }

    protected UniExec(int concurrencyMax) {
        super(concurrencyMax);
    }

    @Override public void deriver(Deriver d) {
        super.deriver(d);
        this.exe = new DeriverExecutor.QueueDeriverExecutor(d);
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
        DeriverExecutor e = this.exe;
        if (e == null)
            return;

        /*
        simplest possible implementation: flat 1 work unit per each what
        */
        long timesliceNS = timeSliceNS();
        boolean sync = timesliceNS == Long.MIN_VALUE;
        BooleanSupplier kontinue;
        if (sync) {
            kontinue = null;
        } else {
            long deadline = nanoTime() + timeSliceNS();
            kontinue = () -> nanoTime() < deadline;
        }

        exe.cycle();

        for (What w : n.what) {
            if (w.isOn()) {
                if (sync)
                    exe.nextSynch(w);
                else
                    exe.next(w, kontinue); //w.next(nanoTime(), timesliceNS);
            }
        }
    }

    /* absolute stop time (in systemNS) for the next executed What; return Long.MIN_VALUE to use immediate */
    protected long timeSliceNS() {
        return Long.MIN_VALUE;
    }

}
