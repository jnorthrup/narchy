package nars.op.java;

import com.google.common.collect.ImmutableSet;
import jcog.TODO;
import jcog.Util;
import jcog.data.map.CustomConcurrentHashMap;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;


/**
 * Created by me on 8/19/15.
 */
public class DefaultTermizer implements Termizer {


    public static final Variable INSTANCE_VAR = $.varDep("instance");


    final Map<Term, Object> termToObj = new CustomConcurrentHashMap<>(STRONG, EQUALS, STRONG /*SOFT*/, IDENTITY, 64);
    final Map<Object, Term> objToTerm = new CustomConcurrentHashMap<>(STRONG /*SOFT*/, IDENTITY, STRONG, EQUALS, 64); 

    /*final HashMap<Term, Object> instances = new HashMap();
    final HashMap<Object, Term> objects = new HashMap();*/

    static final Set<Class> classInPackageExclusions = ImmutableSet.of(
            Class.class,
            Object.class,


            Float.class,
            Double.class,
            Boolean.class,
            Character.class,
            Long.class,
            Integer.class,
            Short.class,
            Byte.class,
            Class.class
    );

    public DefaultTermizer() {
        termToObj.put(Op.True, true);
        termToObj.put(Op.False, false);
        objToTerm.put(true, Op.True);
        objToTerm.put(false, Op.False);
    }

    public void put(Term x, Object y) {
        assert (x != y);

        termToObj.put(x, y);
        objToTerm.put(y, x);

    }

    public void remove(Term x) {

        Object y = termToObj.remove(x);
        objToTerm.remove(y);

    }

    public void remove(Object x) {

        Term y = objToTerm.remove(x);
        termToObj.remove(y);

    }

    /**
     * dereference a term to an object (but do not un-termize)
     */
    @Nullable
    @Override
    public Object object(Term t) {

        if (t == NULL) return null;
        if (t instanceof Int && t.op() == INT)
            return ((Int) t).id;

        Object x = termToObj.get(t);
        if (x == null)
            return t; /** return the term intance itself */

        return x;
    }


    @Nullable
    Term obj2term(@Nullable Object o) {

        if (o == null)
            return NULL;


        if (o instanceof Term) return (Term) o;

        if (o instanceof String)
            return $.quote(o);

        if (o instanceof Boolean)
            return ((Boolean) o) ? Op.True : Op.False;

        if (o instanceof Character)
            return $.quote(String.valueOf(o));

        if (o instanceof Number)
            return number((Number) o);

        if (o instanceof Class) {
            Class oc = (Class) o;
            return classTerm(oc);


        }

        if (o instanceof int[]) {
            return $.p((int[]) o);
        }


        if (o instanceof Object[]) {
            List<Term> arg = Arrays.stream((Object[]) o).map(this::term).collect(Collectors.toList());
            if (arg.isEmpty()) return EMPTY;
            return $.p(arg);
        }

        if (o instanceof List) {
            if (((Collection) o).isEmpty()) return EMPTY;


            Collection c = (Collection) o;
            List<Term> arg = $.newArrayList(c.size());
            for (Object x: c) {
                Term y = term(x);
                arg.add(y);
            }

            if (arg.isEmpty()) return EMPTY;

            return $.p(arg);

        /*} else if (o instanceof Stream) {
            return Atom.quote(o.toString().substring(17));
        }*/
        }

        if (o instanceof Set) {
            Collection<Term> arg = (Collection<Term>) ((Collection) o).stream().map(this::term).collect(Collectors.toList());
            if (arg.isEmpty()) return EMPTY;
            return SETe.the((Collection) arg);
        } else if (o instanceof Map) {

            Map mapo = (Map) o;
            Set<Term> components = $.newHashSet(mapo.size());
            mapo.forEach((k, v) -> {

                Term tv = obj2term(v);
                Term tk = obj2term(k);

                if ((tv != null) && (tk != null)) {
                    components.add(
                            $.inh(tv, tk)
                    );
                }
            });
            if (components.isEmpty()) return EMPTY;
            return SETe.the((Collection) components);
        }


        return instanceTerm(o);


    }

    protected static Term number(Number o) {
        return $.the(o);
    }


    private boolean reportClassInPackage(@NotNull Class oc) {
        if (classInPackageExclusions.contains(oc)) return false;

        if (Term.class.isAssignableFrom(oc)) return false;
        return !oc.isPrimitive();


    }


    /**
     * (#arg1, #arg2, ...), #returnVar
     */
    @NotNull
    private Term[] getMethodArgVariables(@NotNull Method m) {


        String varPrefix = m.getName() + '_';
        int n = m.getParameterCount();
        Term args = $.p(getArgVariables(varPrefix, n));

        return m.getReturnType() == void.class ? new Term[]{
                INSTANCE_VAR,
                args
        } : new Term[]{
                INSTANCE_VAR,
                args,
                $.varDep(varPrefix + "_return")
        };
    }

    @NotNull
    private static Term[] getArgVariables(String prefix, int numParams) {
        Term[] x = new Term[numParams];
        for (int i = 0; i < numParams; i++) {
            x[i] = $.varDep(prefix + i);
        }
        return x;
    }

    public static Term classTerm(Class c) {


        return Atomic.the(c.getSimpleName());


    }

    public static Term termClassInPackage(@NotNull Class c) {
        return $.p(termPackage(c.getPackage()), classTerm(c));
    }

    @NotNull
    public static Term termPackage(@NotNull Package p) {


        String n = p.getName();

        String[] path = n.split("\\.");
        return $.p(path);


    }

    /**
     * generic instance term representation
     */
    public static Term instanceTerm(Object o) {


        return $.the(System.identityHashCode(o), 36);
    }

    @Nullable
    protected Term classInPackage(Term classs, @Deprecated Term packagge) {

        return null;
    }

    public Term[] terms(Object[] args) {
        return Util.map(this::term, Term[]::new, args);
    }

    @Override
    @Nullable
    public Term term(@Nullable Object o) {
        if (o == null) return NULL;
        else if (o instanceof Boolean) {
            return (Boolean) o ? Op.True : Op.False;
        } else if (o instanceof Number) {
            if (o instanceof Byte || o instanceof Short || o instanceof Integer || (o instanceof Long && Math.abs((Long) o) < Integer.MAX_VALUE - 1)) {
                return Int.the(((Number) o).intValue());
            } else if (o instanceof Float || o instanceof Double) {
                return $.the(((Number) o).doubleValue());
            } else if (o instanceof Long /* beyond an Int's capacity */) {
                return $.the(Long.toString((Long) o));
            } else {
                throw new TODO("support: " + o + " (" + o.getClass() + ")");
            }
        } else if (o instanceof String) {
            return Atomic.the((String) o);
        }

        Term y = obj2termCached(o);
        if (y != null)
            return y;

        if (o instanceof Term)
            return (Term) o;

        if (o instanceof Object[])
            return PROD.the(terms((Object[]) o));

        return null;


    }

    @Nullable
    public Term obj2termCached(@Nullable Object o) {

        if (o == null) return NULL;
        if (o instanceof Term)
            return ((Term) o);
        if (o instanceof Integer) {
            return Int.the((Integer) o);
        }


        Term oe;
        if (cacheableInstance(o)) {
            oe = objToTerm.get(o);
            if (oe == null) {
                Term ob = obj2term(o);
                if (ob != null)
                    oe = objToTerm.put(o, ob);
                else
                    return $.varDep("unknown_" + System.identityHashCode(o));
            }
        } else {
            oe = obj2term(o);
        }

        return oe;
    }

    private boolean cacheableInstance(Object o) {


        return true;
    }


    protected void onInstanceChange(Term oterm, Term prevOterm) {

    }

    protected void onInstanceOfClass(Object o, Term oterm, Term clas) {

    }

//    @NotNull
//    public static <T extends Term> Map<Atomic,T> mapStaticClassFields(@NotNull Class c, @NotNull Function<Field, T> each) {
//        Field[] ff = c.getFields();
//        Map<Atomic,T> t = $.newHashMap(ff.length);
//        for (Field f : ff) {
//            if (Modifier.isStatic(f.getModifiers())) {
//                T xx = each.apply(f);
//                if (xx!=null) {
//                    t.put(Atomic.the(f.getName()), xx);
//                }
//            }
//        }
//        return t;
//    }


}
