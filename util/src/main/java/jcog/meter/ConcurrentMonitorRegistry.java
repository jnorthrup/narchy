
package jcog.meter;

import com.google.common.collect.Sets;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.jmx.ObjectNameMapper;
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.NumericMonitor;
import jcog.Util;
import org.jetbrains.annotations.NotNull;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * MonitorRegistry implementation for Servo monitoring API
 * (not NARchy specific)
 */
public class ConcurrentMonitorRegistry implements MonitorRegistry {

    public final Set<Monitor<?>> monitors = Sets.newConcurrentHashSet();

    public void registerFields(Object x) {
        monitorFields(x, this::registerFields);
    }

    public static void monitorFields(Object x, Consumer<Monitor> o) {
        Util.getAllDeclaredFields(x, true).forEach(f -> {
            if (Monitor.class.isAssignableFrom( f.getType() )) {
                if (f.trySetAccessible()) {
                    try {
                        o.accept((Monitor) f.get(x));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        //logger.error("monitor registeration: {} {}", f, e);
                    }
                }
            }
        });
    }

    public static class WithJMX extends ConcurrentMonitorRegistry {
        private final MBeanServer mBeanServer;
        private final ConcurrentMap<MonitorConfig, Monitor<?>> monitors;
        private final String name;
        private final ObjectNameMapper mapper;
        //private final ConcurrentMap<ObjectName, Object> locks = new ConcurrentHashMap<>();

        private final AtomicBoolean updatePending = new AtomicBoolean(false);
//        private final AtomicReference<Collection<Monitor<?>>> monitorList =
//                new AtomicReference<>(UnmodifiableList.<Monitor<?>>of());

        /**
         * Creates a new instance that registers metrics with the local mbean
         * server using the default ObjectNameMapper {@link ObjectNameMapper#DEFAULT}.
         *
         * @param name the registry name
         */
        public WithJMX(String name) {
            this(name, ObjectNameMapper.DEFAULT);
        }

        /**
         * Creates a new instance that registers metrics with the local mbean
         * server using the ObjectNameMapper provided.
         *
         * @param name   the registry name
         * @param mapper the monitor to object name mapper
         */
        public WithJMX(String name, ObjectNameMapper mapper) {

            this.name = name;
            this.mapper = mapper;
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
            monitors = new ConcurrentHashMap<>();
        }

//        @Override
//        public void register(@NotNull Monitor<?> monitor) {
//            super.register(monitor);
//        }

        private void register(ObjectName objectName, DynamicMBean mbean) throws InstanceNotFoundException, MBeanRegistrationException, InstanceAlreadyExistsException, NotCompliantMBeanException {
            //synchronized (getLock(objectName)) {
            if (mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
            }
            mBeanServer.registerMBean(mbean, objectName);
            //}
        }

        @Override
        protected void onAdd(Monitor<?> monitor) {
            createMBeans(name, monitor, mapper).forEach(bean -> {
                try {
                    register(bean.getObjectName(), bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            monitors.put(monitor.getConfig(), monitor);
            updatePending.set(true);

        }

        /**
         * Create a set of MBeans for a {@link com.netflix.servo.monitor.Monitor}. This method will
         * recursively select all of the sub-monitors if a composite type is used.
         *
         * @param domain  passed in to the object name created to identify the beans
         * @param monitor monitor to expose to jmx
         * @param mapper  the mapper which maps the Monitor to ObjectName
         * @return flattened list of simple monitor mbeans
         */
        public static List<MonitorMBean> createMBeans(String domain, Monitor<?> monitor,
                                                      ObjectNameMapper mapper) {
            List<MonitorMBean> mbeans = new ArrayList<>();
            createMBeans(mbeans, domain, monitor, mapper);
            return mbeans;
        }


        protected static void createMBeans(List<MonitorMBean> mbeans, String domain, Monitor<?> monitor,
                                           ObjectNameMapper mapper) {
            if (monitor instanceof CompositeMonitor<?>) {
                for (Monitor<?> m : ((CompositeMonitor<?>) monitor).getMonitors()) {
                    createMBeans(mbeans, domain, m, mapper);
                }
            } else {
                mbeans.add(new MonitorMBean(domain, monitor, mapper));
            }
        }

        /**
         * Exposes a {@link com.netflix.servo.monitor.Monitor} as an MBean that can be registered with JMX.
         */
        public static class MonitorMBean implements DynamicMBean {


            private final Monitor<?> monitor;

            private final ObjectName objectName;

            private final MBeanInfo beanInfo;

            /**
             * Create an MBean for a {@link com.netflix.servo.monitor.Monitor}.
             *
             * @param domain  passed in to the object name created to identify the beans
             * @param monitor monitor to expose to jmx
             * @param mapper  the mapper which maps the monitor to ObjectName
             */
            MonitorMBean(String domain, Monitor<?> monitor, ObjectNameMapper mapper) {
                this.monitor = monitor;
                this.objectName = createObjectName(mapper, domain);
                this.beanInfo = createBeanInfo();
            }

            /**
             * Returns the object name built from the {@link com.netflix.servo.monitor.MonitorConfig}.
             */
            public ObjectName getObjectName() {
                return objectName;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getAttribute(String name) {
                return monitor.getValue();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void setAttribute(Attribute attribute) {
                throw new UnsupportedOperationException("setAttribute is not implemented");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public AttributeList getAttributes(String[] names) {
                AttributeList list = new AttributeList();
                for (String name : names) {
                    list.add(new Attribute(name, monitor.getValue()));
                }
                return list;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public AttributeList setAttributes(AttributeList list) {
                throw new UnsupportedOperationException("setAttributes is not implemented");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object invoke(String name, Object[] args, String[] sig) {
                throw new UnsupportedOperationException("invoke is not implemented");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public MBeanInfo getMBeanInfo() {
                return beanInfo;
            }

            private ObjectName createObjectName(ObjectNameMapper mapper, String domain) {
                return mapper.createObjectName(domain, monitor);
            }

            private MBeanInfo createBeanInfo() {
                MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[1];
                attrs[0] = createAttributeInfo(monitor);
                return new MBeanInfo(
                        this.getClass().getName(),
                        "MonitorMBean",
                        attrs,
                        null,  // constructors
                        null,  // operators
                        null); // notifications
            }

            private MBeanAttributeInfo createAttributeInfo(Monitor<?> m) {
                final String type = (m instanceof NumericMonitor<?>)
                        ? Number.class.getName()
                        : String.class.getName();
                return new MBeanAttributeInfo(
                        "value",
                        type,
                        m.getConfig().toString(),
                        true,   // isReadable
                        false,  // isWritable
                        false); // isIs
            }
        }

    }

    /**
     * The set of registered Monitor objects.
     * be careful to not modify it. otherwise it will have to be wrapped in Unmodifidable and that reduces perf
     */
    @Override
    public Collection<Monitor<?>> getRegisteredMonitors() {
        return monitors;// UnmodifiableList.copyOf(monitors);
    }

    /**
     * Register a new monitor in the registry.
     */
    @Override
    public void register(@NotNull Monitor<?> monitor) {
        //Preconditions.checkNotNull(monitor, "monitor");
        try {
            if (monitors.add(monitor)) {
                onAdd(monitor);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    protected void onAdd(Monitor<?> monitor) {


    }

    protected void onRemove(Monitor<?> monitor) {

    }

    /**
     * Unregister a Monitor from the registry.
     */
    @Override
    public void unregister(@NotNull Monitor<?> monitor) {
        //Preconditions.checkNotNull(monitor, "monitor");
        try {
            if (monitors.remove(monitor)) {
                onRemove(monitor);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    @Override
    public boolean isRegistered(Monitor<?> monitor) {
        return monitors.contains(monitor);
    }
}
