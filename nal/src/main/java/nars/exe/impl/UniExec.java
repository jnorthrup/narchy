package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FastCoWList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Offs;
import jcog.pri.PLink;
import jcog.service.Service;
import nars.NAR;
import nars.exe.Causable;
import nars.exe.Exec;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends Exec {

    static final int inputQueueCapacityPerThread = 256;

    protected final MetalConcurrentQueue in;

    public static final class TimedLink extends PLink<Causable> {

        public final AtomicLong used = new AtomicLong(0);

        /** cached: last calculated non-negative value rate */
        transient public float valueRate;
        /** cached: last calculated positive, negative, or NaN value rate */
        transient public float value;

        TimedLink(Causable causable) {
            super(causable, 0);
        }

//        void addAt(long t, long reserve) {
//            time.accumulateAndGet(t, (x,tt) -> Util.clamp(x + t, -reserve, reserve));
//        }
        void use(long t) {
            used.addAndGet(t);
        }

        public long used() {
            return used.getAndSet(0);
        }

        public MyTimedLink my() { return new MyTimedLink(); }

        /** thread-local view */
        final class MyTimedLink {

            /** allocated time for execution;
             * may be negative when excessive time consumed */
            public long time = 0;

            public final Causable can = TimedLink.this.get();

//            public long addAt(long t) {
//
//            }
            public void use(long t) {
                TimedLink.this.use(t);
                time -= t;
            }

            public float pri() {
                return TimedLink.this.priElseZero();
            }

            public void add(long t, long min, long max) {
                time = Util.clampSafe((time + t), min, max);
            }


        }
    }

    public final FastCoWList<TimedLink> cpu = new FastCoWList(TimedLink[]::new);

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
        n.plugin.change.on(serviceChange);
        refreshServices(); //to be sure

        ons = new Offs();
        ons.add(n.onCycle(this::onCycle));

    }

    private void refreshServices() {
        nar.plugins().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));
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
        return cpu.removeIf(x->x.get()==s);
    }

    private void add(Causable s) {
        //InstrumentedCausable r = can.computeIfAbsent(s, InstrumentedCausable::new);
        Term sid = s.id;
        if (cpu.containsInstance(s))
            throw new RuntimeException("causable " + s + " already present");
        if (cpu.OR(x->sid.equals(x.get().id)))
            throw new RuntimeException("causable " + s + " name collision");

        cpu.add(new TimedLink(s));
    }


}
