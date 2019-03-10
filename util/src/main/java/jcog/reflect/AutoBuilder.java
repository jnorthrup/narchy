package jcog.reflect;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.math.MutableInteger;
import jcog.util.Reflect;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * generic reflective object decorator: constructs representations and multi-representations
 * from materialization abstractions
 */
public class AutoBuilder<X, Y> {

    /** essentially a decomposition of a subject into its components,
     *  include a descriptive relations to each */
    public interface Deduce<R,X> extends Iterable<Pair<R,X>> {

    }

    public static class DeduceFields<X,R,Y> implements Deduce<R,Y> {

        private final X source;

        public DeduceFields(X source) {
            this.source = source;
        }

        @Override
        public Iterator<Pair<R, Y>> iterator() {
            throw new TODO();
        }
    }

    /** for Iterable's incl. Collections */
    public static class DeduceIterable<X> implements Deduce<MutableInteger,X> {

        private final Iterable<X> i;

        public DeduceIterable(Iterable<X> i) {
            this.i = i;
        }
        @Override
        public Iterator<Pair<MutableInteger, X>> iterator() {
            throw new TODO();
        }
    }

    public static class DeduceMap<X,Y> implements Deduce<X,Y> {

        private final Map<X, Y> m;

        public DeduceMap(Map<X,Y> m) {
            this.m = m;
        }

        @Override
        public Iterator<Pair<X, Y>> iterator() {
            return Iterators.transform(m.entrySet().iterator(), (x) -> pair(x.getKey(), x.getValue()));
        }
    }

    /** inverse of deduce, somehow */
    public interface Induce {
        //TODO
    }

    public final Map<Class, BiFunction<? super X, Object /* relation */, Y>> onClass = new ConcurrentHashMap<>();
    public final Map<Predicate, Function<X, Y>> onCondition = new ConcurrentHashMap<>();
    final AutoBuilding<X, Y> building;
    private final int maxDepth;
    private final Set<Object> seen = Sets.newSetFromMap(new IdentityHashMap());

    public AutoBuilder(int maxDepth, AutoBuilding<X, Y> building) {
        this.building = building;
        this.maxDepth = maxDepth;
    }

    /**
     * builds the root item's representation
     */
    public final Y build(X root) {
        return build(root, null, null, root, 0);
    }

    @Nullable
    protected <C> Y build(C root, @Nullable Y parentRepr, Object relation, @Nullable X obj, int depth) {
        if (!add(obj))
            return null; //cycle

        List<Pair<X, Iterable<Y>>> target = new FasterList<>();


        FasterList<BiFunction<? super X, Object, Y>> builders = new FasterList();

//        {
//            if (!onCondition.isEmpty()) {
//                onCondition.forEach((Predicate test, Function builder) -> {
//                    if (test.test(x)) {
//                        Y y = (Y) builder.apply(x);
//                        if (y != null)
//                            built.addAt(pair(x,y));
//                    }
//                });
//            }
//        }

        {
            classBuilders(obj, builders); //TODO check subtypes/supertypes etc
            if (!builders.isEmpty()) {
                Iterable<Y> yy = () -> builders.stream().map(b -> b.apply(obj, relation)).filter(Objects::nonNull).iterator();
                target.add(pair(obj, yy));
            }
        }

        //if (bb.isEmpty()) {
        if (depth <= maxDepth) {
            collectFields(root, obj, parentRepr, target, depth + 1);
        }
        //}


        return building.build(root, target, obj);
    }

    private void classBuilders(X x, FasterList<BiFunction<? super X, Object, Y>> ll) {
        Class<?> xc = x.getClass();
//        Function<X, Y> exact = onClass.get(xc);
//        if (exact!=null)
//            return exact;

        //exhaustive search
        // TODO cache in a type graph
        onClass.forEach((k, v) -> {
            if (k.isAssignableFrom(xc))
                ll.add(v);
        });
    }

    public void clear() {
        seen.clear();
    }

    private <C> void collectFields(C c, X x, Y parentRepr, Collection<Pair<X, Iterable<Y>>> target, int depth) {

        Class cc = x.getClass();
        Reflect.on(cc).fields(true, false, false).forEach((s, ff) -> {
            Field f = ff.get();
            try {
                Object xf = f.get(x);
                if (xf != null && xf != x) {
                    X z = (X) xf;
                    Y w = build(c, parentRepr, f, z, depth);
                    if (w != null)
                        target.add(pair(z, List.of(w)));
                }
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

    public <C extends X> AutoBuilder<X, Y> on(Class<C> c, BiFunction<C, Object, Y> each) {
        onClass.put(c, (BiFunction<? super X, Object, Y>) each);
        return this;
    }

    public AutoBuilder<X, Y> on(Predicate test, Function<X, Y> each) {
        onCondition.put(test, each);
        return this;
    }

    /** TODO use Deduce interface */
    @FunctionalInterface
    public interface AutoBuilding<X, Y> {
        Y build(Object context, List<Pair<X, Iterable<Y>>> features, X obj);
    }
}
