package jcog.meter;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import jcog.event.ListTopic;
import jcog.event.Topic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static jcog.meter.Meter.meter;

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
        getAndAdd(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Number getValue(int pollerIdx) {
        return get();
    }

    @Override
    public final Number getValue() {
        return get();
    }

    @Override
    public final MonitorConfig getConfig() {
        return config;
    }

    public static class ExplainedCounter<E> extends FastCounter {

        public final Topic<E> why = new ListTopic<>();

        public ExplainedCounter(String name) {
            super(name);
        }


        public void increment(Supplier<E> explainer) {
            super.increment();
            if (!why.isEmpty()) {
                why.emit(explainer.get());
            }
        }
    }
}
