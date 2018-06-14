//package jcog.lab;
//
//import com.google.common.base.Joiner;
//import com.google.common.collect.Iterables;
//import com.google.common.primitives.Primitives;
//import jcog.Util;
//import jcog.data.graph.ObjectGraph;
//import jcog.list.FasterList;
//import jcog.math.FloatRange;
//import jcog.math.IntRange;
//import jcog.math.Range;
//import jcog.lab.var.TweakFloat;
//import jcog.util.ObjectFloatToFloatFunction;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
//import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
//import org.eclipse.collections.api.tuple.Pair;
//import org.eclipse.collections.impl.list.mutable.FastList;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.BiConsumer;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
//import static org.eclipse.collections.impl.tuple.Tuples.pair;
//
///**
// * automatically discovers tweakable fields for use in Optimize
// * <p>
// * TODO
// * - MutableFloat, AtomicInteger, etc.. tweaks - but dont descend into these (terminal)
// * - boolean, AtomicBoolean, MutableBoolean etc tweaks
// * - @Range annotations on primitive array values (the annotation will be on the array, which it can find at the previous Path element)
// * - enum tweaks
// * - String tweaks
// * <p>
// * - Map accessor
// * - List/collection accessor
// * <p>
// * - lambda accessors for dynamically specified access
// * <p>
// * - hint string expressions, in place of current autoInc
// * would be:   X.inc = (X.max - X.min)/4
// * where X would be substituted for the key prior to eval of each
// * <p>
// * - discover and report private/final/static fields separately as potentially tweakable
// * <p>
// * - objenome dep inject
// * <p>
// * - probes for runtime optimization
// */
//public class Variables<X> {
//
//    private static final int DEFAULT_DEPTH = 7;
//    private final Supplier<X> subjects;
//    private final Map<String, Float> hints = new HashMap();
//
//    /**
//     * set of all partially or fully ready Tweaks
//     */
//    public final List<Tweak<X, ?>> tweaks = new FasterList();
//
//    public Variables(X subject) {
//        this(() -> subject);
//    }
//
//    public Variables(Supplier<X> subjects) {
//        this.subjects = subjects;
//    }
//
//
//    /**
//     * learns how to modify the possibility space of the parameters of a subject
//     * (generates accessors via reflection)
//     */
//    public Variables<X> discover() {
//        return discover(DiscoveryFilter.all);
//    }
//
//    public Variables<X> discover(DiscoveryFilter filter) {
//        return discover(filter, (root, path, targetType) -> {
//            FastList<Pair<Class, ObjectGraph.Accessor>> p = path.clone();
//            String key = key(p);
//
//
//            tweakers.get(Primitives.wrap(targetType)).learn(root, key, p);
//
//            discover(key, p);
//        });
//    }
//
//
//    @FunctionalInterface
//    interface Discovery<X> {
//        void discovered(X x, FasterList<Pair<Class, ObjectGraph.Accessor>> path, Class type);
//    }
//
//    public static class DiscoveryFilter {
//
//        public static final DiscoveryFilter all = new DiscoveryFilter();
//
//        protected boolean includeField(Field f) {
//            return true;
//        }
//
//        protected boolean includeClass(Class<?> targetType) {
//            return true;
//        }
//
//    }
//
//    /**
//     * auto discovers tweaks by reflecting a sample of the subject
//     */
//    public Variables<X> discover(DiscoveryFilter filter, Discovery<X> each) {
//
//
//        X x = (this.subjects.get());
//
//        ObjectGraph o = new ObjectGraph() {
//
//            @Override
//            protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
//                if (this.nodes.containsKey(target))
//                    return false;
//
//                Class<?> targetType = target.getClass();
//                if (!filter.includeClass(targetType))
//                    return false;
//
//                if (tweakable(targetType)) {
//                    each.discovered((X) root, path.clone(), targetType);
//                }
//
//                return !Primitives.unwrap(target.getClass()).isPrimitive();
//            }
//
//
//            @Override
//            public boolean recurse(Object x) {
//                Class<?> xc = x.getClass();
//                return filter.includeClass(xc) && !tweakable(xc);
//            }
//
//            @Override
//            public boolean includeValue(Object v) {
//                return filter.includeClass(v.getClass());
//            }
//
//            @Override
//            public boolean includeClass(Class<?> c) {
//                return filter.includeClass(c);
//            }
//
//            @Override
//            public boolean includeField(Field f) {
//                int m = f.getModifiers();
//                if (!Modifier.isPublic(m) || !filter.includeField(f))
//                    return false;
//
//                Class<?> t = Primitives.wrap(f.getType());
//                boolean primitive = Primitives.unwrap(f.getType()).isPrimitive();
//                if (tweakable(t)) {
//                    return (!primitive || !Modifier.isFinal(m));
//                } else
//                    return !primitive;
//            }
//        };
//
//        o.add(DEFAULT_DEPTH, x);
//
//        return this;
//    }
//
//
//
//    @FunctionalInterface
//    interface Tweaker<X> {
//        void learn(X sample, String key, FastList<Pair<Class, ObjectGraph.Accessor>> path);
//    }
//
//    final Map<Class, Tweaker<X>> tweakers = Map.of(
//
//            Boolean.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
//                Function<X, Boolean> get = ObjectGraph.getter(p);
//                final BiConsumer<X, Boolean> set = ObjectGraph.setter(p);
//                tweak(k, 0, 1, 0.5f,
//                    (x)->get.apply(x) ? 1f : 0f,
//                    (x, v) -> {
//                        boolean b = (v >= 0.5f);
//                        set.accept(x, b);
//                        return (b) ? 1f : 0f;
//                });
//            },
//
//            AtomicBoolean.class, (sample, k, p) -> {
//                final Function<X, AtomicBoolean> get = ObjectGraph.getter(p);
//                AtomicBoolean fr = get.apply(sample);
//                tweak(k, 0, 1, 0.5f, (x, v) -> {
//                    boolean b = v >= 0.5f;
//                    get.apply(x).set(b);
//                    return b ? 1f : 0f;
//                });
//            },
//
//            Integer.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
//                final Function<X, Integer> get = ObjectGraph.getter(p);
//                final BiConsumer<X, Integer> set = ObjectGraph.setter(p);
//                tweak(k, get, set::accept);
//            },
//            IntRange.class, (sample, k, p) -> {
//                final Function<X, IntRange> get = ObjectGraph.getter(p);
//                IntRange fr = get.apply(sample);
//                tweak(k, fr.min, fr.max, -1, null /* TODO */, (ObjectIntProcedure<X>) (x, v) -> {
//                    get.apply(x).set(v);
//                });
//            },
//
//
//
//            Float.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
//                Function<X, Float> get = ObjectGraph.getter(p);
//                final BiConsumer<X, Float> set = ObjectGraph.setter(p);
//                tweak(k, Float.NaN, Float.NaN, Float.NaN,
//                        get,
//                        (x,v)->{ set.accept(x,v); return v; });
//            },
//
//
//
//            FloatRange.class, (sample, k, p) -> {
//                final Function<X, FloatRange> get = ObjectGraph.getter(p);
//                FloatRange fr = get.apply(sample);
//                tweak(k, fr.min, fr.max, Float.NaN,
//                    (x)-> get.apply(x).floatValue(),
//                    (x, v) -> {
//                        get.apply(x).set(v);
//                        return v;
//                    });
//            }
//
//
//
//
//
//
//
//
//    );
//
//    /**
//     * extract any hints from the path (ex: annotations, etc)
//     */
//    private void discover(String key, FastList<Pair<Class, ObjectGraph.Accessor>> path) {
//        ObjectGraph.Accessor a = path.getLast().getTwo();
//        if (a instanceof ObjectGraph.FieldAccessor) {
//            Field field = ((ObjectGraph.FieldAccessor) a).field;
//            Range r = field.getAnnotation(Range.class);
//            if (r != null) {
//                double min = r.min();
//                if (min == min)
//                    hints.put(key + ".min", (float) min);
//
//                double max = r.max();
//                if (max == max)
//                    hints.put(key + ".max", (float) max);
//
//                double inc = r.step();
//                if (inc == inc)
//                    hints.put(key + ".inc", (float) inc);
//            }
//        }
//
//    }
//
//    private static String key(List<Pair<Class, ObjectGraph.Accessor>> path) {
//        return Joiner.on(':').join(Iterables.transform(path, e ->
//                e.getOne().getName() + '.' + e.getTwo()));
//    }
//
//
//
//    public Variables<X> tweak(String key, Function<X, Integer> get, ObjectIntProcedure<X> apply) {
//        return tweak(key, Float.NaN, Float.NaN, Float.NaN, (x) -> get.apply(x).floatValue() /* HACK */, (X x, float v) -> {
//            int i = Math.round(v);
//            apply.accept(x, i);
//            return i;
//        });
//    }
//
//    public Variables<X> tweak(String key, int min, int max, int inc, Function<X, Integer> get, ObjectIntProcedure<X> apply) {
//        return tweak(key, min, max, inc < 0 ? Float.NaN : inc,
//            (x) -> get!=null ? get.apply(x).floatValue() : null /* HACK */,
//            (X x, float v) -> {
//            int i = Math.round(v);
//            apply.accept(x, i);
//            return i;
//        });
//    }
//
//    public Variables<X> tweak(String id, float min, float max, float inc, ObjectFloatProcedure<X> apply) {
//        tweaks.add(new TweakFloat<>(id, min, max, inc, (x)->null, (X x, float v) -> {
//            apply.value(x, v);
//            return v;
//        }));
//        return this;
//    }
//
//
//    @Deprecated
//    public Variables<X> tweak(String id, float min, float max, float inc, ObjectFloatToFloatFunction<X> set) {
//        return tweak(id, min, max, inc, null, set);
//    }
//
//    public Variables<X> tweak(String id, float min, float max, float inc, Function<X, Float> get, ObjectFloatToFloatFunction<X> set) {
//        tweaks.add(new TweakFloat<>(id, min, max, inc, get, set));
//        return this;
//    }
//
//
//    public Pair<List<Tweak<X, ?>>, SortedSet<String>> get(Map<String, Float> additionalHints) {
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
//        final List<Tweak<X, ?>> ready = new FasterList();
//
//        TreeSet<String> unknowns = new TreeSet<>();
//        for (Tweak<X, ?> t : tweaks) {
//            List<String> u = t.unknown(h);
//            if (u.isEmpty()) {
//                ready.add(t);
//            } else {
//                unknowns.addAll(u);
//            }
//        }
//
//        return pair(ready, unknowns);
//    }
//
//    private boolean tweakable(Class<?> t) {
//        return tweakers.containsKey(Primitives.wrap(t));
//    }
//
//
//    /** simple scalar eval */
//    public Lab<X,X> optimize(FloatFunction<X> score) {
//        return optimize((x->x), new Sensor.NumericLambdaSensor<>(score));
//    }
//
//    /** multi-scalar - automatically named scores (score0, score1... ) */
//    public <Y> Lab<X,Y> optimize(Function<X,Y> experiment, FloatFunction<Y>... scores) {
//        final int[] j = {0};
//        return optimize(experiment, Util.map(
//                    s-> new Sensor.NumericLambdaSensor<>("score" + (j[0]++), s),
//                    new Sensor.NumericLambdaSensor[scores.length],
//                    scores));
//    }
//
//    public <Y> Lab<X,Y> optimize(Function<X,Y> experiment, Sensor<Y,?>... seeks) {
//        return new Lab<>(subjects, experiment, seeks);
//    }
//
//}
