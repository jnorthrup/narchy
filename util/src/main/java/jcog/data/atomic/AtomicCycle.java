package jcog.data.atomic;

import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;

import java.util.concurrent.atomic.AtomicInteger;

abstract public class AtomicCycle  {

    final AtomicInteger i = new AtomicInteger();

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

    public int get() {
        return i.get();
    }

    public int getOpaque() {
        return i.getOpaque();
    }

    public void set(int x) {
        valid(x);
        i.set(x);
    }

    public int getAndSet(int x) {
        valid(x);
        return i.getAndSet(x);
    }

    protected void valid(int x) {
        if (x < low() || x > high())
            throw new ArrayIndexOutOfBoundsException();
    }

    public int getAndIncrement() {
        return i.getAndUpdate(x -> {
            int y = x + 1;
            if (y >= high())
                y = low();
            return y;
        });
    }

    /** procedure receives the index of the next target */
    public int getAndIncrement(IntProcedure r) {
        return i.getAndUpdate(x -> {
            int y = x + 1;
            if (y >= high())
                y = low();
            r.value(y);
            return y;
        });
    }

// TODO
//    public <X> int getAndIncrementWith(ObjectFloatProcedure r, X x) {

}
