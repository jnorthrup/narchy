package nars.exe.impl;

import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Offs;
import nars.NAR;
import nars.exe.Exec;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends Exec {

    static final int inputQueueCapacityPerThread = 256;

    protected final MetalConcurrentQueue in;

    Offs ons = null;

    public UniExec() {
        this(1);
    }

    public UniExec(int concurrencyMax) {
        super(concurrencyMax);
        in = new MetalConcurrentQueue(inputQueueCapacityPerThread * concurrencyMax());
    }

    public int queueSize() {
        return in.size();
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
            ons.off();
            ons = null;
        }

        super.stop();
    }


    protected void onCycle(NAR nar) {

        sync();
        nar.time.schedule(this::executeNow);

        /* flat 1 work unit per each. returns immediately after first iteration */
        nar.control.active.forEach(x -> x.next(nar, () -> false ));
    }

    void sync() {
        Object next;
        while ((next = in.poll()) != null) executeNow(next);
    }




}
