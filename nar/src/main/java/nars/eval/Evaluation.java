package nars.eval;

import jcog.data.iterator.CartesianIterator;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.VersionMap;
import nars.NAR;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.buffer.Termerator;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

public class Evaluation extends Termerator {

    private final Predicate<Term> each;

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

    @Deprecated private boolean termute(Evaluator e, Term y) {

        int before = v.size();

        if (termutes.size() == 1) {
            Iterable<Predicate<VersionMap<Term, Term>>> t = termutes.remove(0);
            for (Predicate tt : t) {
                if (tt.test(subs)) {
                    if (!recurse(e, y))
                        break;
                }
                v.revert(before);
            }
        } else {
            CartesianIterator<Predicate>/*<VersionMap<Term,Term>>>*/ ci =
                    new CartesianIterator(
                            Predicate[]::new, termutes.toArrayRecycled(Iterable[]::new));
            termutes.clear();
            nextProduct:
            while (ci.hasNext()) {

                v.revert(before);

                Predicate/*<VersionMap<Term,Term>>*/[] c = ci.next();

                for (Predicate<VersionMap<Term, Term>> cc : c) {
                    if (cc == null)
                        break; //null target list
                    if (!cc.test(subs))
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
        Term z = y.replace(subs);
        //recurse
        return z == y || (z instanceof Compound && eval(e, (Compound)z));  //CUT
    }


    private boolean eval(Evaluator e, final Compound x) {
        ArrayHashSet<Term> c = e.clauses(x, this);
        return eval(e, x, c != null ? c.list : null);
    }

    /**
     * fails fast if no known functors apply
     */
    protected boolean evalTry(Compound x, Evaluator e) {
        ArrayHashSet<Term> c = e.clauses(x, this);
        if ((c == null || c.isEmpty()) && (termutes == null || termutes.isEmpty())) {
            each.test(x);
            return true; //early exit
        }
        return eval(e, x, c != null ? c.list : null);
    }

    /**
     * simple complexity heuristic: sorting first by volume naively ensures innermost functors evaluated first
     */
    static private final Comparator<Term> byVolume = Comparator.comparingInt(Term::volume).thenComparingInt(Term::vars).thenComparingInt(Term::hashCode).thenComparing(System::identityHashCode);

    private boolean eval(Evaluator e, final Compound x, @Nullable List<Term> clauses) {

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

                    boolean remove = false;

                    Term af = Functor.func(a);
                    if (!(af instanceof Functor)) {
                        //try resolving
                        Term aa = e.apply(a);
                        if (aa == a) {
                            //no change. no such functor
                            remove = true;

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

                            if (!(y instanceof Compound && y.op().conceptualizable))
                                break main;

                        }


                        if (substing) {
                            y = y.replace(subs);

                            if (!(y instanceof Compound && y.op().conceptualizable))
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
                                     q = p.replace(subs);
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
        if (termutators() > 0)
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
        return termutes != null ? termutes.size() : 0;
    }

    private int now() {
        return v != null ? v.size() : 0;
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


    /**
     * returns first result. returns null if no solutions
     */
    public static Term solveFirst(Compound x, NAR n) {
        return solveFirst(x, n::axioms);
    }

    public static Set<Term> eval(Compound x, NAR n) {
        return eval(x, true, false, n);
    }

    public static Set<Term> eval(Compound x, boolean includeTrues, boolean includeFalses, NAR n) {
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






}
