package jcog.lab;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Primitives;
import jcog.data.graph.ObjectGraph;
import jcog.io.arff.ARFF;
import jcog.lab.util.ExperimentRun;
import jcog.lab.util.Opti;
import jcog.lab.util.Optimization;
import jcog.lab.var.FloatVar;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.util.ObjectFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

import static java.util.stream.Collectors.toList;

/**
 * the Lab is essentially an ExperimentBuilder.
 * it is a mutable representation of the state necessary to
 * "compile" individual experiment instances, and such
 * is the entry point to performing them.
 *
 * @param X subject type
 * @param E experiment type. may be the same as X in some cases but other times
 *          there is a reason to separate the subject from the experiment
 */
public class Lab<X> {

    private final static Logger logger = LoggerFactory.getLogger(Lab.class);
    private static final int DEFAULT_DEPTH = 7;
    final Supplier<X> subject;
    final Map<String, Sensor<X, ?>> sensors = new ConcurrentHashMap<>();
    public final Map<String, Var<X, ?>> vars = new ConcurrentHashMap<>();
    final Map<String, Object> hints = new HashMap();

    public Lab() {
        this(null);
    }

    public Lab(Supplier<X> subject) {
        initDefaults();

        this.subject = subject;


    }

    private static String varReflectedKey(Iterable<Pair<Class, ObjectGraph.Accessor>> path) {
        return Joiner.on(':').join(Iterables.transform(path, e ->
                e.getOne().getName() + '.' + e.getTwo()));
    }

    void initDefaults() {
        final float autoInc_default = 5f;
        hints.put("autoInc", autoInc_default);
    }

    /**
     * records all sensors ()
     */
    public static <X> Object[] record(X x, ARFF data, List<Sensor<X, ?>> sensors) {
        synchronized (data) {
            Object[] row = row(x, sensors);
            data.add(row);
            return row;
        }
    }

    public static <X> Object[] rowVars(X x, List<Var<X, ?>> vars) {
        List<Sensor<X, ?>> sensors = new FasterList(vars.size());
        for (int i = 0; i < vars.size(); i++)
            sensors.add(vars.get(i).sense());
        return row(x, sensors);
    }

    public static <X> Object[] row(X x, List<Sensor<X, ?>> sensors) {
        Object row[] = new Object[sensors.size()];
        int c = 0;
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            row[c++] = sensors.get(i).apply(x);
        }
        return row;
    }

    protected void add(Var<X, ?> v) {
        if (vars.put(v.id, v) != null)
            throw new UnsupportedOperationException(v.id + " already present in variables");
    }


    private TreeSet<String> validate() {

//        Map<String, Float> h;
//        if (!this.hints.isEmpty()) {
//            if (additionalHints.isEmpty()) {
//                h = this.hints;
//            } else {
//
//                h = new HashMap();
//                h.putAll(this.hints);
//                h.putAll(additionalHints);
//            }
//        } else {
//            h = additionalHints;
//        }
//
//
        TreeSet<String> unknowns = new TreeSet<>();
        for (Var<X, ?> t: vars.values()) {
            List<String> u = t.unknown(hints);
            if (!u.isEmpty())
                unknowns.addAll(u);
        }
        return unknowns;
    }

    public ExperimentRun<X> run(BiConsumer<X, ExperimentRun<X>> proc) {
        return new ExperimentRun<>(subject.get(), sensors.values(), proc);
    }

    /**
     * score is an objective function that the optimization process tries to
     * maximize.
     */
    public static <X, Y> Optimization<X, Y> optimize(Supplier<X> subj, List<Var<X, ?>> vars, Optimization.OptimizationStrategy strategy, Function<Supplier<X>, Y> procedure, Goal<Y> goal, List<Sensor<Y, ?>> sensors) {

        if (vars.isEmpty())
            throw new UnsupportedOperationException("no Var's provided");

        return new Optimization<>(subj, procedure, goal, vars, sensors, strategy);
    }

    /**
     * defaults:
     * optimizing using all ready variables
     * the default optimization strategy and its parameters
     *
     * @param score
     * @param procedure
     * @param goal
     */
    public Optimization<X, X> optimize(Consumer<X> procedure, Goal<X> goal) {
        return optimize(subject, vars.values().stream().filter(Var::ready).collect(toList()),
                newDefaultOptimizer(), (s -> {
                    X ss = s.get();
                    procedure.accept(ss);
                    return ss;
                }), goal, new FasterList<>(sensors.values()));
    }

    public <Y> Optimization<X, Y> optimize(Function<Supplier<X>, Y> procedure, Goal<Y> goal, List<Sensor<Y, ?>> sensors) {
        return optimize(subject, vars.values().stream().filter(Var::ready).collect(toList()),
                newDefaultOptimizer(), procedure, goal, sensors
        );
    }

    public <Y> Optimization<X, Y> optimize(Function<Supplier<X>, Y> procedure, Goal<Y> goal, Lab<Y> sensors) {
        return optimize(subject, vars.values().stream().filter(Var::ready).collect(toList()),
                newDefaultOptimizer(), procedure, goal, new FasterList(sensors.sensors.values())
        );
    }


    /**
     * simple usage method
     * provies procedure and goal; no additional experiment sensors
     */
    public Opti<X> optimize(Consumer<X> procedure, FloatFunction<X> goal) {
        return new Opti<>(optimize(procedure,
                new Goal<>(goal)));
    }


    /**
     * simple usage method
     * provies procedure and goal; no additional experiment sensors
     */
    public <E> Optimization<X, E> optimize(Function<Supplier<X>, E> procedure, FloatFunction<E> goal) {
        return optimize(procedure, new Goal<>(goal), List.of());
    }
    public <E> Optimization<X, E> optimize(Function<Supplier<X>, E> procedure, ToDoubleFunction<E> goal) {
        return optimize(procedure, new Goal<>(goal), List.of());
    }

    /**
     * simple usage method
     * provies procedure and goal; no additional experiment sensors
     */
    public Opti<X> optimize(Goal<X> goal) {
        return new Opti<>(optimize(e -> {  }, goal));
    }

    public Opti<X> optimize(FloatFunction<X> goal) {
        return optimize(new Goal<>(goal));
    }

    private Optimization.OptimizationStrategy newDefaultOptimizer()
    {
        return newDefaultOptimizer(512);
    }

    private Optimization.OptimizationStrategy newDefaultOptimizer(int maxIter) {
        return
                vars.size() == 1 ?
                        new Optimization.SimplexOptimizationStrategy(maxIter) :
                        new Optimization.CMAESOptimizationStrategy(maxIter);
    }


    public Lab<X> sense(Sensor sensor) {
        Sensor removed = sensors.put(sensor.id, sensor);
        if (removed != null)
            throw new RuntimeException("sensor name collision");
        return this;
    }

    private Lab<X> senseNumber(String id, Function<X, Number> f) {
        return sense(NumberSensor.ofNumber(id, f));
    }

    public Lab<X> sense(String id, FloatFunction<X> f) {
        return sense(NumberSensor.of(id, f));
    }
    public Lab<X> sense(String id, BooleanFunction<X> f) {
        return sense(NumberSensor.of(id, f));
    }
    public Lab<X> sense(String id, ToDoubleFunction<X> f) {
        return sense(NumberSensor.of(id, f));
    }

    public Lab<X> sense(String id, ToLongFunction<X> f) {
        return sense(NumberSensor.of(id, f));
    }

    public Lab<X> sense(String id, ToIntFunction<X> f) {
        return sense(NumberSensor.of(id, f));
    }

    public Lab<X> sense(String id, Predicate<X> f) {
        return sense(NumberSensor.of(id, f));
    }

    /**
     * learns how to modify the possibility space of the parameters of a subject
     * (generates accessors via reflection)
     */
    public Lab<X> varAuto() {
        varAuto(DiscoveryFilter.all);

        SortedSet<String> unknown = validate();

        if (!unknown.isEmpty()) {
            for (String w: unknown) {
                logger.warn("unknown: {}", w);
            }
        }

//        if (this.vars.isEmpty()) {
//            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(unknown));
//        }


        return this;
    }

    public Lab<X> varAuto(DiscoveryFilter filter) {
        return varAuto(filter, (root, path, targetType) -> {
            FastList<Pair<Class, ObjectGraph.Accessor>> p = path.clone();
            String key = varReflectedKey(p);


            varByClass.get(Primitives.wrap(targetType)).learn(root, key, p);

            varAuto(key, p);
        });
    }

    /**
     * auto discovers Vars by reflecting a sample of the subject
     */
    public Lab<X> varAuto(DiscoveryFilter filter, Discovery<X> each) {


        X x = (this.subject.get());

        ObjectGraph o = new ObjectGraph() {

            @Override
            protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
                if (this.nodes.containsKey(target))
                    return false;

                Class<?> targetType = target.getClass();
                if (!filter.includeClass(targetType))
                    return false;

                if (contains(targetType)) {
                    each.discovered((X) root, path.clone(), targetType);
                }

                return !Primitives.unwrap(target.getClass()).isPrimitive();
            }


            @Override
            public boolean recurse(Object x) {
                Class<?> xc = x.getClass();
                return filter.includeClass(xc) && !contains(xc);
            }

            @Override
            public boolean includeValue(Object v) {
                return filter.includeClass(v.getClass());
            }

            @Override
            public boolean includeClass(Class<?> c) {
                return filter.includeClass(c);
            }

            @Override
            public boolean includeField(Field f) {
                int m = f.getModifiers();
                if (!Modifier.isPublic(m) || !filter.includeField(f))
                    return false;

                Class<?> t = Primitives.wrap(f.getType());
                boolean primitive = Primitives.unwrap(f.getType()).isPrimitive();
                if (contains(t)) {
                    return (!primitive || !Modifier.isFinal(m));
                } else
                    return !primitive;
            }
        };

        o.add(DEFAULT_DEPTH, x);

        return this;
    }

    /**
     * extract any hints from the path (ex: annotations, etc)
     */
    private void varAuto(String key, FastList<Pair<Class, ObjectGraph.Accessor>> path) {
        ObjectGraph.Accessor a = path.getLast().getTwo();
        if (a instanceof ObjectGraph.FieldAccessor) {
            Field field = ((ObjectGraph.FieldAccessor) a).field;
            Range r = field.getAnnotation(Range.class);
            if (r != null) {
                double min = r.min();
                if (min == min)
                    hints.put(key + ".min", (float) min);

                double max = r.max();
                if (max == max)
                    hints.put(key + ".max", (float) max);

                double inc = r.step();
                if (inc == inc)
                    hints.put(key + ".inc", (float) inc);
            }
        }

    }

    public Lab<X> var(String key, Function<X, Integer> get, ObjectIntProcedure<X> apply) {
        return var(key, Float.NaN, Float.NaN, Float.NaN, (x) -> get.apply(x).floatValue() /* HACK */, (X x, float v) -> {
            int i = Math.round(v);
            apply.accept(x, i);
            return i;
        });
    }

    public Lab<X> var(String key, int min, int max, int inc, Function<X, Integer> get, ObjectIntProcedure<X> apply) {
        return var(key, min, max, inc < 0 ? Float.NaN : inc,
            (x) -> get!=null ? get.apply(x).floatValue() : null /* HACK */,
            (X x, float v) -> {
                int i = Math.round(v);
                apply.accept(x, i);
                return i;
        });
    }

    public Lab<X> var(String id, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
        vars.put(id, new FloatVar<>(id, min, max, inc, null, (X x, float v) -> {
            apply.value(x, v);
            return v;
        }));
        return this;
    }

    /** TODO use an IntVar impl */
    public Lab<X> var(String id, int min, int max, int inc, ObjectIntProcedure<X> apply) {
        vars.put(id, new FloatVar<>(id, min, max, inc, null, (X x, float v) -> {
            int vv = Math.round(v);
            apply.value(x, vv);
            return vv;
        }));
        return this;
    }

    @Deprecated
    public Lab<X> var(String id, float min, float max, float inc, ObjectFloatToFloatFunction<X> set) {
        return var(id, min, max, inc, null, set);
    }

    public Lab<X> var(String id, float min, float max, float inc, Function<X, Float> get, ObjectFloatToFloatFunction<X> set) {
        vars.put(id, new FloatVar<>(id, min, max, inc, get, set));
        return this;
    }

    private boolean contains(Class<?> t) {
        return varByClass.containsKey(Primitives.wrap(t));
    }


    @FunctionalInterface
    private interface Discovery<X2> {
        void discovered(X2 x, FasterList<Pair<Class, ObjectGraph.Accessor>> path, Class type);
    }

    @FunctionalInterface
    private interface VarAccess<X2> {
        void learn(X2 sample, String key, FastList<Pair<Class, ObjectGraph.Accessor>> path);
    }

    public static class DiscoveryFilter {

        public static final DiscoveryFilter all = new DiscoveryFilter();

        protected boolean includeField(Field f) {
            return true;
        }

        protected boolean includeClass(Class<?> targetType) {
            return true;
        }

    }

    private final Map<Class, VarAccess<X>> varByClass = Map.of(

            Boolean.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
                Function<X, Boolean> get = ObjectGraph.getter(p);
                final BiConsumer<X, Boolean> set = ObjectGraph.setter(p);
                var(k, 0, 1, 0.5f,
                        (x)->get.apply(x) ? 1f : 0f,
                        (x, v) -> {
                            boolean b = (v >= 0.5f);
                            set.accept(x, b);
                            return (b) ? 1f : 0f;
                        });
            },

            AtomicBoolean.class, (sample, k, p) -> {
                final Function<X, AtomicBoolean> get = ObjectGraph.getter(p);
                AtomicBoolean fr = get.apply(sample);
                var(k, 0, 1, 0.5f, (x, v) -> {
                    boolean b = v >= 0.5f;
                    get.apply(x).set(b);
                    return b ? 1f : 0f;
                });
            },

            Integer.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
                final Function<X, Integer> get = ObjectGraph.getter(p);
                final BiConsumer<X, Integer> set = ObjectGraph.setter(p);
                var(k, get, set::accept);
            },
            IntRange.class, (sample, k, p) -> {
                final Function<X, IntRange> get = ObjectGraph.getter(p);
                IntRange fr = get.apply(sample);
                var(k, fr.min, fr.max, -1, null /* TODO */, (ObjectIntProcedure<X>) (x, v) -> {
                    get.apply(x).set(v);
                });
            },



            Float.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
                Function<X, Float> get = ObjectGraph.getter(p);
                final BiConsumer<X, Float> set = ObjectGraph.setter(p);
                var(k, Float.NaN, Float.NaN, Float.NaN,
                        get,
                        (x,v)->{ set.accept(x,v); return v; });
            },



            FloatRange.class, (sample, k, p) -> {
                final Function<X, FloatRange> get = ObjectGraph.getter(p);
                FloatRange fr = get.apply(sample);
                var(k, fr.min, fr.max, Float.NaN,
                        (x)-> get.apply(x).floatValue(),
                        (x, v) -> {
                            get.apply(x).set(v);
                            return v;
                        });
            }

    );


}