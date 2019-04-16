package jcog.data.atomic;

import java.util.function.IntUnaryOperator;

abstract public class AtomicCycle implements IntUnaryOperator {

    private final static MetalAtomicIntegerFieldUpdater<AtomicCycle> I =
            new MetalAtomicIntegerFieldUpdater(AtomicCycle.class, "i");

    /** TODO use VarHandle */
    private volatile int i;

    @Override
    public String toString() {
        return Integer.toString(I.get(this));
    }

    /** inclusive */
    abstract public int low();

    /** exclusive */
    abstract public int high();

    public static class AtomicCycleNonNegative extends AtomicCycle {
        @Override public int low() {
            return 0;
        }

        @Override public int high() {
            return Integer.MAX_VALUE;
        }
    }

    /** [0..n) */
    public static class AtomicCycleN extends AtomicCycle {
        public int n;

        public AtomicCycleN(int n) {
            assert(n > 1);
            this.n = n;
        }

        public AtomicCycleN high(int newHigh) {
            this.n = newHigh;
            return this;
        }

        @Override public int low() {
            return 0;
        }

        @Override public int high() {
            return n;
        }
    }

    public AtomicCycle() {
        super();
        set(low());
    }

    public final int get() {
        return I.get(this);
    }

    public final int getOpaque() {
        return I.getOpaque(this);
    }

    public void set(int x) {
        valid(x);
        I.set(this, x);
    }

    public int getAndSet(int x) {
        valid(x);
        return I.getAndSet(this, x);
    }

    private void valid(int x) {
        if (x < low() || x > high())
            throw new ArrayIndexOutOfBoundsException();
    }

    public final int getAndIncrement() {
        return I.getAndUpdate(this, this);
    }
    public final int incrementAndGet() {
        return I.updateAndGet(this, this);
    }

    /** spinner */
    @Override public final int applyAsInt(int x) {
        return ++x >= high() ? low() : x;
    }


// TODO
//    public <X> int getAndIncrementWith(ObjectFloatProcedure r, X x) {

}
