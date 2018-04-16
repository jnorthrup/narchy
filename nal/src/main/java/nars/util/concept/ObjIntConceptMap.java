//package nars.util.data;
//
//import nars.NAR;
//import nars.concept.Concept;
//import nars.term.Term;
//import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Iterator;
//
///** uses a predefined set of terms that will be mapped */
//public abstract class ObjIntConceptMap<T extends Term> extends MutableConceptMap<T> {
//
//
//    public final ObjectIntHashMap<T> values = new ObjectIntHashMap();
//
//
//    protected ObjIntConceptMap(@NotNull NAR nar) {
//        super(nar);
//    }
//
//    @NotNull
//    @Override
//    public Iterator<T> iterator() {
//        return values.keySet().iterator();
//    }
//
//    @Override
//    public boolean include(@NotNull Concept c) {
//        T t = (T) c.term();
//        return values.getIfAbsentPut(t, 0) == 0;
//    }
//
//    @Override
//    public boolean exclude(@NotNull Concept c) {
//        return exclude(c.term());
//    }
//
//    public boolean exclude(Term t) {
//        int size = values.size();
//        values.remove(t);
//        return (values.size() != size);
//    }
//
//
//    @Override
//    public boolean contains(T t) {
//        if (!values.containsKey(t)) {
//            return super.contains(t);
//        }
//        return true;
//    }
//
//
//    /** set a term to be present always in this map, even if the conept disappears */
//    @Override
//    public void include(T a) {
//        super.include(a);
//        values.getIfAbsentPut(a, 0);
//    }
//
//    /** remove an inclusion, and/or add an exclusion */
//    //TODO public void exclude(Term a) { }
//
//    public int size() {
//        return values.size();
//    }
//
//
//
//    public int add(T t, int dx) {
//        int c = values.getIfAbsent(t, Integer.MIN_VALUE);
//        if (c == Integer.MIN_VALUE) {
//            if (dx < 0)
//                return -1; //subtracting from an entry non-existint, return -1
//            else
//                c = 0; //begin at zero
//        }
//        c = c + dx;
//        if (c < 0)
//            c = 0;
//        values.put(t, c);
//        return c;
//    }
//
//    public int set(T t, int n) {
//        values.put(t, n);
//        return n;
//    }
//}
