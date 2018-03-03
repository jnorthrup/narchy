package jcog.meter.event;

import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongGuage extends AtomicLong  {

    private volatile int count = 0;

    private volatile long sumPrev;

    public AtomicLongGuage() {
        super(0);
    }


    /** clears then returns (via the taker) the pre-cleared sum (long) and count (int) */
    public void commit(LongIntProcedure taker) {
        int[] c = new int[1];
        long total = this.sumPrev = getAndUpdate((v)->{
            c[0] = count;
            count = 0;
            return 0;
        });

        taker.value(total, c[0]);
    }

    public void add(long dSum, int dCount) {
        updateAndGet(sum -> {
           count+=dCount;
           return sum + dSum;
        });
    }

    /** the sum at the last commit */
    public long sumPrev() {
        return sumPrev;
    }
}

