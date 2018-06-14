package jcog.meter.event;

import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.arraycopy;

public class AtomicLongGuage extends AtomicLong  {

    private volatile int[] counts;

    private volatile long sumPrev;

    public AtomicLongGuage(int auxData) {
        super(0);
        counts = new int[auxData];
    }

//    public LongMeanCounter mean(String name) {
//        return new LongMeanCounter(name);
//    }

    /** clears then returns (via the taker) the pre-cleared sum (long) and count (int) */
    public void commit(LongObjectProcedure<int[]> taker) {
        int[] c = new int[counts.length];
        long total = this.sumPrev = getAndUpdate((v)->{
            arraycopy(c, 0, counts, 0, counts.length);
            Arrays.fill(counts, 0);
            return 0;
        });

        taker.value(total, c);
    }


    public void add(long x) {
        add(x, 1);
    }

    public void add(long dSum, int... inc) {
        updateAndGet(sum -> {
            for (int i = 0; i < counts.length; i++)
                counts[i] += inc[i]; //possibly unstable volatile use AtomicIntegerArray
           return sum + dSum;
        });
    }

    /** the sum at the last commit */
    public long sumPrev() {
        return sumPrev;
    }


//    public class LongMeanCounter extends AbstractMonitor<Number> implements Counter {
//        public LongMeanCounter(String name) {
//            super(MonitorConfig.builder(name).build());
//        }
//
//        @Override
//        public Double getValue(int pollerIndex) {
//            final double[] mean = new double[1];
//            commit((sum,count)->{
//                mean[0] = ((double)sum)/count;
//            });
//            return mean[0];
//        }
//
//        @Override
//        public void increment() {
//            increment(1);
//        }
//
//        @Override
//        public void increment(long amount) {
//            add(amount);
//        }
//    }
}

