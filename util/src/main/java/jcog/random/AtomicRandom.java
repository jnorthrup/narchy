package jcog.random;

import jcog.data.atomic.AwesomeAtomicIntegerFieldUpdater;

import java.util.Random;

/** base class for Random implementations that apply a busy spin wait to ensure updates to state are atomic */
abstract public class AtomicRandom extends Random {
    private static final AwesomeAtomicIntegerFieldUpdater<AtomicRandom> business =
            new AwesomeAtomicIntegerFieldUpdater(AtomicRandom.class, "busy");

    private volatile int busy = 0;


    @Override
    public final long nextLong() {
        enter();

        long l = _nextLong();

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
        business.set(this, 0);
    }

    private void enter() {
        while (!business.compareAndSet(this, 0, 1))
            Thread.onSpinWait();
    }

    abstract protected long _nextLong();

    abstract protected void _setSeed(final long seed);
}
