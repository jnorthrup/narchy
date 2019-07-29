package nars.eval;

import jcog.data.iterator.CartesianIterator;
import jcog.data.set.ArrayHashSet;
import jcog.math.ShuffledPermutations;
import jcog.util.ArrayUtil;
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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.term.atom.Bool.*;

/** see: https://www.swi-prolog.org/pldoc/man?section=preddesc */
public class Evaluation extends Termerator {

    private final Predicate<Term> each;

    public static void eval(Term x, Function<Atom, Functor> resolver, Predicate<Term> each) {
        if (evalable(x))
            new Evaluation(each).evalTry((Compound)x, new Evaluator(resolver), false);
    }

    private static void eval(Term x, boolean includeTrues, boolean includeFalses, Function<Atom, Functor> resolver, Predicate<Term> each) {

        if (evalable(x)) {
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
            return termute1(e, y, before);
        } else {
            return termuteN(e, y, before);
        }
    }

    protected Random random() {
        return ThreadLocalRandom.current();
    }

    private boolean termuteN(Evaluator e, Term y, int start) {

        Iterable<Predicate<Termerator>>[] tt = termutes.toArrayRecycled(Iterable[]::new);
        for (int i = 0, ttLength = tt.length; i < ttLength; i++)
            tt[i] = shuffle(tt[i]);
        ArrayUtil.shuffle(tt, random());

        CartesianIterator<Predicate<Termerator>> ci = new CartesianIterator(Predicate[]::new, tt);

        termutes.clear();

        nextProduct: while (ci.hasNext()) {

            v.revert(start);

            Predicate/*<VersionMap<Term,Term>>*/[] c = ci.next();

            for (Predicate<Termerator> cc : c) {
//                if (cc == null)
//                    break; //null target list
                if (!cc.test(this))
                    continue nextProduct;
            }

            //all components applied successfully

            if (!recurse(e, y))
                return false;

        }
        return true;
    }


    private boolean termute1(Evaluator e, Term y, int start) {
        Iterable<Predicate<Termerator>> t = shuffle(termutes.remove(0));
        for (Predicate<Termerator> tt : t) {
            if (tt.test(this)) {
                if (!recurse(e, y))
                    return false;
            }
            v.revert(start);
        }
        return true;
    }

    private Iterable<Predicate<Termerator>> shuffle(Iterable<Predicate<Termerator>> t) {
        return ShuffledPermutations.shuffle(t, random());
    }

    private boolean recurse(Evaluator e, Term y) {
        Term z = y.replace(subs);
        return y.equals(z) || eval(e, z);  //CUT
    }


    public boolean eval(Evaluator e, final Term x) {
        return eval(e, x, x instanceof Compound ? e.clauses((Compound)x, this) : null);
    }

    /**
     * fails fast if no known functors apply
     */
    protected boolean evalTry(Compound x, Evaluator e, boolean includeOriginal) {
        @Nullable ArrayHashSet<Term> c = e.clauses(x, this);

        if ((c == null || c.isEmpty()) && (termutes == null || termutes.isEmpty())) {
            if (includeOriginal)
                each.test(x);
            return true;
        }

        return eval(e, x, c);
    }

    private boolean eval(Evaluator e, final Term x, @Nullable ArrayHashSet<Term> clauses) {

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

                        if (clauses.isEmpty())
                            break main;


                        int clausesRemain = clauses.size();
                        for (int i = 0; i < clausesRemain; i++) {
                            Term ci = clauses.get(i);
                            Term p;
                            if (z != null)
                                p = ci.replace(a, z);
                            else
                                p = ci;

                            Term q = substing ? p.replace(subs) : p;

                            if (ci != q) {

                                if (q instanceof Compound) {
                                    @Nullable ArrayHashSet<Term> qq = e.clauseFind((Compound) q);
                                    if (qq!=null) {
                                        //merge new sub-clauses into the clause queue
                                        for (Term qqq : qq) {
                                            if (!qqq.equals(a)) {
                                                if (clauses.add(qqq))
                                                    clausesRemain++;
                                            }
                                        }
                                    }
                                }

//                                     if (q==Null /*|| !Functor.isFunc(q)*/) {
                                   clauses.remove(ci);
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



    public static Term solveFirst(Term x, Function<Atom, Functor> axioms) {
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
