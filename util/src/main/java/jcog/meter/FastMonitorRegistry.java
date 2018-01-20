package jcog.meter;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitor;
import jcog.list.FasterList;

import java.util.Collection;
import java.util.Comparator;

public final class FastMonitorRegistry implements MonitorRegistry {

    private final FasterList<Monitor<?>> monitors;

    public FastMonitorRegistry(Object x) {
        monitors = new FasterList<>();
        ConcurrentMonitorRegistry.monitorFields(x, monitors::add);
        monitors.compact();
    }

    public FastMonitorRegistry(Collection<Monitor<?>> monitors) {
        this.monitors = new FasterList<>(monitors);
        this.monitors.sortThis(Comparator.comparing((m)->m.getConfig().getName()));
    }

    @Override
    public Collection<Monitor<?>> getRegisteredMonitors() {
        return monitors;
    }

    @Override
    public void register(Monitor<?> monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(Monitor<?> monitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRegistered(Monitor<?> monitor) {
        throw new UnsupportedOperationException();
    }
}
