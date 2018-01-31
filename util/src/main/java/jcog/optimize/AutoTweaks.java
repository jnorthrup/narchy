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
                if (this.nodes.containsKey(target))
                    return false; //prevent cycle

                Class<?> targetType = target.getClass();
                if (tweakable(targetType)) {
                    tweak((X)root, path.clone(), targetType);
                }

                if (Primitives.unwrap(target.getClass()).isPrimitive())
                    return false; //dont add the primitive value itself which would get caught later in the cycle detector above

                return true;
            }


            @Override
            public boolean recurse(Object x) {
                return !tweakable(x.getClass());
            }

            @Override
            public boolean includeValue(Object v) {
                return true;
            }

            @Override
            public boolean includeClass(Class<?> c) {
                //return !excludedClasses.contains(c);
                return true;
            }

            @Override
            public boolean includeField(Field f) {
                int m = f.getModifiers();
                if (!Modifier.isPublic(m) || !AutoTweaks.this.includeField(f))
                    return false;

                Class<?> t = Primitives.wrap(f.getType());
                boolean primitive = Primitives.unwrap(f.getType()).isPrimitive();
                if (tweakable(t)) {
                    return (!primitive || !Modifier.isFinal(m));
                } else
                    return !primitive; //explore further into Object's, final or not
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
                tweak(k, Float.NaN, Float.NaN, Float.NaN, set::accept);
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
        return Joiner.on(':').join(Iterables.transform(path, e ->
                e.getOne().getName() + "." + e.getTwo()));
    }

    protected boolean includeField(Field f) {
        return true;
    }

    @Override
    public Pair<List<Tweak<X>>, SortedSet<String>> get(Map<String, Float> additionalHints) {
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
        return super.get(h);
    }

    protected void tweak(X sample, FastList<Pair<Class, ObjectGraph.Accessor>> path, Class targetType) {
        String key = key(path);

        //TODO find matching Super-types
        tweakers.get(Primitives.wrap(targetType)).learn(sample, key, path);

        learn(key, path);
    }




    public boolean tweakable(Class<?> t) {
        return tweakers.containsKey(Primitives.wrap(t));
    }


    public Optimize<X> optimize() {
        return optimize(subjects);
    }

    public Result<X> optimize(int maxIterations, FloatFunction<Supplier<X>> eval) {
        return optimize().run(maxIterations, eval);
    }

}
