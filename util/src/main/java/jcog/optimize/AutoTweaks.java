package jcog.optimize;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Primitives;
import jcog.TODO;
import jcog.data.graph.ObjectGraph;
import jcog.list.FasterList;
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
import java.util.function.Supplier;

/** automatically discovers tweakable fields for use in Optimize */
public class AutoTweaks<X> extends Tweaks<X> {

    private static final int DEFAULT_DEPTH = 3;
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
                    tweak(path.clone(), targetType);
                }
                return true;
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

                Class<?> t = f.getType();
                if (tweakable(t))
                    return !Modifier.isFinal(m);
                else
                    return true; //explore further into Object's, final or not
            }
        };

        o.add(DEFAULT_DEPTH, x);

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

    protected void tweak(FastList<Pair<Class, ObjectGraph.Accessor>> path, Class targetType) {

        String key = key(path);
        if (targetType == Float.class) {
            final BiConsumer<X, Float> set = ObjectGraph.setter(path);

            tweak(key, Float.NaN,Float.NaN,Float.NaN, set::accept);
            learn(key, path);
        } else if (targetType == Integer.class) {
            final BiConsumer<X, Integer> set = ObjectGraph.setter(path);
            tweak(key, set::accept);
            learn(key, path);
        } else {
            throw new TODO(path + " -> " + targetType);
        }
    }

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

    public boolean tweakable(Class<?> t) {
        //TODO support enum, strings, etc...
        return Primitives.unwrap(t).isPrimitive();
    }


    public Optimize<X> optimize() {
        return optimize(subjects, Map.of());
    }

    public Result<X> optimize(int maxIterations, FloatFunction<X> eval) {
        return optimize().run(maxIterations, eval);
    }

}
