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

    @Deprecated private int loops;

    public UniExec(int power) {
        this(1, power);
    }

    protected UniExec(int concurrencyMax, int power) {
        super(concurrencyMax);
        power(power);
    }

    public final UniExec power(int itersPerCycle) {
        this.loops = itersPerCycle;
        return this;
    }

    @Override public void deriver(Deriver d) {
        super.deriver(d);
        this.exe =
            new DeriverExecutor.QueueDeriverExecutor(d);

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

        var n = this.nar;
        var e = this.exe;
        if (e == null)
            return;

        /*
        simplest possible implementation: flat 1 work unit per each what
        */
        var timesliceNS = timeSliceNS();
        var sync = timesliceNS == Long.MIN_VALUE;
        BooleanSupplier kontinue;
        if (sync) {
            kontinue = null;
        } else {
            var deadline = nanoTime() + timeSliceNS();
            kontinue = () -> nanoTime() < deadline;
        }




        for (var w : n.what) {
            if (w.isOn()) {
                if (sync)
                    exe.next(w, loops);
                else
                    exe.next(w, kontinue); //w.next(nanoTime(), timesliceNS);
            }
        }
    }

    /* absolute stop time (in systemNS) for the next executed What; return Long.MIN_VALUE to use immediate */
    protected static long timeSliceNS() {
        return Long.MIN_VALUE;
    }

}
