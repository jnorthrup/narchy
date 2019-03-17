//
//package jcog.signal.meter;
//
//import com.google.common.collect.Sets;
//import com.netflix.servo.MonitorRegistry;
//import com.netflix.servo.monitor.Monitor;
//import jcog.util.Reflect;
//import org.jetbrains.annotations.NotNull;
//
//import java.lang.reflect.Field;
//import java.util.Collection;
//import java.util.Set;
//import java.util.function.Consumer;
//
///**
// * MonitorRegistry implementation for Servo monitoring API
// * (not NARchy specific)
// */
//public class ConcurrentMonitorRegistry implements MonitorRegistry {
//
//    public final Set<Monitor<?>> monitors = Sets.newConcurrentHashSet();
//
//
//
//
//
//    public static void monitorFields(Object x, Consumer<Monitor> o) {
//        Reflect.on(x.getClass()).fields(true, true, true).forEach((s,ff)->{
//            Field f = ff.get();
//            if (Monitor.class.isAssignableFrom( f.getType() )) {
//                if (f.trySetAccessible()) {
//                    try {
//                        o.accept((Monitor) f.get(x));
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//
//                    }
//                }
//            }
//        });
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    /**
//     * The set of registered Monitor objects.
//     * be careful to not modify it. otherwise it will have to be wrapped in Unmodifidable and that reduces perf
//     */
//    @Override
//    public Collection<Monitor<?>> getRegisteredMonitors() {
//        return monitors;
//    }
//
//    /**
//     * Register a new monitor in the registry.
//     */
//    @Override
//    public void register(@NotNull Monitor<?> monitor) {
//
//        try {
//            if (monitors.add(monitor)) {
//                onAdd(monitor);
//            }
//        } catch (RuntimeException e) {
//            throw new IllegalArgumentException("invalid object", e);
//        }
//    }
//
//    protected void onAdd(Monitor<?> monitor) {
//
//
//    }
//
//    protected void onRemove(Monitor<?> monitor) {
//
//    }
//
//    /**
//     * Unregister a Monitor from the registry.
//     */
//    @Override
//    public void unregister(@NotNull Monitor<?> monitor) {
//
//        try {
//            if (monitors.remove(monitor)) {
//                onRemove(monitor);
//            }
//        } catch (RuntimeException e) {
//            throw new IllegalArgumentException("invalid object", e);
//        }
//    }
//
//    @Override
//    public boolean isRegistered(Monitor<?> monitor) {
//        return monitors.contains(monitor);
//    }
//}
