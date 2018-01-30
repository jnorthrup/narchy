package jcog.optimize;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Primitives;
import jcog.data.graph.ObjectGraph;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.Range;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** automatically discovers tweakable fields for use in Optimize
 *
 * TODO
 *      - MutableFloat, AtomicInteger, etc.. tweaks - but dont descend into these (terminal)
 *      - boolean, AtomicBoolean, MutableBoolean etc tweaks
 *      - enum tweaks
 *      - String tweaks
 *
 *      - hint string expressions, in place of current autoInc
 *              would be:   X.inc = (X.max - X.min)/4
 *              where X would be substituted for the key prior to eval of each
 *
 *      - discover and report private/final/static fields separately as potentially tweakable
 *
 *      - objenome dep inject
 * */
public class AutoTweaks<X> extends Tweaks<X> {

    private static final int DEFAULT_DEPTH = 4;
    private final Supplier<X> subjects;
    private Map<String, Float> hints = new HashMap();

    public AutoTweaks(Supplier<X> subject) {
        super();
        this.subjects = subject;
        discover(subject.get());
    }


    private void discover(X x /* sample instance */) {

        ObjectGraph o = new ObjectGraph() {

            @Override
            protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
                Class<?> targetType = target.getClass();
                if (tweakable(targetType)) {
                    tweak((X)root, path.clone(), targetType);
                }
                return true;
            }

            @Override
            public boolean recurse(Object x) {
                return !valueHolder(x.getClass());
            }

            @Override
            public boolean includeValue(Object v) {
                return true;
            }

            @Override
            public boolean includeClass(Class<?> c) {
                return true;
            }

            @Override
            public boolean includeField(Field f) {
                int m = f.getModifiers();
                if (!Modifier.isPublic(m))
                    return false;

                Class<?> t = Primitives.unwrap(f.getType());
                if (tweakable(t)) {
                    return (!t.isPrimitive() || !Modifier.isFinal(m));
                } else
                    return AutoTweaks.this.includeField(f); //explore further into Object's, final or not
            }
        };

        o.add(DEFAULT_DEPTH, x);

    }

    @FunctionalInterface interface Tweaker<X> {
        void learn(X sample, String key, FastList<Pair<Class, ObjectGraph.Accessor>> path);
    }

    final Map<Class,Tweaker<X>> tweakers = Map.of(
//            Boolean.class, null,

            Integer.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p)->{
                final BiConsumer<X, Integer> set = ObjectGraph.setter(p);
                tweak(k, set::accept);
            },

            Float.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p)->{
                final BiConsumer<X, Float> set = ObjectGraph.setter(p);
                tweak(k, set::accept);
            },

//            AtomicBoolean.class, null,
//            AtomicInteger.class, null,
//            AtomicDouble.class, null,
//            AtomicLong.class, null,
//
//            MutableFloat.class, null,
//            MutableInteger.class, null,
//            MutableDouble.class, null,
//            MutableLong.class, null,
//
            FloatRange.class, (sample, k, p)->{
                final Function<X, FloatRange> get = ObjectGraph.getter(p);

                //use the min/max at the time this is constructed, which assumes they will remain the same
                FloatRange fr = get.apply(sample);

                tweak(k, fr.min, fr.max, Float.NaN, (x,v)->{
                    fr.set(v);
                });
            }

//            FloatRangeRounded.class, null
    );

    /** extract any hints from the path (ex: annotations, etc) */
    private void learn(String key, FastList<Pair<Class, ObjectGraph.Accessor>> path) {
        ObjectGraph.Accessor a = path.getLast().getTwo();
        if (a instanceof ObjectGraph.FieldAccessor) {
            Field field = ((ObjectGraph.FieldAccessor)a).field;
            Range r = field.getAnnotation(Range.class);
            if (r!=null) {
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

    private String key(List<Pair<Class, ObjectGraph.Accessor>> path) {
        return Joiner.on(':').join(Iterables.transform(path, (e)->{
            return e.getOne().getName() + "." + e.getTwo();
        }));
    }

    /** whethre the class is a terminal value holder itself, handled by special Tweak implementations */
    protected boolean valueHolder(Class c) {
        return tweakers.containsKey(c);
    }

    protected boolean includeField(Field f) {
        return true;
    }

    @Override
    public SortedSet<String> unknown(Map<String, Float> additionalHints) {
        Map<String, Float> h;
        if (!this.hints.isEmpty()) {
            if (additionalHints.isEmpty()) {
                h = this.hints;
            } else {
                //combine
                h = new HashMap();
                h.putAll(this.hints);
                h.putAll(additionalHints); //allow supplied hints to override inferred
            }
        } else {
            h = additionalHints;
        }
        return super.unknown(h);
    }

    protected void tweak(X sample, FastList<Pair<Class, ObjectGraph.Accessor>> path, Class targetType) {
        String key = key(path);

        //TODO find matching Super-types
        tweakers.get(targetType).learn(sample, key, path);

        learn(key, path);
    }




    public boolean tweakable(Class<?> t) {
        return valueHolder(t);
    }


    public Optimize<X> optimize() {
        return optimize(subjects);
    }

    public Result<X> optimize(int maxIterations, FloatFunction<X> eval) {
        return optimize().run(maxIterations, eval);
    }

}
