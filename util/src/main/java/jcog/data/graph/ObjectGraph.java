package jcog.data.graph;

import jcog.TODO;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * see: https://github.com/bramp/objectgraph/blob/master/src/main/java/net/bramp/objectgraph/ObjectGraph.java
 */
public abstract class ObjectGraph extends MapNodeGraph<Object, ObjectGraph.Accessor /* TODO specific types of edges */> {

    private static final Logger logger = LoggerFactory.getLogger(ObjectGraph.class);

    public ObjectGraph(int depth, Object... seeds) {
        this();
        add(depth, seeds);
    }

    public ObjectGraph() {
        super(new IdentityHashMap<>());
    }

    public ObjectGraph add(int depth, Object... xx) {
        for (Object x: xx)
            add(x, depth);
        return this;
    }

    public MutableNode<Object,Accessor> add(Object x, int depth) {
        return add(x, x, new FasterList<>(), depth);
    }

    protected MutableNode<Object,Accessor> add(Object root, Object x, FasterList<Pair<Class,Accessor>> path, int level) {


        MutableNode<Object, Accessor> n = addNode(x);

        if (level == 0)
            return n;

        Class<?> clazz = x.getClass();

        //boolean terminate = visitor.visit(x, clazz);
        //if (terminate) return;

        //if (!canDescend(clazz)) continue;

        if (clazz.isArray()) {
            // If an Array, add each element to follow up
            //Class<?> arrayType = clazz.getComponentType();

            if (includeClass(clazz.getComponentType())) {
                final int len = Array.getLength(x);

                for (int i = 0; i < len; i++) {
                    Object aa = Array.get(x, i);
                    if ((aa != null ||  includeNull()) && includeValue(aa)) {
                        access(root, n, clazz, aa, new ArrayAccessor(clazz, i), path, level);
                    }
                }
            }

        } else {
            // If a normal class, add each field
            fields(x.getClass()).forEach(field -> {

                if (!includeField(field))   return;

                Class<?> fieldType = field.getType();

                // If the field type is directly on the exclude list, then skip.
                // Strictly this isn't needed as isExcludedClass is called later, but this is cheap
                // and avoids getting the object, which could be expensive (think hibernate).
                if (!includeClass(fieldType)) return;

                try {
                    field.setAccessible(true);

                    Object value = field.get(x);

                    if ((value != null ||  includeNull()) && includeValue(value)) {
                        FieldAccessor axe = new FieldAccessor(field);
                        access(root, n, clazz, value, axe, path, level);
                    }

                } catch (IllegalAccessException e) {
                    // Ignore the exception
                    logger.info("field access {}", e);
                }
            });
        }

        return n;
    }



    private void access(Object root, MutableNode<Object, Accessor> src, Class<?> srcClass, Object target, Accessor axe, FasterList<Pair<Class, Accessor>> path, int level) {
        path.add(pair(srcClass, axe));

        if (!access(root, path, target))
            return;

        addEdge(src, axe, add(root, target, path, level - 1));
        path.removeLast();
    }

    protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
        return true;
    }

    public boolean includeNull() {
        return false;
    }

    abstract public boolean includeValue(Object v);

    abstract public boolean includeClass(Class<?> c);

    abstract public boolean includeField(Field f);

//isPrimitive..
//    int modifiers = field.getModifiers();
//
//    boolean excludeStatic = false;
//    boolean excludeTransient = false;
//
//                if (excludeStatic && (modifiers & Modifier.STATIC) == Modifier.STATIC) continue;
//
//                if (excludeTransient && (modifiers & Modifier.TRANSIENT) == Modifier.TRANSIENT) continue;


//    @Override
//    protected Node<Object, Object> newNode(Object x) {
//        return new ObjectNode(x);
//    }
//
//
//    class ObjectNode extends MutableNode<Object,Object> {
//
//        public ObjectNode(Object x) {
//            super(x, new LinkedHashSet<>(), new LinkedHashSet<>());
//
//        }
//
//
//
//    }

    /**
     * Return all declared and inherited fields for this class.
     * TODO cache
     */
    protected Stream<Field> fields(Class<?> clazz) {

        Stream<Field> s = Stream.of(clazz.getDeclaredFields());

        Class<?> sc = clazz.getSuperclass();
        if (sc != null && includeClass(sc)) {
            s = Stream.concat(s, fields(sc));
        }

        return s;
    }

    /** creates a field setter from a path */
    public static <X,V> BiConsumer<X,V> setter(FastList<Pair<Class, Accessor>> path) {
        return (X root, V val) -> {
            Object current = root;
            for (int i = 0, pathSize = path.size()-1; i < pathSize; i++) {
                Pair<Class, Accessor> p = path.get(i);
                current = p.getTwo().get(current);
            }
            path.getLast().getTwo().set(current, val);
        };
    }

    abstract public static class Accessor {
        abstract Object get(Object container);
        abstract void set(Object container, Object value);
    }

    /** TODO use VarHandle or something faster than reflect.Field */
    public static class FieldAccessor extends Accessor {
        public final Field field;

        FieldAccessor(Field field) {
            this.field = field;
        }

        @Override
        public String toString() {
            return field.getName();
        }

        @Override
        Object get(Object container) {
            try {
                return field.get(container);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        void set(Object container, Object value) {
            try {
                field.set(container, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ArrayAccessor extends Accessor {
        final Class type;
        final int index;

        protected ArrayAccessor(Class type, int index) {
            this.type = type;
            this.index = index;
        }

        @Override
        public String toString() {
            return type + "[" + index + ']';
        }

        @Override
        Object get(Object container) {
            throw new TODO();
        }

        @Override
        void set(Object container, Object value) {
            throw new TODO();
        }
    }
}
