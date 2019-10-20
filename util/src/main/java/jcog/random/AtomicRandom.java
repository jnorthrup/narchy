package jcog.random;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Random;

/** base class for Random implementations that apply a busy spin wait to ensure updates to state are atomic.
 * this doesnt seem to perform good at all.  not recommended
 * */
public abstract class AtomicRandom extends Random {

    private static final VarHandle BUSY;
    static {
        try {
            BUSY = MethodHandles.lookup().findVarHandle(AtomicRandom.class, "busy", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile int busy;

    public AtomicRandom() {
        super();
    }

    public AtomicRandom(long seed) {
        super(seed);
    }

    @Override
    public final long nextLong() {
        enter();

        var l = _nextLong();

        exit();

        return l;
    }

    @Override
    public final void setSeed(long seed) {
        enter();

        _setSeed(seed);

        exit();
    }

    private void exit() {
        BUSY.set(this, 0);
    }

    private void enter() {
        while (!BUSY.compareAndSet(this, 0, 1))
            Thread.onSpinWait();
    }

    protected abstract long _nextLong();

    protected abstract void _setSeed(long seed);
}
