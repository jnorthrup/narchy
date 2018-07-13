package jcog.signal.meter;

import com.netflix.servo.Metric;
import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.util.Clock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MetricsMapper extends BaseMetricObserver {

    private final Clock clock;
    private final Supplier<Map<String, Object>> target;


    /**
     * Creates a new instance that stores files in {@code dir} with a name that
     * is created using {@code namePattern}.
     *
     * @param name        name of the observer
     * @param namePattern date format pattern used to create the file names
     * @param dir         directory where observations will be stored
     * @param compress    whether to compress our output
     * @param clock       clock instance to use for getting the time used in the filename
     */

    public MetricsMapper(String name, Clock clock, Supplier<Map<String, Object>> target) {
        super(name);
        this.clock = clock;
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateImpl(List<Metric> metrics) {
        Map<String, Object> map = target.get();
        update(metrics, map);
    }

    protected void update(List<Metric> metrics, Map<String, Object> map) {
        for (Metric m : metrics) {
            String n = name(m);
            if (n!=null) {
                Object v = value(m);
                if (v!=null)
                    map.put(n, v);
            }
        }
        map.put("now", clock.now());
    }

    @Nullable protected Object value(Metric m) {
        return m.getValue();
    }

    @Nullable protected String name(Metric m) {
        return m.getConfig().getName();
    }
}

