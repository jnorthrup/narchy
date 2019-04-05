/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package jcog.service;

import jcog.WTF;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.exe.Exe;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * CONTRAINER / OBJENOME
 * A collection or container of 'parts'.
 * Smart Dependency Injection (DI) container with:
 *
 *      autowiring
 *          type resolution assisted by CastGraph, with diverse of builtin transformers
 *
 *      hints from commandline args, env variables, or constructor string:
 *
 *             "parts={<key>:<value>,[<key>:<value>] }"
 *                 key = interface name | variable name
 *                 value = JSON-parseable java constant

 *         the hints keys and values are fuzzy matchable, with levenshtein dist as a decider in case of ambiguity
 *
 *         note: such syntax should be parseable both in JSON and NAL
 *
 *
 * Original docs:
 * <p>
 * A manager for monitoring and controlling a set of {@linkplain Part services}. This class
 * provides methods for {@linkplain #startAsync() starting}, {@linkplain #stopAsync() stopping} and
 * {@linkplain #servicesByState inspecting} a collection of {@linkplain Part services}.
 * Additionally, users can monitor state transitions with the {@linkplain Listener listener}
 * mechanism.
 * <p>
 * <p>While it is recommended that service lifecycles be managed via this class, state transitions
 * initiated via other mechanisms do not impact the correctness of its methods. For example, if the
 * services are started by some mechanism besides {@link #startAsync}, the listeners will be invoked
 * when appropriate and {@link #awaitHealthy} will still work as expected.
 * <p>
 * <p>Here is a simple example of how to use a {@code ServiceManager} to start a server.
 * <pre>   {@code
 * class Server {
 *   public static void main(String[] args) {
 *     Set<Service> services = ...;
 *     ServiceManager manager = new ServiceManager(services);
 *     manager.addListener(new Listener() {
 *         public void stopped() {}
 *         public void healthy() {
 *
 *         }
 *         public void failure(Service service) {
 *
 *
 *           System.exit(1);
 *         }
 *       },
 *       MoreExecutors.directExecutor());
 *
 *     Runtime.getRuntime().addShutdownHook(new Thread() {
 *       public void run() {
 *
 *
 *         try {
 *           manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
 *         } catch (TimeoutException timeout) {
 *
 *         }
 *       }
 *     });
 *     manager.startAsync();
 *   }
 * }}</pre>
 * <p>
 * <p>This class uses the ServiceManager's methods to start all of its services, to respond to
 * service failure and to ensure that when the JVM is shutting down all the services are stopped.
 *
 * @author Luke Sandberg (original)
 */
public class Parts<C /* context */, K /* service key */> {

    public final C id;
    final Logger logger;
    protected final Executor exe;
    public final Topic<ObjectBooleanPair<Part<C>>> change = new ListTopic<>();
    private final ConcurrentMap<K, Part<C>> services;


    public Parts(C id) {
        this(id,
                Exe.executor()
                //MoreExecutors.directExecutor()
                /*ForkJoinPool.commonPool()*/
        );
    }

    /**
     * Constructs a new instance for managing the given services.
     *
     * @param services The services to manage
     * @param x
     * @throws IllegalArgumentException if not all services are {@linkplain ServiceState#NEW new} or if there
     *                                  are any duplicate services.
     */
    public Parts(@Nullable C id, Executor exe) {
        this.id = id == null ? (C) this : id;
        this.logger = LoggerFactory.getLogger(id.toString());
        this.exe = exe;
        this.services = new ConcurrentHashMap<>(32);
    }

    public final Stream<Part<C>> stream() {
        return services.values().stream();
    }

    public final Set<Map.Entry<K, Part<C>>> entrySet() {
        return services.entrySet();
    }

    public final void add(K key, Part<C> s) {
        set(key, s, true);
    }

    public final void remove(K serviceID) {
        remove(serviceID, null);
    }

    public final void remove(K serviceID, Part<C> s) {
        set(serviceID, s, false);
    }

    private void set(K key, @Nullable Part<C> added, boolean start) {

        if (added == null && start)
            throw new WTF();

        Part<C> removed = added!=null ? services.put(key, added) : services.remove(key);

        if (added!=removed) {
            if (removed != null) {

                removed.stop(this, start ? () -> added.start(this, exe) : null);

            } else {

                if (start)
                    added.start(this);
            }
        } else {
            if (start && added.isOff()) {
                added.start(this);
            } else if (!start && added.isOn()) {
                added.stop(this);
            }
        }
    }



    public Parts<C, K> stop() {
        for (Part<C> part : services.values()) {
            part.stop(this);
        }
        return this;
    }

    public int size() {
        return services.size();
    }


    public void print(PrintStream out) {
        services.forEach((k, s) -> out.println(k + " " + s.state()));
    }

    void error(@Nullable Part part, Throwable e, String what) {
        if (part!=null)
            logger.error("{} {} {} {}", part, what, this, e);
        else
            logger.error("{} {} {}", what, this, e);
    }

    public void start(Part<C> part, Executor exe) {
        exe.execute(() -> {
            try {

                part.start(id);

                boolean toggledOn = part.state.compareAndSet(Parts.ServiceState.OffToOn, Parts.ServiceState.On);
                if (!toggledOn)
                    throw new WTF();

                change.emitAsync(pair(part, true), exe);

            } catch (Throwable e) {
                part.state.set(Parts.ServiceState.Off);
                error(part, e, "start");
            }
        });
    }

    public void stop(Part<C> part, @Nullable Runnable afterOff, Executor exe) {
        exe.execute(() -> {
            try {

                part.stop(id);

                boolean toggledOff = part.state.compareAndSet(Parts.ServiceState.OnToOff, Parts.ServiceState.Off);
                if (!toggledOff)
                    throw new WTF();

                if (afterOff!=null)
                    afterOff.run();

                change.emitAsync(pair(part, false), exe);

            } catch (Throwable e) {
                part.state.set(Parts.ServiceState.Off);
                error(part, e, "stop");
            }
        });
    }


    enum ServiceState {
        Off {
            @Override
            public String toString() {
                return "-";
            }
        },
        OffToOn {
            @Override
            public String toString() {
                return "-+";
            }
        },
        On {
            @Override
            public String toString() {
                return "+";
            }
        },
        OnToOff {
            @Override
            public String toString() {
                return "+-";
            }
        }
    }

}