package nars.op.java;

import com.google.common.collect.ImmutableSet;
import jcog.TODO;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.map.CustomConcurrentHashMap;
import nars.$;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;


/**
 * Created by me on 8/19/15.
 */
public class DefaultTermizer implements Termizer {


    public static final Variable INSTANCE_VAR = $.varDep("instance");
    static final Term TRUE_TERM =
            Atomic.the("TRUE");
            //Bool.True;
    static final Term FALSE_TERM =
            TRUE_TERM.neg();
            //Bool.False;


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
        termToObj.put(TRUE_TERM, true);
        termToObj.put(FALSE_TERM, false);
        objToTerm.put(true, TRUE_TERM);
        objToTerm.put(false, FALSE_TERM);
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
     * dereference a target to an object (but do not un-termize)
     */
    @Override
    public @Nullable Object object(Term t) {

        if (t == NULL) return null;
        if (t instanceof Int && t.op() == INT)
            return ((Int) t).i;

        Object x = termToObj.get(t);
        if (x == null)
            return t; /** return the target intance itself */

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
            return ((Boolean) o) ? TRUE_TERM : FALSE_TERM;

        if (o instanceof Character)
            return $.quote(String.valueOf(o));

        if (o instanceof Number)
            return number((Number) o);

        if (o instanceof Class) {
            Class oc = (Class) o;
            return classTerm(oc);
        }

        if (o instanceof Path) {
            return $.the((Path)o);
        }
        if (o instanceof URI) {
            return $.the((URI)o);
        }
        if (o instanceof URL) {
            return $.the((URL)o);
        }

        if (o instanceof int[]) {
            return $.p((int[]) o);
        }


        if (o instanceof Object[]) {
            List<Term> arg = new ArrayList<>();
            for (Object o1 : (Object[]) o) {
                Term term = term(o1);
                arg.add(term);
            }
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
            return SETe.the(arg);
        } else if (o instanceof Map) {

            Map mapo = (Map) o;
            List<Term> components = new FasterList(mapo.size());
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
            return SETe.the(components);
        }


        return instanceTerm(o);


    }

    protected static Term number(Number o) {
        return $.the(o);
    }


    private static boolean reportClassInPackage(Class oc) {
        if (classInPackageExclusions.contains(oc)) return false;

        if (Term.class.isAssignableFrom(oc)) return false;
        return !oc.isPrimitive();


    }


    /**
     * (#arg1, #arg2, ...), #returnVar
     */
    
    private static Term[] getMethodArgVariables(Method m) {


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

    
    private static Term[] getArgVariables(String prefix, int numParams) {
        List<nars.term.Variable> list = new ArrayList<>();
        for (int i = 0; i < numParams; i++) {
            nars.term.Variable variable = $.varDep(prefix + i);
            list.add(variable);
        }
        Term[] x = list.toArray(new Term[0]);
        return x;
    }

    public static Term classTerm(Class c) {


        return Atomic.the(c.getSimpleName());


    }

    public static Term termClassInPackage( Class c) {
        return $.p(termPackage(c.getPackage()), classTerm(c));
    }

    
    public static Term termPackage( Package p) {


        String n = p.getName();

        String[] path = n.split("\\.");
        return $.p(path);


    }

    /**
     * generic instance target representation
     */
    public static Term instanceTerm(Object o) {
        return $.p(System.identityHashCode(o), 36);
    }

    protected @Nullable Term classInPackage(Term classs, @Deprecated Term packagge) {

        return null;
    }

    public Term[] terms(Object[] args) {
        return Util.map(this::term, Term[]::new, args);
    }

    @Override
    public @Nullable Term term(@Nullable Object o) {
        if (o == null) return NULL;
        else if (o instanceof Boolean) {
            return (Boolean) o ? TRUE_TERM : FALSE_TERM;
        } else if (o instanceof Number) {
            if (o instanceof Byte || o instanceof Short || o instanceof Integer || (o instanceof Long && Math.abs((Long) o) < Integer.MAX_VALUE - 1)) {
                return Int.the(((Number) o).intValue());
            } else if (o instanceof Float || o instanceof Double) {
                return $.the(((Number) o).doubleValue());
            } else if (o instanceof Long /* beyond an Int's capacity */) {
                return $.the(Long.toString((Long) o));
            } else {
                throw new TODO("support: " + o + " (" + o.getClass() + ')');
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

    public @Nullable Term obj2termCached(@Nullable Object o) {

        if (o == null)
            return NULL;
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
                if (ob != null) {
                    objToTerm.put(o, ob);
                    return ob;
                } else
                    return $.the("Object_" + System.identityHashCode(o));
            }
        } else {
            oe = obj2term(o);
        }

        return oe;
    }

    private static boolean cacheableInstance(Object o) {


        return true;
    }


    protected void onInstanceChange(Term oterm, Term prevOterm) {

    }

    protected void onInstanceOfClass(Object o, Term oterm, Term clas) {

    }

//    
//    public static <T extends Term> Map<Atomic,T> mapStaticClassFields( Class c,  Function<Field, T> each) {
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
