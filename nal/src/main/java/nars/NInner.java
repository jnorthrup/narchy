package nars;

import com.netflix.servo.Metric;
import com.netflix.servo.monitor.*;
import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.Reflection;
import com.netflix.servo.util.Throwables;
import jcog.event.On;
import jcog.exe.Loop;
import jcog.list.FasterList;
import jcog.meter.event.BufferedFloatGuage;
import nars.util.ConcurrentMonitorRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * inner sense: low-level internal experience
 */
public class NInner extends ConcurrentMonitorRegistry/*.WithJMX*/ {

    static final Logger logger = LoggerFactory.getLogger(NInner.class);

    private final NAR nar;
    //private final MetricPoller poller;
    private On onCycle;

    public NInner(NAR n) {
        //super("NAR." + n.self());
        this.nar = n;

//        this.poller =
//                new MonitorRegistryMetricPoller(this);


        register(new BasicCompositeMonitor(id("emotion"), new FasterList(nar.emotion.getRegisteredMonitors())));

        //MetricObserver obs = new FileMetricObserver("stats", directory);

//            PollScheduler scheduler = PollScheduler.getInstance();
//            scheduler.start();


//            MetricObserver transform = new CounterToRateMetricTransform(
//                    obs, 1, TimeUnit.SECONDS);
        PollRunnable task = nar.emotion.printer(this, System.out);
        new Loop(2000) {

            @Override
            public boolean next() {
                task.run();
                return true;
            }
        };
        //scheduler.addPoller(task, 2, TimeUnit.SECONDS);
    }

//    public List<Metric> meter() {
//        return poller.poll(BasicMetricFilter.MATCH_ALL);
//    }

    protected void cycle() {
    }

    public void start() {
        synchronized (nar) {
            assert (onCycle == null);
            onCycle = nar.onCycle(this::cycle);
        }
    }

    public void stop() {
        synchronized (nar) {
            assert (onCycle != null);
            onCycle.off();
            onCycle = null;
        }
    }


    /**
     * Writes observations to a file. The format is a basic text file with tabs
     * separating the fields.
     */
    public static class PrintStreamMetricObserver extends BaseMetricObserver {

        private final Clock clock;
        private final PrintStream out;


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

        public PrintStreamMetricObserver(String name, Clock clock, PrintStream out) {
            super(name);
            this.clock = clock;
            this.out = out;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updateImpl(List<Metric> metrics) {
            out.println(clock.now());
            for (Metric m : metrics) {
                out.append(m.getConfig().getName()).append('\t')
                        .append(m.getValue().toString()).append('\t')
                        .append(m.getConfig().getTags().toString()).append('\n');
            }
        }
    }

    /**
     * Extract all fields of {@code obj} that are of type {@link Monitor} and add them to
     * {@code monitors}.
     */
    @Nullable
    static CompositeMonitor monitor(String id, Object obj, Tag... tags ) {
        //final TagList tags = getMonitorTags(obj);

        Class c = obj.getClass();
        final MonitorConfig.Builder builder = MonitorConfig.builder(id);
        final String className = c.getName();
        if (!className.isEmpty()) {
            builder.withTag("class", className);
        }

        if (tags.length > 0) {
            builder.withTags(new BasicTagList(List.of(tags)));
        }


        List<Monitor<?>> monitors = new FasterList<>();


        //final String objectId = (id == null) ? DEFAULT_ID : id;


        try {
//            final SortedTagList.Builder builder = SortedTagList.builder();
//            builder.withTag("class", (obj.getClass()).getSimpleName());
//            if (id != null) {
//                builder.withTag("id", id);
//            }
//            //final TagList classTags = builder.builder();

            final Set<Field> fields = Reflection.getAllFields(obj.getClass());
            for (Field field : fields) {
                Collection<? extends Monitor<?>> f = fieldMonitor(field, obj);
                if (f!=null)
                    monitors.addAll(f);
            }
        } catch (RuntimeException e) {
            throw Throwables.propagate(e);
        }

        if (!monitors.isEmpty()) {
            logger.info("monitor {}", monitors);
            return new BasicCompositeMonitor(builder.build(), monitors);
        }

        return null;
    }



    static Collection<? extends Monitor<?>> fieldMonitor(Field f, Object obj) {
        Class type = f.getType();
        if (BufferedFloatGuage.class.isAssignableFrom(type)) {
            if (f.trySetAccessible()) {
                return singleton(new BasicGauge<>(
                        MonitorConfig.builder(f.toString()).build(),
                        () -> ((BufferedFloatGuage) f.get(obj)).getMean()
                ));
            }
        }
        return null;
    }

    public static MonitorConfig id(String name) {
        return MonitorConfig.builder(name).build();
    }

}
