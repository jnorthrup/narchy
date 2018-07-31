package jcog.signal.meter;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;

import java.util.concurrent.atomic.AtomicLong;

import static jcog.signal.meter.Meter.meter;

public class FastCounter extends AtomicLong implements Monitor<Number>, Counter {
    protected final MonitorConfig config;


    public FastCounter(String name) {
        this(meter(name));
    }

    public FastCounter(MonitorConfig config) {
        this.config = config;
    }

    @Override
    public final void increment() {
        incrementAndGet();
    }

    @Override
    public void increment(long amount) {
        addAndGet(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Number getValue(int pollerIdx) {
        return getValue();
    }

    @Override
    public final Number getValue() {
        return getOpaque();
    }

    @Override
    public final MonitorConfig getConfig() {
        return config;
    }

}
