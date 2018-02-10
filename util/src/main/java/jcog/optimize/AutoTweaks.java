package jcog.optimize;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Primitives;
import jcog.data.graph.ObjectGraph;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.math.Range;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** automatically discovers tweakable fields for use in Optimize
 *
 * TODO
 *      - MutableFloat, AtomicInteger, etc.. tweaks - but dont descend into these (terminal)
 *      - boolean, AtomicBoolean, MutableBoolean etc tweaks
 *      - @Range annotations on primitive array values (the annotation will be on the array, which it can find at the previous Path element)
 *      - enum tweaks
 *      - String tweaks
 *
 *      - Map accessor
 *      - List/collection accessor
 *
 *      - lambda accessors for dynamically specified access
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

    private static final int DEFAULT_DEPTH = 7;
    private final Supplier<X> subjects;
    private final Map<String, Float> hints = new HashMap();

    final Set<Class> excludedClasses = new HashSet();

    public AutoTweaks(Supplier<X> subject) {
        super();
        this.subjects = subject;
    }

    public AutoTweaks<X> exclude(Class c) {
        excludedClasses.add(c);
        return this;
    }

    private void discover(X x /* sample instance */) {

        ObjectGraph o = new ObjectGraph() {

            @Override
            protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
                if (this.nodes.containsKey(target))
                    return false; //prevent cycle

                Class<?> targetType = target.getClass();
                if (excludedClasses.contains(targetType))
                    return false;

                if (tweakable(targetType)) {
                    tweak((X)root, path.clone(), targetType);
                }

                return !Primitives.unwrap(target.getClass()).isPrimitive();
            }


            @Override
            public boolean recurse(Object x) {
                Class<?> xc = x.getClass();
                return !excludedClasses.contains(xc) && !tweakable(xc);
            }

            @Override
            public boolean includeValue(Object v) {
                return !excludedClasses.contains(v.getClass());
            }

            @Override
            public boolean includeClass(Class<?> c) {
                return !excludedClasses.contains(c);
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

            Boolean.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p)->{
                final BiConsumer<X, Boolean> set = ObjectGraph.setter(p);
                tweak(k, 0, 1, 0.5f, (x, v)->{
                    boolean b = v >= 0.5f;
                    set.accept(x, b);
                    return (b) ? 1f : 0f;
                });
            },

            AtomicBoolean.class, (sample, k, p)->{
                final Function<X, AtomicBoolean> get = ObjectGraph.getter(p);
                AtomicBoolean fr = get.apply(sample); //use the min/max at the time this is constructed, which assumes they will remain the same
                tweak(k, 0, 1, 0.5f, (x, v)->{
                    boolean b = v >= 0.5f;
                    fr.set(b);
                    return b ? 1f : 0f;
                });
            },

            Integer.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p)->{
                final BiConsumer<X, Integer> set = ObjectGraph.setter(p);
                tweak(k, set::accept);
            },
            IntRange.class,  (sample, k, p)->{
                final Function<X, IntRange> get = ObjectGraph.getter(p);
                IntRange fr = get.apply(sample); //use the min/max at the time this is constructed, which assumes they will remain the same
                tweak(k, fr.min, fr.max, -1, (ObjectIntProcedure<X>)(x, v)->{ fr.set(v); });
            },
//            AtomicInteger.class, null,
//            MutableInteger.class, null,

            Float.class, (X sample, String k, FastList<Pair<Class, ObjectGraph.Accessor>> p)->{
                final BiConsumer<X, Float> set = ObjectGraph.setter(p);
                tweak(k, Float.NaN, Float.NaN, Float.NaN, set::accept);
            },
//            MutableFloat.class, null,


            FloatRange.class, (sample, k, p)->{
                final Function<X, FloatRange> get = ObjectGraph.getter(p);
                FloatRange fr = get.apply(sample); //use the min/max at the time this is constructed, which assumes they will remain the same
                tweak(k, fr.min, fr.max, Float.NaN, (x,v)->{ fr.set(v); });
            }

//            FloatRangeRounded.class, null
//            AtomicDouble.class, null,
//            AtomicLong.class, null,
//
//            MutableDouble.class, null,
//            MutableLong.class, null,

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



    public Result<X> optimize(int maxIterations, FloatFunction<Supplier<X>> eval) {
        return optimize(maxIterations, 1, eval);
    }

    public Result<X> optimize(int maxIterations, int repeats, FloatFunction<Supplier<X>> eval) {
        X sample = this.subjects.get();
        discover(sample);
        float sampleScore = eval.floatValueOf(subjects);
        System.out.println("control score=" + sampleScore);
        return optimize(subjects).run(maxIterations, repeats, eval);
    }

}
