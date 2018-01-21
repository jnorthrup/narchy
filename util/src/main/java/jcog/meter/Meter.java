package jcog.meter;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.util.Clock;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * use this to meter Monitor fields of a class, ex:
 *
 * public final Counter deriveEval = new FastCounter("event");

 */
public interface Meter { ;


    public static MonitorConfig meter(String name) {
        return MonitorConfig.builder(name).build();
    }

    default Runnable printer(PrintStream out) {
        return printer(new FastMonitorRegistry(this), out);
    }

    default Runnable getter(Supplier<Map<String,Object>> each) {
        return getter(new FastMonitorRegistry(this), each);
    }

    default Runnable printer(MonitorRegistry reg, PrintStream p) {
        return new PollRunnable(
                new MonitorRegistryMetricPoller(reg),
                BasicMetricFilter.MATCH_ALL,
                new MetricsPrinter(name(), clock(), p)
        );
    }
    default Runnable getter(MonitorRegistry reg, Supplier<Map<String,Object>> p) {
        return new PollRunnable(
                new MonitorRegistryMetricPoller(reg),
                BasicMetricFilter.MATCH_ALL,
                new MetricsMapper(name(), clock(), p)
        );
    }

    Clock clock();
    String name();

}
