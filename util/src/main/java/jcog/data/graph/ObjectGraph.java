package jcog.data.graph;

import jcog.data.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.IdentityHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * see: https:
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

    private Node<Object, Accessor> add(Object x, int depth) {
        return add(x, x, new FasterList<>(), depth);
    }

    private MutableNode<Object,Accessor> add(Object root, Object x, FasterList<Pair<Class, Accessor>> path, int level) {


        MutableNode<Object, Accessor> n = addNode(x);

        if ((level == 0) || !recurse(x))
            return n;

        Class<?> clazz = x.getClass();

        
        

        

        if (clazz.isArray()) {
            
            

            if (includeClass(clazz.getComponentType())) {
                int len = Array.getLength(x);

                for (int i = 0; i < len; i++) {
                    Object aa = Array.get(x, i);
                    if ((aa != null ||  includeNull()) && includeValue(aa)) {
                        access(root, n, clazz, aa, new ArrayAccessor(clazz, i), path, level);
                    }
                }
            }

        } else {
            
            fields(x.getClass()).forEach(field -> {

                if (!includeField(field))   return;

                Class<?> fieldType = field.getType();

                
                
                
                if (!includeClass(fieldType)) return;

                try {
                    try {
                        field.setAccessible(true);

                        Object value = field.get(x);

                        if ((value != null ||  includeNull()) && includeValue(value)) {
                            FieldAccessor axe = new FieldAccessor(field);
                            access(root, n, clazz, value, axe, path, level);
                        }

                    } catch (InaccessibleObjectException ioe) {
                        logger.debug("inaccessible: {} {}", field, ioe );
                    }


                } catch (IllegalAccessException e) {
                    
                    logger.info("field access {}", e);
                }
            });
        }

        return n;
    }



    private void access(Object root, MutableNode<Object, Accessor> src, Class<?> srcClass, Object target, Accessor axe, FasterList<Pair<Class, Accessor>> path, int level) {
        path.add(pair(srcClass, axe));

        if (access(root, path, target)) {
            addEdgeByNode(src, axe, add(root, target, path, level - 1));
        }

        path.removeLast();
    }

    protected boolean access(Object root, FasterList<Pair<Class, Accessor>> path, Object target) {
        return true;
    }

    private static boolean includeNull() {
        return false;
    }

    /** whether to recurse into a value, after having added it as a node */
    public boolean recurse(Object x) { return true; }

    public abstract boolean includeValue(Object v);

    public abstract boolean includeClass(Class<?> c);

    public abstract boolean includeField(Field f);





























    /**
     * Return all declared and inherited fields for this class.
     * TODO cache
     */
    private Stream<Field> fields(Class<?> clazz) {

        Stream<Field> s = Stream.of(clazz.getDeclaredFields());

        Class<?> sc = clazz.getSuperclass();
        if (sc != null && includeClass(sc)) {
            s = Stream.concat(s, fields(sc));
        }

        return s;
    }

    /** creates a field setter from a path */
    public static <X,V> BiConsumer<X,V> setter(FastList<Pair<Class, Accessor>> path) {
        return (root, val) -> {
            Object current = root;

            for (int i = 0, pathSize = path.size()-1; i < pathSize; i++)
                current = path.get(i).getTwo().get(current);

            path.getLast().getTwo().set(current, val);
        };
    }
    /** creates a field getter from a path */
    public static <X,Y> Function<X,Y> getter(FastList<Pair<Class, Accessor>> path) {
        return root -> {
            Object current = root;

            for (int i = 0, pathSize = path.size()-1; i < pathSize; i++)
                current = path.get(i).getTwo().get(current);

            return (Y) path.getLast().getTwo().get(current);
        };
    }
    public abstract static class Accessor {
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

    static class ArrayAccessor extends Accessor {
        final Class type;
        final int index;

        ArrayAccessor(Class type, int index) {
            this.type = type;
            this.index = index;
        }

        @Override
        public String toString() {
            return type + "[" + index + ']';
        }

        @Override
        Object get(Object container) {
            return Array.get(container, index);
        }

        @Override
        void set(Object container, Object value) {
            Array.set(container, index, value);
        }
    }
}
