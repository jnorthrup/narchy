package jcog.lab;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Primitives;
import jcog.data.graph.ObjectGraph;
import jcog.lab.var.FloatVar;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.util.ObjectFloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * automatically discovers Varable fields for use in Optimize
 * <p>
 * TODO
 * - MutableFloat, AtomicInteger, etc.. Vars - but dont descend into these (terminal)
 * - boolean, AtomicBoolean, MutableBoolean etc Vars
 * - @Range annotations on primitive array values (the annotation will be on the array, which it can find at the previous Path element)
 * - enum Vars
 * - String Vars
 * <p>
 * - Map accessor
 * - List/collection accessor
 * <p>
 * - lambda accessors for dynamically specified access
 * <p>
 * - hint string expressions, in place of current autoInc
 * would be:   X.inc = (X.max - X.min)/4
 * where X would be substituted for the key prior to eval of each
 * <p>
 * - discover and report private/final/static fields separately as potentially Varable
 * <p>
 * - objenome dep inject
 * <p>
 * - probes for runtime optimization
 */
public class Variables<E> {

    private final Supplier<E> subject;
    private final Map<String, Var<E, ?>> vars;

    private static final int DEFAULT_DEPTH = 7;

    public final Map<String, Float> hints = new HashMap();

    public Variables(Supplier<E> subject) {
        this.subject = subject;
        this.vars = new HashMap<>();
    }

    /**
     * set of all partially or fully ready Vars
     */
    public Variables(Supplier<E> subject, Map<String, Var<E, ?>> vars) {
        this.subject = subject;
        this.vars = vars;
    }


    /**
     * learns how to modify the possibility space of the parameters of a subject
     * (generates accessors via reflection)
     */
    public Variables<E> discover() {
        return discover(DiscoveryFilter.all);
    }

    public Variables<E> discover(DiscoveryFilter filter) {
        return discover(filter, (root, path, targetType) -> {
            FastList<Pair<Class, ObjectGraph.Accessor>> p = path.clone();
            String key = key(p);


            byClass.get(Primitives.wrap(targetType)).learn(root, key, p);

            discover(key, p);
        });
    }


    @FunctionalInterface
    private interface Discovery<X> {
        void discovered(X x, FasterList<Pair<Class, ObjectGraph.Accessor>> path, Class type);
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

    /**
     * auto discovers Vars by reflecting a sample of the subject
     */
    public Variables<E> discover(DiscoveryFilter filter, Discovery<E> each) {


        E x = (this.subject.get());

        ObjectGraph o = new ObjectGraph() {

            @Override
            protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
                if (this.nodes.containsKey(target))
                    return false;

                Class<?> targetType = target.getClass();
                if (!filter.includeClass(targetType))
                    return false;

                if (contains(targetType)) {
                    each.discovered((E) root, path.clone(), targetType);
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



    @FunctionalInterface
    private interface VarAccess<X> {
        void learn(X sample, String key, FastList<Pair<Class, ObjectGraph.Accessor>> path);
    }


    /**
     * extract any hints from the path (ex: annotations, etc)
     */
    private void discover(String key, FastList<Pair<Class, ObjectGraph.Accessor>> path) {
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

    private static String key(Iterable<Pair<Class, ObjectGraph.Accessor>> path) {
        return Joiner.on(':').join(Iterables.transform(path, e ->
                e.getOne().getName() + '.' + e.getTwo()));
    }



    public Variables<E> add(String key, Function<E, Integer> get, ObjectIntProcedure<E> apply) {
        return add(key, Float.NaN, Float.NaN, Float.NaN, (x) -> get.apply(x).floatValue() /* HACK */, (E x, float v) -> {
            int i = Math.round(v);
            apply.accept(x, i);
            return i;
        });
    }

    public Variables<E> add(String key, int min, int max, int inc, Function<E, Integer> get, ObjectIntProcedure<E> apply) {
        return add(key, min, max, inc < 0 ? Float.NaN : inc,
            (x) -> get!=null ? get.apply(x).floatValue() : null /* HACK */,
            (E x, float v) -> {
                int i = Math.round(v);
                apply.accept(x, i);
                return i;
        });
    }

    public Variables<E> add(String id, float min, float max, float inc, ObjectFloatProcedure<E> apply) {
        vars.put(id, new FloatVar<>(id, min, max, inc, null, (E x, float v) -> {
            apply.value(x, v);
            return v;
        }));
        return this;
    }

    /** TODO use an IntVar impl */
    public Variables<E> add(String id, int min, int max, int inc, ObjectIntProcedure<E> apply) {
        vars.put(id, new FloatVar<>(id, min, max, inc, null, (E x, float v) -> {
            int vv = Math.round(v);
            apply.value(x, vv);
            return vv;
        }));
        return this;
    }

    @Deprecated
    public Variables<E> add(String id, float min, float max, float inc, ObjectFloatToFloatFunction<E> set) {
        return add(id, min, max, inc, null, set);
    }

    public Variables<E> add(String id, float min, float max, float inc, Function<E, Float> get, ObjectFloatToFloatFunction<E> set) {
        vars.put(id, new FloatVar<>(id, min, max, inc, get, set));
        return this;
    }



    private boolean contains(Class<?> t) {
        return byClass.containsKey(Primitives.wrap(t));
    }

    final Map<Class, VarAccess<E>> byClass = Map.of(

            Boolean.class, (E sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
                Function<E, Boolean> get = ObjectGraph.getter(p);
                final BiConsumer<E, Boolean> set = ObjectGraph.setter(p);
                add(k, 0, 1, 0.5f,
                        (x)->get.apply(x) ? 1f : 0f,
                        (x, v) -> {
                            boolean b = (v >= 0.5f);
                            set.accept(x, b);
                            return (b) ? 1f : 0f;
                        });
            },

            AtomicBoolean.class, (sample, k, p) -> {
                final Function<E, AtomicBoolean> get = ObjectGraph.getter(p);
                AtomicBoolean fr = get.apply(sample);
                add(k, 0, 1, 0.5f, (x, v) -> {
                    boolean b = v >= 0.5f;
                    get.apply(x).set(b);
                    return b ? 1f : 0f;
                });
            },

            Integer.class, (E sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
                final Function<E, Integer> get = ObjectGraph.getter(p);
                final BiConsumer<E, Integer> set = ObjectGraph.setter(p);
                add(k, get, set::accept);
            },
            IntRange.class, (sample, k, p) -> {
                final Function<E, IntRange> get = ObjectGraph.getter(p);
                IntRange fr = get.apply(sample);
                add(k, fr.min, fr.max, -1, null /* TODO */, (ObjectIntProcedure<E>) (x, v) -> {
                    get.apply(x).set(v);
                });
            },



            Float.class, (E sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p) -> {
                Function<E, Float> get = ObjectGraph.getter(p);
                final BiConsumer<E, Float> set = ObjectGraph.setter(p);
                add(k, Float.NaN, Float.NaN, Float.NaN,
                        get,
                        (x,v)->{ set.accept(x,v); return v; });
            },



            FloatRange.class, (sample, k, p) -> {
                final Function<E, FloatRange> get = ObjectGraph.getter(p);
                FloatRange fr = get.apply(sample);
                add(k, fr.min, fr.max, Float.NaN,
                        (x)-> get.apply(x).floatValue(),
                        (x, v) -> {
                            get.apply(x).set(v);
                            return v;
                        });
            }

    );


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

}
