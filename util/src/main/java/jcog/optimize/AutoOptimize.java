package jcog.optimize;

import com.google.common.primitives.Primitives;
import jcog.TODO;
import jcog.data.graph.ObjectGraph;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** automatically discovers tweakable fields for use in Optimize */
public class AutoOptimize<X> extends Optimize<X> {

    private static final int DEFAULT_DEPTH = 3;

    public AutoOptimize(Supplier<X> subject) {
        super(subject);
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

    protected void tweak(FastList<Pair<Class, ObjectGraph.Accessor>> path, Class targetType) {

        String key = key(path);
        if (targetType == Float.class) {
            final BiConsumer<X, Float> set = ObjectGraph.setter(path);
            tweak(key, 0, 1, 0.1f, set::accept);
        } else if (targetType == Integer.class) {
            final BiConsumer<X, Integer> set = ObjectGraph.setter(path);
            tweak(key, 0, 2, 1, set::accept);
        } else {
            throw new TODO(path + " -> " + targetType);
        }
    }

    private String key(List<Pair<Class, ObjectGraph.Accessor>> path) {
        return path.toString();
    }

    public boolean tweakable(Class<?> t) {
        //TODO support enum, strings, etc...
        return Primitives.unwrap(t).isPrimitive();
    }


}
