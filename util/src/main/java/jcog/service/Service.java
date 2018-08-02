package jcog.service;

import jcog.event.Off;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public abstract class Service<C> extends AtomicReference<Services.ServiceState> implements Off {

    public boolean isOn() {
        return getOpaque() == Services.ServiceState.On;
    }

    public boolean isOff() {
        Services.ServiceState s = getOpaque();
        return s == Services.ServiceState.Off;
    }

    protected Service() {
        super(Services.ServiceState.Off);
    }

    @Override
    public String toString() {
        String nameString = getClass().getName();

        
        if (nameString.startsWith("jcog.") || nameString.startsWith("nars."))
            nameString = getClass().getSimpleName();

        return nameString + ':' + super.toString();
    }

    public final void start(Services<C,?> x, Executor exe) {
        if (compareAndSet(Services.ServiceState.Off, Services.ServiceState.OffToOn)) {
            exe.execute(() -> {
                try {
                    start(x.id);
                    boolean toggledOn = compareAndSet(Services.ServiceState.OffToOn, Services.ServiceState.On);
                    assert toggledOn;
                    x.change.emitAsync(pair(Service.this, true), exe);
                } catch (Throwable e) {
                    set(Services.ServiceState.Off);
                    x.logger.error("{} {}", this, e);
                }
            });
        }
    }

    public final void stop(Services<C,?> x, Executor exe, @Nullable Runnable afterOff) {
        if (compareAndSet(Services.ServiceState.On, Services.ServiceState.OnToOff)) {
            exe.execute(() -> {
                try {
                    stop(x.id);
                    boolean toggledOff = compareAndSet(Services.ServiceState.OnToOff, Services.ServiceState.Off);
                    assert toggledOff;
                    if (afterOff!=null) {
                        afterOff.run();
                    }
                    x.change.emitAsync(pair(this, false), exe);

                } catch (Throwable e) {
                    set(Services.ServiceState.Off);
                    x.logger.error("{} {}", this, e);
                }
            });
        }
    }

    abstract protected void start(C x);

    abstract protected void stop(C x);


}
