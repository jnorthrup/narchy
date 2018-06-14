package jcog.meter.event;

import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongGuage extends AtomicLong  {

    private volatile int count = 0;

    private volatile long sumPrev;

    public AtomicLongGuage() {
        super(0);
    }

    public LongMeanCounter mean(String name) {
        return new LongMeanCounter(name);
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
    public void commit() {
        this.sumPrev = getAndUpdate((v)->{
            count = 0;
            return 0;
        });
    }

    public void add(long x) {
        add(x, 1);
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
    /** the sum at the last commit */
    public double meanPrev() {
        return ((double)sumPrev)/count;
    }

    public class LongMeanCounter extends AbstractMonitor<Number> implements Counter {
        public LongMeanCounter(String name) {
            super(MonitorConfig.builder(name).build());
        }

        @Override
        public Double getValue(int pollerIndex) {
            final double[] mean = new double[1];
            commit((sum,count)->{
                mean[0] = ((double)sum)/count;
            });
            return mean[0];
        }

        @Override
        public void increment() {
            increment(1);
        }

        @Override
        public void increment(long amount) {
            add(amount);
        }
    }
}

