package nars.term.transform;

import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;

import java.util.HashMap;
import java.util.Map;

/**
 * Variable normalization
 * <p>
 * Destructive mode modifies the input Compound instance, which is
 * fine if the concept has been created and unreferenced.
 * <p>
 * The term 'destructive' is used because it effectively destroys some
 * information - the particular labels the input has attached.
 */
public class VariableNormalization extends VariableTransform {

    /**
     * indexing offset of assigned variable id's
     */
    private final int offset;

    protected int count;

    /*@NotNull*/
    public final Map<Variable /* Input Variable */, Variable /*Variable*/> map;

    //private boolean renamed;

    /*public VariableNormalization() {
        this(0);
    }*/

    /**
     * for use with compounds that have exactly one variable (when offset=0, default)
     */
    public static final VariableTransform singleVariableNormalization = new VariableTransform() {

        @Override public Term transform(Term atomic) {
            return atomic instanceof Variable ? atomic.normalize((byte)1) : atomic;
        }
    };


    @Override
    public final Termed transform(Term atomic) {
        if (atomic instanceof Variable) {
            if (atomic.equals(Op.Imdex)) {
                //anonymized to a unique variable each occurrence
                return newVariableIncreasingCount((Variable) atomic);
            } else {
                return map.computeIfAbsent((Variable) atomic, this::newVariableIncreasingCount);
            }
        } else {
            return atomic;
        }
    }


    /*@NotNull*/
    private Variable newVariableIncreasingCount(/*@NotNull*/ Variable x) {
        ++count;
        return newVariable(x);
    }

    /*@NotNull*/
    protected Variable newVariable(/*@NotNull*/ Variable x) {
        //Variable y;

        int vid = this.count + offset;

        return x.normalize((byte)vid);

//        if (x instanceof UnnormalizedVariable) {
//            y = ((UnnormalizedVariable) x).normalize(vid); //HACK
//        } else {
//
//            //y = y.equals(x) ? x : y; //attempt to use the original if they are equal, this can help prevent unnecessary transforms etc
//        }

        //return y ;

    }


    public VariableNormalization() {
        this(new HashMap<>(4, 0.9f), 0);
    }

    public VariableNormalization(int size /* estimate */, int offset) {
        this(new HashMap<>(size), offset);
    }

    public VariableNormalization(/*@NotNull*/ Map<Variable, Variable> r) {
        this(r, 0);
    }

    public VariableNormalization(/*@NotNull*/ Map<Variable, Variable> r, int offset) {
        this.offset = offset;
        this.map = r;

        //NOTE:
        //rename = new ConcurrentHashMap(size); //doesnt work being called recursively
        //rename = new HashMap(size); //doesnt work being called recursively
    }


//    final static Comparator<Map.Entry<Variable, Variable>> comp = new Comparator<Map.Entry<Variable, Variable>>() {
//        @Override
//        public int compare(Map.Entry<Variable, Variable> c1, Map.Entry<Variable, Variable> c2) {
//            return c1.getKey().compareTo(c2.getKey());
//        }
//    };

//    /**
//     * overridden keyEquals necessary to implement alternate variable hash/equality test for use in normalization's variable transform hashmap
//     */
//    static final class VariableMap extends FastPutsArrayMap<Pair<Variable,Term>, Variable> {
//
//
//
//        public VariableMap(int initialCapacity) {
//            super(initialCapacity);
//        }
//
//        @Override
//        public final boolean keyEquals(final Variable a, final Object ob) {
//            if (a == ob) return true;
//            Variable b = ((Variable) ob);
//            return Byted.equals(a, b);
//        }
//
////        @Override
////        public Variable put(Variable key, Variable value) {
////            Variable removed = super.put(key, value);
////            /*if (size() > 1)
////                Collections.sort(entries, comp);*/
////            return removed;
////        }
//    }


}
