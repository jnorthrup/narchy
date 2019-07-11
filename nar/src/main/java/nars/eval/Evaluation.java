package nars.eval;

import jcog.data.iterator.CartesianIterator;
import jcog.data.list.FasterList;
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

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.*;

/** see: https://www.swi-prolog.org/pldoc/man?section=preddesc */
public class Evaluation extends Termerator {

    private final Predicate<Term> each;

    private static void eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver, Predicate<Term> each) {

        if (canEval(x)) {
            new Evaluator(resolver).eval(each, includeTrues, includeFalses, x);
        } else {
            each.test(x); //didnt need evaluating, just input
        }
    }

    protected Evaluation() {
        this.each = (Predicate<Term>) this;
    }

    Evaluation(Predicate<Term> each) {
        this.each = each;
    }

    @Deprecated private boolean termute(Evaluator e, Term y) {

        int before = v.size();

        if (termutes.size() == 1) {
            Iterable<Predicate<Termerator>> t = termutes.remove(0);
            for (Predicate<Termerator> tt : t) {
                if (tt.test(this)) {
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

                for (Predicate<Termerator> cc : c) {
                    if (cc == null)
                        break; //null target list
                    if (!cc.test(this))
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
        return eval(e, x, e.clauses(x, this));
    }

    /**
     * fails fast if no known functors apply
     */
    protected boolean evalTry(Compound x, Evaluator e, boolean includeOriginal) {
        FasterList<Term> c = e.clauses(x, this);

        if ((c == null || c.isEmpty()) && (termutes == null || termutes.isEmpty())) {
            if (includeOriginal)
                each.test(x);
            return true;
        }

        return eval(e, x, c);
    }

    private boolean eval(Evaluator e, final Compound x, @Nullable FasterList<Term> clauses) {

        Term y = x;

        if (clauses != null && !clauses.isEmpty()) {

            Term prev;
            int vStart, mods, mutStart;
            //iterate until stable
            main:
            do {
                prev = y;
                mutStart = termutators();
                mods = 0;
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
                    if (z == Null || z == False) {
                        ii.remove(); //CUT
                        y = z;
                        break main;
                    }
                    if (z == True) {
                        remove = true;
                    }

                    substing = now() != vStart;
                    mutAdded = mutStart != termutators();
                    if ((z == null && (substing || mutAdded)) || (z != null && z != a)) { //(z instanceof Bool)) {
                        remove = true;
                    }


                    if (remove) {
                        ii.remove();
                    }


                    if (substing || (z != null && z != a )) {
                        mods++;

                        Term y0 = y;
                        if (z != null) {
                            y = y.replace(a, z); //TODO replace only the first?

                            if (!y.op().conceptualizable)
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


                             int clausesRemain = clauses.size();
                             for (int i = 0, clausesSize = clausesRemain; i < clausesSize; i++) {
                                 Term o = clauses.get(i);
                                 Term p;
                                 if (z != null)
                                     p = o.replace(a, z);
                                 else
                                     p = o;

                                 Term q;
                                 if (substing)
                                     q = p.replace(subs);
                                 else
                                     q = p;

                                 if (o != q) {

                                     if (q instanceof Compound) {
                                         @Nullable FasterList<Term> qq = e.clauseFind((Compound) q);
                                         if (!qq.isEmpty()) {
                                             //merge new sub-clauses into the clause queue
                                             for (Term qqq : qq) {
                                                 if (!qqq.equals(a)) {
                                                     if (clauses.addIfNotPresent(qqq))
                                                         clausesRemain++;
                                                 }
                                             }
                                         }
                                     }

//                                     if (q==Null /*|| !Functor.isFunc(q)*/) {
                                        clauses.setFast(i, null);
                                        clausesRemain--;
//                                     } else {
//
//                                         clauses.setFast(i, q);
//
//
//                                     }
                                 }


                             }
                             if (clausesRemain == 0)
                                 break main;

                             clauses.removeNulls();
                         }

                        break; //changed so start again
                    }

                }

            } while ((!y.equals(prev)) || (mods > 0));
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
        if (b == True) {
            return boolTrue(x);
        } else if (b == False) {
            return boolFalse(x);
        } else {
            return Null;
        }
    }

    Term boolTrue(Term x) {
        return x;
    }

    Term boolFalse(Term x) {
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

    @Deprecated public static Set<Term> eval(Compound x, NAR n) {
        return eval(x, true, false, n);
    }

    @Deprecated public static Set<Term> eval(Compound x, boolean includeTrues, boolean includeFalses, NAR n) {
        return eval(x, includeTrues, includeFalses, n::axioms);
    }

    /**
     * gathers results from one truth setAt, ex: +1 (true)
     * TODO add limit
     */
    public static Set<Term> eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver) {

        UnifiedSet ee = new UnifiedSet(0, 0.5f);

        Evaluation.eval(x, includeTrues, includeFalses, resolver, t -> {
            if (t != Null)
                ee.add(t);
            return true;
        });

        if (ee.isEmpty()) {
            //java.util.Set.of($.func(Inperience.wonder, x))
            return Set.of();
        } else
            return ee;
    }






}
