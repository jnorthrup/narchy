package nars.exe;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.event.On;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.control.Activate;

import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * unified executor
 * concurrent, thread-safe. one central concept bag
 */
abstract public class AbstractExec extends Exec {

    private final int CAPACITY;

    public Bag<?, Activate> active;
    private On onCycle;
    

    protected AbstractExec(@Deprecated int conceptsCapacity) {

        CAPACITY = conceptsCapacity;
    }

    @Override
    public void clear() {
        synchronized (this) {
            if (active != null)
                active.clear();
        }
    }
    @Override
    public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }
    public void execute(Consumer<NAR> r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(() -> r.accept(nar));
        } else {
            r.accept(nar);
        }
    }

    @Override
    public void activate(Concept c, float activationApplied) {
        active.putAsync(new Activate(c, activationApplied));
    }

    @Override
    public Activate fire() {
        return active.sample(nar.random());
    }

    @Override
    public void fire(Predicate<Activate> each) {

        active.sample(nar.random(), each);










    }

    @Override
    public void start(NAR nar) {

        synchronized (this) {
            assert (active == null && onCycle == null);

            active =
                    concurrent() ?





                            new PriorityHijackBag<>(Math.round(CAPACITY * 1.5f), 4) {
                                @Override
                                public Activate key(Activate value) {
                                    return value;
                                }










                            }

                            :

                            new CurveBag<>(
                                    Param.activateMerge,
                                    new HashMap<>(CAPACITY*2),
                                    CAPACITY) {










                            }
            ;

            super.start(nar);

            onCycle = nar.onCycle(this::update);
        }
    }

    protected void update(NAR nar) {
        active.commit(active.forget(nar.forgetRate.floatValue()));
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (onCycle!=null) {
                onCycle.off();
                onCycle = null;
            }
            if (active!=null) {
                active.clear();
                active = null;
            }
        }
    }


    @Override
    public Stream<Activate> active() {
        Iterable<nars.control.Activate> a = active;
        return a == null ? Stream.empty() : active.stream();
    }














































}
