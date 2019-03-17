package jcog.data.atomic;

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
    public static class AtomicCycleN extends AtomicCycle {
        public final int n;

        public AtomicCycleN(int n) {
            assert(n > 1);
            this.n = n;
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
//            if (y < 0)
//                y = 0;
            if (y >= high())
                y = low();
            return y;
        });
    }

}
