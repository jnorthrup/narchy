package nars.exe;

import jcog.Util;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Offs;
import jcog.pri.PLink;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import jcog.service.Service;
import nars.NAR;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends AbstractExec {

    //public final ConcurrentFastIteratingHashSet<Causable> can = new ConcurrentFastIteratingHashSet<>(new Causable[0]);

    static final int inputQueueCapacityPerThread = 1024;

    final MetalConcurrentQueue in;

    public static class TimedLink extends PLink<Causable> {

        /** allocated time for execution;
         * may be negative when excessive time consumed */
        public final AtomicLong time = new AtomicLong(0);

        /** cached value */
        transient public float value;

        TimedLink(Causable causable) {
            super(causable, 0);
        }

        void add(long t, long reserve) {
            time.accumulateAndGet(t, (x,tt) -> Util.clamp(x + t, -reserve, reserve));
        }
        void use(long t) {
            time.addAndGet( -t );
        }
    }

    public final ArrayBag<Causable, TimedLink> cpu = new PriArrayBag(PriMerge.replace, 64);
    Offs ons = null;

    public UniExec() {
        this(1, 1);
    }

    UniExec(int concurrency, int concurrencyMax) {
        super(concurrency, concurrencyMax);
        in = new MetalConcurrentQueue(inputQueueCapacityPerThread * concurrencyMax());
    }

    public int queueSize() {
        return in.size();
    }


    @Override
    public void start(NAR n) {
        super.start(n);

        Consumer<ObjectBooleanPair<Service<NAR>>> serviceChange = (xb) -> {
            Service<NAR> s = xb.getOne();
            if (s instanceof Causable) {
                if (xb.getTwo())
                    UniExec.this.add((Causable) s);
                else
                    UniExec.this.remove((Causable) s);
            }
        };
        refreshServices();
        n.services.change.on(serviceChange);
        refreshServices(); //to be sure

        ons = new Offs();
        ons.add(n.onCycle(this::onCycle));

    }

    private void refreshServices() {
        nar.services().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));
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

        cpu.forEach(x -> x.get().next(nar, () -> false /* 1 work unit */));
    }

    void sync() {
        Object next;
        while ((next = in.poll()) != null) executeNow(next);
    }


    private boolean remove(Causable s) {
        return cpu.remove(s)!=null; //==s
    }

    private void add(Causable s) {
        //InstrumentedCausable r = can.computeIfAbsent(s, InstrumentedCausable::new);
        cpu.put(new TimedLink(s));
    }


}
