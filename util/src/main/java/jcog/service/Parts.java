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

import com.google.common.util.concurrent.MoreExecutors;
import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.exe.Exe;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * CONTRAINER / OBJENOME
 * A collection or container of 'parts'.
 * Smart Dependency Injection (DI) container with:
 * 
 * autowiring
 * type resolution assisted by CastGraph, with diverse of builtin transformers
 * 
 * hints from commandline args, env variables, or constructor string:
 * 
 * "parts={<key>:<value>,[<key>:<value>] }"
 * key = interface name | variable name
 * value = JSON-parseable java constant
 * 
 * the hints keys and values are fuzzy matchable, with levenshtein dist as a decider in case of ambiguity
 * 
 * note: such syntax should be parseable both in JSON and NAL
 *
 * @param K service key
 * @param C service context
 *
 */
public class Parts<K /* service key */, C /* context */  > {

    private final C id;
    public final Topic<ObjectBooleanPair<Part<C>>> eventAddRemove = new ListTopic<>();
    public final Executor executor;
    public final Logger logger;
    protected final ConcurrentMap<K, Part<C>> parts;

    protected Parts() {
        this(ForkJoinPool.commonPool());
    }

    protected Parts(Executor executor) {
        this(null, executor);
    }

    /**
     * Constructs a new instance for managing the given services.
     *
     * @param services The services to manage
     * @param x
     * @throws IllegalArgumentException if not all services are {@linkplain ServiceState#NEW new} or if there
     *                                  are any duplicate services.
     */
    private Parts(@Nullable C id, Executor executor) {
        if (id == null)
            id = (C)this; //attempts cast

        this.id = id;
        this.logger = Util.logger(id.toString());
        this.executor = executor;
        this.parts = new ConcurrentHashMap<>();
    }

    public Parts(@Nullable C id) {
        this(id,
                Exe.concurrent() ? Exe.executor() : MoreExecutors.directExecutor()
                //MoreExecutors.directExecutor()
                /*ForkJoinPool.commonPool()*/
        );
    }


    /** restart an already added part */
    public final boolean start(K key) {
        tryStart(part(key));
        return false;
    }


    public final Part<C> part(K key) {
        return parts.get(key);
    }




    /** add and starts it */
    public final boolean start(K key, Part<C> instance) {
        return set(key, instance, true);
    }

    /** tries to add the new instance, replacing any existing one, but doesnt start */
    public final boolean add(K key, Part<C> instance) {
        return set(key, instance, false);
    }

    public final boolean remove(K k) {
        return set(k, (Part)null, false);
    }
    public boolean stop(K k) {
        return set(k, part(k), false);
    }

//    public final boolean add(K key, Function<K, ? extends Part<C>> builder) {
//        return add(key, builder.apply(key));
//    }

    public final Part<C> start(K key, Class<? extends Part<C>> instanceOf) {
        return set(key, instanceOf, true);
    }

    public final Part<C> add(K key, Class<? extends Part<C>> instanceOf) {
        return set(key, instanceOf, false);
    }

    public final Part<C> set(K key, Class<? extends Part<C>> instanceOf, boolean start) {
        Part<C> p = build(key, instanceOf).get();
        if (set(key, p, start))
            return p;
        else
            throw new WTF();
    }


    /** stops all parts (but does not remove them) */
    public Parts<K, C> stopAll() {
        parts.keySet().forEach(this::stop);
        return this;
    }

    public final Stream<Part<C>> partStream() {
        return parts.values().stream();
    }

    public final Set<Map.Entry<K, Part<C>>> partEntrySet() {
        return parts.entrySet();
    }

    public int size() {
        return parts.size();
    }

    /** TODO construct a table, using TableSaw of the following schema, and pretty print the table instance:
     *      K key
     *      state
     *      Part value
     *      Class valueClass
     * */
    public void print(PrintStream out) {
        parts.forEach((k, s) -> out.println(s.state() + "\t" + k + "\t" + s + "\t" + s.getClass() ));
    }

    private void error(@Nullable Part part, Throwable e, String what) {
        if (part != null)
            logger.error("{} {} {} {}", part, what, this, e);
        else
            logger.error("{} {} {}", what, this, e);
    }

    private boolean _stop(Part<C> part, @Nullable Runnable afterOff) {

        if (!part.state.compareAndSet(ServiceState.On, ServiceState.OnToOff))
            return false;

        executor.execute(() -> {
            try {

                boolean toggledOff = part.state.compareAndSet(Parts.ServiceState.OnToOff, Parts.ServiceState.Off);
                if (!toggledOff)
                    throw new WTF();

                if (afterOff != null)
                    afterOff.run();

                eventAddRemove.emitAsync(pair(part, false), executor);

            } catch (Throwable e) {
                part.state.set(Parts.ServiceState.Off);
                error(part, e, "stop");
            }
        });
        return true;
    }

    public final Set<K> partKeySet() {
        return parts.keySet();
    }

//    public final void toggle(K key) {
//        Part<C> part = part(key);
//        set(key, part, !part.isOn());
//    }


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

    /** returns true if a state change could be attempted; not whether it was actually successful (since it is invoked async) */
    private boolean set(K key, @Nullable Part<C> x, boolean start) {

        if (x == null && start)
            throw new WTF();

        Part<C> removed = x != null ? parts.put(key, x) : parts.remove(key);

        if (x != removed) {
            if (removed != null) {

                _stop(removed, start ? () -> tryStart(x) : null);

                return true;

            } else {
                return !start || tryStart(x);
            }

        } else {
            if (start && x.isOff()) {
                return tryStart(x);
            } else if (!start && x!=null && x.isOn()) {
                return _stop(x,null);
            } else
                return false;
        }
    }

    private boolean tryStart(@Nullable Part<C> x) {
        if (x.state.compareAndSet(ServiceState.Off, ServiceState.OffToOn)) {
            executor.execute(() -> {
                try {

                    x.start(id);

                    boolean toggledOn = x.state.compareAndSet(ServiceState.OffToOn, ServiceState.On);
                    if (!toggledOn)
                        throw new WTF();

                    eventAddRemove.emitAsync(pair(x, true), executor);

                } catch (Throwable e) {
                    x.state.set(ServiceState.Off);
                    error(x, e, "start");
                }
            });
            return true;
        }
        return false;
    }


    public final <X extends Part<C>> Supplier<X> build(Class<X> klass) {
        return build(null, klass);
    }

    public <X extends Part<C>> Supplier<X> build(@Nullable K key, Class<X> klass) {
        if (!(klass.isInterface() || Modifier.isAbstract(klass.getModifiers()))) {
            //concrete class, attempt constructor injection
            return new PartResolveByConstructorInjection(key, klass);
        } else {
            return new PartResolveByClass(key, klass);
        }
    }

    class PartResolveByClass<X extends Part<C>> implements Supplier<X> {

        private final Class<X> klass;

        private PartResolveByClass(K key, Class<X> klass) {
            this.klass = klass;
        }

        @Override
        public X get() {
            throw new TODO();
        }
    }

    class PartResolveByConstructorInjection<X extends Part<C>> implements Supplier<X> {

        private final Class<X> klass;

        PartResolveByConstructorInjection(K key, Class<X> klass) {
            this.klass = klass;
        }

        @Override
        public X get() {

            Constructor[] constructors = klass.getConstructors();


            Object[] args = null;
            int constructor = -1;

            //TODO try new Part(key, thisContext) constructors

            //TODO try new Part(key) constructors

            //try new Part(thisContext) constructors
            if (args == null) {
                int partsIDSettable = ArrayUtils.indexOf(constructors, c -> c.getParameterTypes().length == 1 && c.getParameterTypes()[0].isAssignableFrom(id.getClass()));
                if (partsIDSettable != -1) {
                    constructor = partsIDSettable;
                    args = new Object[]{id};
                }
            }

            //try no-arg constructors
            if (args == null) {
                int noArgConstructor = ArrayUtils.indexOf(constructors, c->c.getParameterTypes().length == 0);
                if (noArgConstructor!=-1) {
                    constructor = noArgConstructor;
                    args = ArrayUtils.EMPTY_OBJECT_ARRAY;
                }
            }

            if (args!=null) {
                try {
                    Constructor cc = constructors[constructor];
                    if (cc.trySetAccessible())
                        return (X) cc.newInstance(args);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            throw new TODO();
        }
    }
}