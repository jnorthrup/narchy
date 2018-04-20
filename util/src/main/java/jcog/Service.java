package jcog;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public abstract class Service<C> extends AtomicReference<Services.ServiceState> {

    public boolean isOn() {
        return get() == Services.ServiceState.On;
    }

    public boolean isOff() {
        return get() == Services.ServiceState.Off;
    }

    protected Service() {
        super(Services.ServiceState.Off);
    }

    @Override
    public String toString() {
        String nameString = getClass().getName();

        //quick common package name filters
        if (nameString.startsWith("jcog.") || nameString.startsWith("nars."))
            nameString = getClass().getSimpleName();

        return nameString + ':' + super.toString();
    }

    public final <S extends Services<C,?,Service<C>>> void start(S x, Executor exe) {
        if (compareAndSet(Services.ServiceState.Off, Services.ServiceState.OffToOn)) {
            exe.execute(() -> {
                try {
                    start(x.id);
                    boolean toggledOn = compareAndSet(Services.ServiceState.OffToOn, Services.ServiceState.On);
                    assert toggledOn;
                    x.change.emit(pair(Service.this, true));
                } catch (Throwable e) {
                    set(Services.ServiceState.Deleted);
                    x.logger.error("{} {}", this, e);
                }
            });
        }
    }

    public final <S extends Services<C,?,Service<C>>>  void stop(S x, Executor exe, @Nullable Runnable afterDelete) {
        if (compareAndSet(Services.ServiceState.On, Services.ServiceState.OnToOff)) {
            exe.execute(() -> {
                try {
                    stop(x.id);
                    boolean toggledOff = compareAndSet(Services.ServiceState.OnToOff, Services.ServiceState.Off);
                    assert toggledOff;
                    if (afterDelete!=null) {
                        set(Services.ServiceState.Deleted);
                        afterDelete.run();
                    }
                    x.change.emit(pair(this, false));

                } catch (Throwable e) {
                    set(Services.ServiceState.Deleted);
                    x.logger.error("{} {}", this, e);
                }
            });
        }
    }

    abstract protected void start(C x);

    abstract protected void stop(C x);


}
