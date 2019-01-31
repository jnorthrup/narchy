package jcog.random;

import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;

import java.util.Random;

/** base class for Random implementations that apply a busy spin wait to ensure updates to state are atomic */
abstract public class AtomicRandom extends Random {
    private static final MetalAtomicIntegerFieldUpdater<AtomicRandom> BUSY =
            new MetalAtomicIntegerFieldUpdater(AtomicRandom.class, "busy");

    private volatile int busy = 0;

    public AtomicRandom() {
        super();
    }

    public AtomicRandom(long seed) {
        super(seed);
    }

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
        BUSY.lazySet(this,0);
    }

    private void enter() {
        while (!BUSY.weakCompareAndSet(this, 0, 1))
            Thread.onSpinWait();
    }

    abstract protected long _nextLong();

    abstract protected void _setSeed(final long seed);
}
