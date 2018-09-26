package spacegraph.space2d.widget.meta;

import com.google.common.collect.Sets;
import jcog.data.list.FasterList;
import jcog.util.Reflect;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class AutoBuilder<X, Y> {

    public AutoBuilder(int maxDepth, AutoBuilding<X, Y> building) {
        this.building = building;
        this.maxDepth = maxDepth;
    }

    /**
     * builds the root item's representation
     */
    public final Y build(X root) {
        return build(root, null, null, 0);
    }

    protected Y build(X root, @Nullable Y parentRepr, @Nullable Object relation, int depth) {
        List<Pair<X, Iterable<Y>>> bb = new FasterList<>();
        collect(root, bb, depth, relation);
        return building.build(bb, root, parentRepr);
    }

    private void collect(X x, List<Pair<X,Iterable<Y>>> built, int depth, Object relation) {

        if (!add(x))
            return;

        FasterList<BiFunction<X,Object,Y>> builders = new FasterList();

//        {
//            if (!onCondition.isEmpty()) {
//                onCondition.forEach((Predicate test, Function builder) -> {
//                    if (test.test(x)) {
//                        Y y = (Y) builder.apply(x);
//                        if (y != null)
//                            built.add(pair(x,y));
//                    }
//                });
//            }
//        }

        {
            classBuilders(x, builders); //TODO check subtypes/supertypes etc
            if (!builders.isEmpty()) {
                Iterable<Y> yy = ()->builders.stream().map(b -> b.apply(x, relation)).iterator();
                built.add(pair(x, yy));
            }
        }

        if (depth < maxDepth) {
            collectFields(x, built, depth + 1);
        }


    }

    private void classBuilders(X x, FasterList<BiFunction<X,Object,Y>> ll) {
        Class<?> xc = x.getClass();
//        Function<X, Y> exact = onClass.get(xc);
//        if (exact!=null)
//            return exact;

        //exhaustive search
        // TODO cache in a type graph
        onClass.forEach((k,v)->{
            if (k.isAssignableFrom(xc))
                ll.add(v);
        });
    }

    public void clear() {
        seen.clear();
    }

//    private List<Pair<X, Y>> collectElements(Iterable<?> x, int depth) {
//        List<Pair<X,Iterable<Y>>> m = new FasterList();
//        for (Object o : x) {
//            collect((X)o, m, depth);
//        }
//        return m;
//    }


    private void collectFields(Object x, List<Pair<X,Iterable<Y>>> target, int depth) {
        Class cc = x.getClass();
        Reflect.on(cc).fields(true, false, false).forEach((s, ff) -> {
            Field f = ff.get();
            try {
                Object xf = f.get(x);
                if (xf != null && xf != x)
                    collect((X)xf, target, depth, f);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
//        for (Field f : cc.getFields()) {
//
//            int mods = f.getModifiers();
//            if (Modifier.isStatic(mods))
//                continue;
//            if (!Modifier.isPublic(mods))
//                continue;
//            if (f.getType().isPrimitive())
//                continue;
//
//            try {
//
//
//                f.trySetAccessible();
//
//
//                Object y = f.get(x);
//                if (y != null && y != x)
//                    collect(y, target, depth, f.getName());
//
//            } catch (Throwable t) {
//                t.printStackTrace();
//            }
//        }


    }


    private boolean add(Object x) {
        return seen.add(x);
    }

    @FunctionalInterface
    public interface AutoBuilding<X, Y> {
        Y build(List<Pair<X, Iterable<Y>>> content, @Nullable X parent, @Nullable Y parentRepr);
    }

    final AutoBuilding<X, Y> building;
    private final int maxDepth;
    private final Set<Object> seen = Sets.newSetFromMap(new IdentityHashMap());


    public final Map<Class, BiFunction<X, Object /* relation */, Y>> onClass = new ConcurrentHashMap<>();
    public final Map<Predicate, Function<X, Y>> onCondition = new ConcurrentHashMap<>();

    public AutoBuilder<X, Y> on(Class c, BiFunction<X, Object, Y> each) {
        onClass.put(c, each);
        return this;
    }

    public AutoBuilder<X, Y> on(Predicate test, Function<X, Y> each) {
        onCondition.put(test, each);
        return this;
    }
}
