package nars.exe;

import jcog.data.list.MetalConcurrentQueue;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.event.Offs;
import jcog.exe.valve.*;
import jcog.service.Service;
import nars.NAR;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends AbstractExec {



    public final ConcurrentFastIteratingHashMap<Causable, InstrumentedCausable> can = new ConcurrentFastIteratingHashMap<>(new InstrumentedCausable[0]);

    static final int inputQueueCapacityPerThread = 1024;

    final MetalConcurrentQueue in;


    final Sharing sharing = new Sharing();
    TimeSlicing cpu;
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
    public int queueCapacity() {
        return in.capacity();
    }



    public final class InstrumentedCausable extends InstrumentedWork {

        public final Causable c;

        InstrumentedCausable(Causable c) {
            super(new MyWork(c));
            this.c = c;
        }

//        @Override
//        public float pri(float p) {
//            if (c instanceof Prioritizable) {
//                ((Prioritizable)c).pri(p);
//            }
//            return super.pri(p);
//        }
    }

    private final class MyWork extends AbstractWork {

        private final Causable c;

        MyWork(Causable c) {
            super(sharing.start(c), "CPU", 0.5f);
            this.c = c;
        }

        @Override
        public boolean next() {
            c.next(nar, () -> false);
            return false;
        }

        @Override
        public final int next(int n) {
            int[] done = new int[]{0};
            try {
                c.next(nar, () -> ++done[0] < n);
            } catch (Throwable t) {
                logger.error("{} {}", c, t);
                return 0;
            }
            return done[0];
        }

        @Override
        public int next(BooleanSupplier kontinue) {

            try {
                return c.nextCounted(nar, kontinue);
            } catch (Throwable t) {
                logger.error("{} {}", c, t);
                return 0;
            }

        }
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

        sharing.can(cpu = scheduler());
    }

    TimeSlicing scheduler() {
        /* deprecated */ return new TimeSlicing<>("CPU", 1, nar.exe) {
            @Deprecated
            @Override
            protected void trySpawn() {

            }

            @Override
            @Deprecated
            protected boolean work() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Mix<Object, String, InstrumentedWork<Object, String>> commit() {
                return this;
            }
        };
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
        if (sharing != null) {
            sharing.off(cpu.what, cpu);
        }
        super.stop();
    }


    void onCycle(NAR nar) {

        sync();
        nar.time.schedule(this::executeNow);

        can.forEachValue(c ->
                c.c.next(nar, () -> false /* 1 work unit */)
        );
    }

    void sync() {
        Object next;
        while ((next = in.poll()) != null) executeNow(next);
    }


    private boolean remove(Causable s) {
        return can.remove(s) != null;
    }

    private void add(Causable s) {
        InstrumentedCausable r = can.computeIfAbsent(s, InstrumentedCausable::new);
    }


}
