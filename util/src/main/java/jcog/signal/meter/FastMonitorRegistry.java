//package jcog.signal.meter;
//
//import com.netflix.servo.Metric;
//import com.netflix.servo.MonitorRegistry;
//import com.netflix.servo.monitor.Monitor;
//import jcog.data.list.FasterList;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Comparator;
//import java.util.List;
//
//public final class FastMonitorRegistry implements MonitorRegistry {
//
//    private final FasterList<Monitor<?>> monitors;
//
//    public FastMonitorRegistry(Object x) {
//        monitors = new FasterList<>();
//        ConcurrentMonitorRegistry.monitorFields(x, monitors::add);
//        this.monitors.sortThis(Comparator.comparing((m)->m.getConfig().getName()));
//    }
//
//    public FastMonitorRegistry(Collection<Monitor<?>> monitors) {
//        this.monitors = new FasterList<>(monitors);
//        this.monitors.sortThis(Comparator.comparing((m)->m.getConfig().getName()));
//    }
//
//    @Override
//    public List<Monitor<?>> getRegisteredMonitors() {
//        return monitors;
//    }
//    public List<Metric> getMetrics(long now) {
//        List<Monitor<?>> monitors = getRegisteredMonitors();
//        List<Metric> metrics = new ArrayList<>(monitors.size());
//        for (Monitor<?> monitor : monitors) {
//            Object v = monitor.getValue();
//            if (v != null) {
//                metrics.add(new Metric(monitor.getConfig(), now, v));
//            }
//        }
//        return metrics;
//    }
//
//    @Override
//    public void register(Monitor<?> monitor) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void unregister(Monitor<?> monitor) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public boolean isRegistered(Monitor<?> monitor) {
//        throw new UnsupportedOperationException();
//    }
//}
