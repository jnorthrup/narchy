package jcog.reflect;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import jcog.TODO;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.data.list.FasterList;
import jcog.func.TriFunction;
import jcog.math.MutableInteger;
import jcog.sort.FloatRank;
import jcog.sort.RankedN;
import jcog.util.Reflect;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * generic reflective object decorator: constructs representations and multi-representations
 * from materialization abstractions
 */
public class AutoBuilder<X, Y> {

    public final Map<Class, TriFunction/*<Field, Object, Object, Object>*/> annotation;
    public final Map<Class, BiFunction<? super X, Object /* relation */, Y>> onClass;
    public final Map<Predicate, Function<X, Y>> onCondition;

    final AutoBuilding<X, Y> building;
    private final int maxDepth;

    private final Set<Object> seen = Sets.newSetFromMap(new IdentityHashMap());

    public AutoBuilder(int maxDepth, AutoBuilding<X, Y> building, Map<Class, BiFunction<? super X, Object, Y>> onClass) {
        this.building = building;
        this.maxDepth = maxDepth;
        this.annotation = new HashMap();
        this.onClass = onClass;
        this.onCondition = new HashMap<>();
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


        FasterList<BiFunction<Object, Object, Y>> builders = new FasterList();

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
                target.add(pair(obj,
                        () -> builders.stream().map(b -> b.apply(obj, relation)).filter(Objects::nonNull).iterator()
                ));
            }
        }

        //if (bb.isEmpty()) {
        if (depth <= maxDepth) {
            collectFields(root, obj, parentRepr, target, depth + 1);
        }

//        if (obj instanceof Map) {
//            ((Map<?,?>) obj).entrySet().stream()
//                    .map((Map.Entry<?,?> x) ->
//                            Tuples.pair(obj,
//                                (Iterable<Y>)(builders.stream().map(b ->
//                                    b.apply(x, relation)).filter(Objects::nonNull)::iterator)))
//                    .forEach(target::add);
//        }/* else if (obj instanceof List) {
//            ((List<?>)obj).stream()
//                    .map((Object x) ->
//                            Tuples.pair(obj, (Iterable<Y>)(builders.stream().map(b ->
//                                            b.apply(x, relation)).filter(Objects::nonNull)::iterator)))
//                    .forEach(target::add);
//
//        }*/


        return building.build(root, target, obj);
    }

    private void classBuilders(X x, FasterList<BiFunction</* X */Object, Object, Y>> ll) {
        Class<?> xc = x.getClass();
//        Function<X, Y> exact = onClass.get(xc);
//        if (exact!=null)
//            return exact;

        //exhaustive search
        // TODO cache in a type graph
        onClass.forEach((k, v) -> {
            if (k.isAssignableFrom(xc))
                ll.add((BiFunction) v);
        });
    }

    public void clear() {
        seen.clear();
    }

    public <Annotation, FieldValue> void annotation(Class<? extends Annotation> essenceClass, TriFunction<Field, FieldValue, Annotation, Object> o) {
        annotation.put(essenceClass, o);
    }

    private <C> void collectFields(C c, X x, Y parentRepr, Collection<Pair<X, Iterable<Y>>> target, int depth) {

        Reflect.on(x.getClass()).fields(true, false, false).forEach((s, ff) -> {

            try {
                Field f = ff.get();
                Object fVal = f.get(x);
                if(Modifier.isPublic(f.getModifiers())) {
                    for (Map.Entry<Class, TriFunction> e : annotation.entrySet()) {
                        java.lang.annotation.Annotation fe = f.getAnnotation(e.getKey());
                        if (fe!=null) {
                            Object v = e.getValue().apply(f, fVal, fe);
                            if (v != null) {
                                Object vv;
                                try {
                                    //HACK
                                    vv = build((X) v);
                                } catch (ClassCastException ce) {
                                    //continue
                                    vv = v;
                                }
                                if (vv!=null) {
                                    fVal = vv;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (fVal != null && fVal != x) {
                    X z = (X) fVal;
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

    /** TODO */
    enum RelationType {
        Root,
        Field,
        Dereference,
        ListElement,
        MapElement
        //..
    }

//    public <C extends X> AutoBuilder<X, Y> on(Class<C> c, BiFunction<C, /*RelationType*/Object, Y> each) {
//        onClass.put(c, (BiFunction<? super X, Object, Y>) each);
//        return this;
//    }

    public AutoBuilder<X, Y> on(Predicate test, Function<X, Y> each) {
        onCondition.put(test, each);
        return this;
    }

    /** TODO use Deduce interface */
    @FunctionalInterface
    public interface AutoBuilding<X, Y> {
        Y build(Object context, List<Pair<X, Iterable<Y>>> features, X obj);
    }

    abstract public static class Way<X> implements Supplier<X> {
        public String name;
    }

    /** supplies zero or more chocies from a set */
    public static class Some<X> implements Supplier<X[]> {
        final Way<X>[] way;
        final AtomicMetalBitSet enable = new AtomicMetalBitSet();

        public Some(Way<X>[] way) {
            this.way = way;
            assert(way.length > 1 && way.length <= 31 /* AtomicMetalBitSet limit */);
        }

        public Some<X> set(int which, boolean enable) {
            this.enable.set(which, enable);
            return this;
        }

        @Override
        public X[] get() {
            throw new TODO();
        }

        public int size() {
            return way.length;
        }
    }

    public static class Best<X> extends RankedN implements Supplier<X> {
        final Some<X> how;
        final FloatRank<X> rank;

        public Best(Some<X> how, FloatRank<X> rank) {
            super(new Object[how.size()], rank);
            this.how = how;
            this.rank = rank;
        }

        @Override
        public X get() {
            clear();
            X[] xx = how.get();
            if (xx.length == 0)
                return null;
            for (X x : xx)
                add(x);
            return (X) top();
        }
    }

    /** forces a one or none choice from a set */
    public static class Either<X> implements Supplier<X> {
        final Way<X>[] way;
        volatile int which = -1;

        public Either(Way<X>... way) {
            assert(way.length > 1);
            this.way = way;
        }

        public Either<X> set(int which) {
            this.which = which;
            return this;
        }

        public final Either<X> disable() {
            set(-1);
            return this;
        }

        @Override
        public X get() {
            int c = this.which;
            return c >=0 ? way[c].get() : null;
        }
    }

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

}
