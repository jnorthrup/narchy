package nars.exe.impl;

import jcog.event.Offs;
import nars.NAR;
import nars.control.How;
import nars.exe.Exec;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends Exec {

    static final int inputQueueCapacityPerThread = 256;


    Offs ons = null;

    public UniExec() {
        this(1);
    }

    public UniExec(int concurrencyMax) {
        super(concurrencyMax);
    }



    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public void start(NAR n) {
        super.start(n);



        ons = new Offs();
        ons.add(n.onCycle(this::onCycle));

    }


    @Override
    public void stop() {
        if (ons != null) {
            ons.pause();
            ons = null;
        }

        super.stop();
    }


    protected void onCycle(NAR nar) {
        nar.time.schedule(this::executeNow);

        /*
        simplest possible implementation: flat 1 work unit per each what
        */
        for (How h : nar.how) {
            nar.what.forEach(w -> h.next(w, ()->false));
        }
    }

}
