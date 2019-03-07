package nars.eval;

import com.google.common.collect.Iterables;
import jcog.data.iterator.CartesianIterator;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

public class Evaluation {

    private final Predicate<Term> each;

    transient private FasterList<Iterable<Predicate<VersionMap<Term, Term>>>> termutator = null;

    transient private Versioning v = null;

    protected transient VersionMap<Term, Term> subst = null;


    private static void eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver, Predicate<Term> each) {

        if (canEval(x)) {
            Evaluator y = new Evaluator(resolver);
            y.eval(each, includeTrues, includeFalses, x);
        } else {
            each.test(x); //didnt need evaluating, just input
        }
    }

    protected Evaluation() {
        this.each = (Predicate<Term>) this;
    }

    public Evaluation(Predicate<Term> each) {
        this.each = each;
    }

    private boolean termute(Evaluator e, Term y) {

        int before = v.size();

        if (termutator.size() == 1) {
            Iterable<Predicate<VersionMap<Term, Term>>> t = termutator.remove(0);
            for (Predicate tt : t) {
                if (tt.test(subst)) {
                    if (!recurse(e, y))
                        break;
                }
                v.revert(before);
            }
        } else {
            CartesianIterator<Predicate>/*<VersionMap<Term,Term>>>*/ ci =
                    new CartesianIterator(
                            Predicate[]::new, termutator.toArrayRecycled(Iterable[]::new));
            termutator.clear();
            nextProduct:
            while (ci.hasNext()) {

                v.revert(before);

                Predicate/*<VersionMap<Term,Term>>*/[] c = ci.next();

                for (Predicate<VersionMap<Term, Term>> cc : c) {
                    if (cc == null)
                        break; //null target list
                    if (!cc.test(subst))
                        continue nextProduct;
                }

                //all components applied successfully

                if (!recurse(e, y))
                    return false;

            }
        }
        return true;
    }

    private boolean recurse(Evaluator e, Term y) {
        Term z = y.replace(subst);
        if (z != y && !eval(e, z)) //recurse
            return false;  //CUT
        return true;
    }


    private boolean eval(Evaluator e, final Term x) {
        ArrayHashSet<Term> c = e.clauses(x, this);
        return eval(e, x, c != null ? c.list : null);
    }

    /**
     * fails fast if no known functors apply
     */
    public boolean evalTry(Term x, Evaluator e) {
        ArrayHashSet<Term> c = e.clauses(x, this);
        if ((c == null || c.isEmpty()) && (termutator == null || termutator.isEmpty())) {
            each.test(x);
            return true; //early exit
        }
        return eval(e, x, c != null ? c.list : null);
    }

    /**
     * simple complexity heuristic: sorting first by volume naively ensures innermost functors evaluated first
     */
    static private final Comparator<Term> byVolume = Comparator.comparingInt(Term::volume).thenComparing(Term::vars);

    private boolean eval(Evaluator e, final Term x, @Nullable List<Term> clauses) {

        Term y = x;

        if (clauses != null && !clauses.isEmpty()) {

            //TODO topologically sort operations according to variable dependencies; it acts like an evaluation plan so ordering can help */
            if (clauses.size() > 1) {
                ((FasterList<Term>) clauses).sortThis(byVolume);
            }

            Term prev;
            int vStart, tried, mutStart;
            //iterate until stable
            main:
            do {
                prev = y;
                mutStart = termutators();
                tried = 0;
                Iterator<Term> ii = clauses.iterator();
                while (ii.hasNext()) {
                    vStart = now();

                    Term a = ii.next();


                    //still a functor, but different. update it

                    //run the functor resolver for any new functor terms which may have appeared

                    boolean remove = false, eval = true;

                    Term af = Functor.func(a);
                    if (!(af instanceof Functor)) {
                        //try resolving
                        Term aa = e.apply(a);
                        if (aa == a) {
                            //no change. no such functor
                            remove = true;
                            eval = false;
                        } else {
                            a = aa;
                        }
                    }

                    Term z;
                    boolean substing, mutAdded;

                    Functor func = (Functor) a.sub(1);
                    Subterms args = a.sub(0).subterms();
//                        if (canEval(args)) {
//                            //evaluate subterms recursively
//                            args = args.transformSubs(new DirectTermTransform() {
//                                @Override
//                                public Term transform(Term x) {
//                                    return Evaluation.solveFirst(x, (Function<Atom, Functor>) e.funcResolver);
//                                }
//                            }, PROD);
//                        }

                    z = func.apply(this, args);
                    if (z == Null) {
                        ii.remove(); //CUT
                        y = Null;
                        break main;
                    }
                    substing = now() != vStart;
                    mutAdded = mutStart != termutators();
                    if ((z == null && (substing || mutAdded)) || (z != null && z != a)) { //(z instanceof Bool)) {
                        remove = true;
                    }


                    if (remove) {
                        ii.remove();
                    }


                    if (substing || (z != null && z != a)) {
                        tried++;

                        Term y0 = y;
                        if (z != null) {
                            y = y.replace(a, z); //TODO replace only the first?

                            if (!y.op().conceptualizable)
                                break main;
                        }


                        if (substing) {
                            y = y.replace(subst);

                            if (!y.op().conceptualizable)
                                break main;
                        }

                        if (clauses == null || clauses.isEmpty())
                            break main;

                         if (z != null || substing) {
                             Term finalA = a;
                             clauses.replaceAll(o -> {
                                 Term p;
                                 if (z != null) {
                                     p = o.replace(finalA, z);
                                 } else
                                     p = o;

                                 Term q;
                                 if (substing) {
                                     q = p.replace(subst);
                                 } else
                                     q = p;

                                 if (o != q && !Functor.isFunc(q))
                                     return Null;
                                 else
                                     return q;
                             });

                             if (clauses.removeIf(xxx -> xxx == Null) && clauses.isEmpty())
                                 break main;
                         }

                        break; //changed so start again
                    }

                }

            } while ((y != prev) || (tried > 0));
        }

        assert (y != null);

        if (y instanceof Bool)
            return each.test(bool(x, (Bool) y)); //Terminal Result

        //if termutators, collect all results. otherwise 'cur' is the only result to return
        int ts = termutators();
        if (ts > 0)
            return termute(e, y);
        else
            return each.test(y); //Transformed Result (possibly same)
    }

    protected Term bool(Term x, Bool b) {
        if (b == Bool.True) {
            return boolTrue(x);
        } else if (b == Bool.False) {
            return boolFalse(x);
        } else {
            return Null;
        }
    }

    protected Term boolTrue(Term x) {
        return x;
    }

    protected Term boolFalse(Term x) {
        return x.neg();
    }

    private int termutators() {
        return termutator != null ? termutator.size() : 0;
    }

    private int now() {
        return v != null ? v.size() : 0;
    }

    /**
     * returns first result. returns null if no solutions
     */
    public static Term solveFirst(Term x, NAR n) {
        return solveFirst(x, n::axioms);
    }

    private static Term solveFirst(Term x, Function<Atom, Functor> axioms) {
        Term[] y = new Term[1];
        Evaluation.eval(x, true, true, axioms, (what) -> {
            if (what instanceof Bool) {
                if (y[0] != null)
                    return true; //ignore and continue try to find a non-bool solution
            }
            y[0] = what;
            return false;
        });
        return y[0];
    }

    //    public static Set<Term> eval(String s, NAR n) {
//        return eval($$(s), n);
//    }

    public static Set<Term> eval(Term x, NAR n) {
        return eval(x, true, false, n);
    }

    public static Set<Term> eval(Term x, boolean includeTrues, boolean includeFalses, NAR n) {
        return eval(x, includeTrues, includeFalses, n::axioms);
    }

    private static final class MyEvaluated extends UnifiedSet<Term> implements Predicate<Term> {
        protected MyEvaluated() {
            super(1, 0.99f);
        }

        @Override
        public boolean test(Term y) {
            if (y != Null)
                add(y);
            return true;
        }
    }

    /**
     * gathers results from one truth setAt, ex: +1 (true)
     * TODO add limit
     */
    public static Set<Term> eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver) {
        MyEvaluated ee = new MyEvaluated();

        Evaluation.eval(x, includeTrues, includeFalses, resolver, ee);

        if (ee.isEmpty()) {
            //java.util.Set.of($.func(Inperience.wonder, x))
            return Set.of();
        } else
            return ee;
    }


    private void ensureReady() {
        if (v == null) {
            v = new Versioning<>(Param.UnificationStackMax, Param.TASK_EVALUATION_TTL);
            subst = new VersionMap<>(v);
            termutator = new FasterList(1);
        }
    }


    private Evaluation clear() {
        if (v != null) {
            termutator.clear();
            v.clear();
            subst.clear();
        }
        return this;
    }


    /**
     * assign 1 variable
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term y) {
        if (x.equals(y))
            return true;
        else {
            ensureReady();
            return subst.set(x, y);
        }

    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
        return is(x, xx) && is(y, yy);
    }

    /**
     * 2-ary AND
     */
    public static Predicate<VersionMap<Term, Term>> assign(Term x, Term xx, Term y, Term yy) {
        return m -> m.set(x, xx) && m.set(y, yy);
    }

    protected static Predicate<VersionMap<Term, Term>> assign(Term x, Term y) {
        return (subst) -> subst.set(x, y);
    }


    /**
     * OR, forked
     * TODO limit when # termutators exceed limit
     */
    public void canBe(Iterable<Predicate<VersionMap<Term, Term>>> x) {
        ensureReady();
        termutator.add(x);
    }

    public void canBe(Predicate<VersionMap<Term, Term>> x) {
        ensureReady();
        termutator.add(List.of(x));
    }

    public void canBe(Term x, Term y) {
        if (!x.equals(y))
            canBe(assign(x, y));
    }

    public void canBe(Term x, Collection<Term> y) {
        if (!y.isEmpty()) {
            canBe(x, (Iterable)y);
        }
    }
    public void canBePairs(List<Term> y) {
        canBe((Predicate<VersionMap<Term,Term>>)(VersionMap<Term,Term> e)->{
            int n = y.size();
            for (int i = 0; i < n; ) {
                if (!e.set(y.get(i++), y.get(i++)))
                    return false;
            }
            return true;
        });
    }

    public void canBe(Term x, Iterable<Term> y) {
        canBe((Iterable<Predicate<VersionMap<Term, Term>>>) Iterables.transform(y, yy -> assign(x, yy)));
    }

    public void canBe(Term a, Term b, Term x, Term y) {
        if (x.equals(y)) {
            canBe(a, b);
        } else if (a.equals(b)) {
            canBe(x, y);
        } else {
            canBe(assign(a, b, x, y));
        }
    }

    public static boolean canEval(Termlike x) {
        return x.hasAll(Op.FuncBits);
    }


}
