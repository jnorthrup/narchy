package jcog.service;

import jcog.WTF;
import jcog.event.Off;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public abstract class Service<C> extends AtomicReference<Services.ServiceState> implements Off {

    public final boolean isOn() {
        return getOpaque() == Services.ServiceState.On;
    }

    public final boolean isOff() {
        return getOpaque() == Services.ServiceState.Off;
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

    public final void start(Services<C,?> x) {
        start(x, x.exe);
    }

    public final void start(Services<C,?> x, Executor exe) {

        if (compareAndSet(Services.ServiceState.Off, Services.ServiceState.OffToOn)) {

            exe.execute(() -> {
                try {

                    start(x.id);

                    boolean toggledOn = compareAndSet(Services.ServiceState.OffToOn, Services.ServiceState.On);
                    if (!toggledOn)
                        throw new WTF();

                    x.change.emitAsync(pair(Service.this, true), exe);

                } catch (Throwable e) {
                    set(Services.ServiceState.Off);
                    x.logger.error("{} {}", Service.this, e);
                }
            });
        }
    }

    public final void stop(Services<C,?> x) {
        stop(x, null);
    }

    public final void stop(Services<C,?> x, @Nullable Runnable afterOff) {
        stop(x, x.exe, afterOff);
    }

    public final void stop(Services<C,?> x, Executor exe, @Nullable Runnable afterOff) {
        if (compareAndSet(Services.ServiceState.On, Services.ServiceState.OnToOff)) {
            exe.execute(() -> {
                try {

                    stop(x.id);

                    boolean toggledOff = compareAndSet(Services.ServiceState.OnToOff, Services.ServiceState.Off);
                    if (!toggledOff)
                        throw new WTF();

                    if (afterOff!=null)
                        afterOff.run();

                    x.change.emitAsync(pair(Service.this, false), exe);

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
