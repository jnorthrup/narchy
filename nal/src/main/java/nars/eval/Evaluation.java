package nars.eval;

import jcog.data.iterator.CartesianIterator;
import jcog.data.list.FasterList;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;
import static nars.Op.*;

public class Evaluation {

    private final Predicate<Term> each;
    private final Evaluator e;

    private FasterList<Iterable<Predicate<VersionMap<Term, Term>>>> termutator = null;

    private Versioning v;

    private VersionMap<Term, Term> subst;

    ///ArrayHashSet<Term> seen = new ArrayHashSet<>(1); //deduplicator,TODO use different deduplication strategies including lossy ones (Bagutator)

    @Nullable
    public static Evaluation eval(Term x, NAR nar, Predicate<Term> each) {
        return eval(x, nar::functor,
                //nar.facts(0.75f, true),
                null,
                each);
    }

    @Nullable
    public static Evaluation answer(Term x, NAR nar, Predicate<Term> each) {
        return eval(x, nar::functor,
                nar.facts(0.75f, true),
                each);
    }

    /**
     *
     * @param x
     * @param resolver
     * @param facts
     * @param each
     * @return
     */
    @Nullable public static Evaluation eval(Term x, Function<Atom, Functor> resolver, @Nullable Function<Term,Stream<Term>> facts, Predicate<Term> each) {

        if (canEval(x)) {
            Evaluator y = facts==null ? new Evaluator(resolver) : new FactualEvaluator(resolver, facts);
            return y.eval(each, x);
        }

        each.test(x); //didnt need evaluating, just input
        return null;
    }

    protected Evaluation(Evaluator e, Predicate<Term> each) {
        this.each = each;
        this.e = e;
    }

    private boolean termute(Term y, Predicate<Term> each) {


        int before = v.now();

        if (termutator.size() == 1) {
            Iterable<Predicate<VersionMap<Term, Term>>> t = termutator.get(0);
            termutator.clear();
            for (Predicate tt : t) {
                if (tt.test(subst)) {
                    Term z = y.replace(subst);
                    //if (z!=y) {
                        if (!each.test(z)) { //TODO check for new solvable sub-components
                            return false;
                        }
                    //}
                }
                v.revert(before);
            }
        } else {
            CartesianIterator<Predicate<VersionMap<Term,Term>>> ci = new CartesianIterator<>(Predicate[]::new, termutator.toArray(Iterable[]::new));
            termutator.clear();
            nextProduct: while (ci.hasNext()) {

                v.revert(before);

                Predicate<VersionMap<Term,Term>>[] c = ci.next();

                for (Predicate<VersionMap<Term,Term>> cc : c) {
                    if (!cc.test(subst))
                        break nextProduct;
                }

                //all components applied successfully

                Term z = y.replace(subst);
                if (z!=y) {
                    if (canEval(z)) { // && !(ez = e.clone().query(z)).isEmpty()) {
                        Evaluator ee = e.clone();
                        ee.query(z);
                        if (!ee.isEmpty() && !eval(ee, z)) //recurse
                            return false; //CUT
                    } else {
                        if (!each.test(z))
                            return false; //CUT
                    }

                }
            }
        }
        return true;
    }


    protected boolean eval(Evaluator e, Term x) {
        //iterate until stable
        Term y = x, prev;
        int vStart, tried, mutStart;
        main:
        do {
            prev = y;
            ListIterator<Term> ii = e.listIterator();
            vStart = now();
            mutStart = termutators();
            tried = 0;
            while (ii.hasNext()) {

                Term a = ii.next();

                boolean removeEntry, eval;

                if (Functor.isFunc(a)) {

                    //still a functor, but different. update it

                    //run the functor resolver for any new functor terms which may have appeared
                    Term af = Functor.func(a);

                    removeEntry = false;

                    eval = true;

                    if (!(af instanceof Functor)) {
                        //try resolving
                        Term aa = e.transform(a);
                        if (aa == a) {
                            //no change. no such functor
                            removeEntry = true;
                            eval = false;
                        } else {
                            a = aa;
                        }
                    }

                } else {
                    eval = false;
                    removeEntry = true;
                }

                Term z;
                boolean substAdded, mutAdded;
                if (!removeEntry && eval) {
                    Functor func = (Functor) a.sub(1);
                    Subterms args = a.sub(0).subterms();

                    z = func.apply(this, args);
                    if (z == Null) {
                        return each.test(Null);
                    }
                    substAdded = now() != vStart;
                    mutAdded = mutStart != termutators();
                    if ((z == null && (substAdded||mutAdded)) || (z == True)) {
                        removeEntry = true;
                    }

                } else {
                    substAdded = mutAdded = false;
                    z = a;
                }

                if (removeEntry) {
                    ii.remove();
                }


                if ((z != null && z != a) || substAdded) {
                    tried++;


                    if (z != null) {
                        y = y.replace(a, z);
                    }
                    if (substAdded) {
                        y = y.replace(subst);
                    }

                    //pendingRewrites.add(new Term[]{a, z});
                    Term finalA = a;
                    e.list.replaceAll(o -> {
                        Term p, q;
                        if (z != null) {
                            p = o.replace(finalA, z);
                            if (o != p && !Functor.isFunc(p))
                                return Null;
                        } else
                            p = o;

                        if (substAdded) {
                            q = p.replace(subst);
                            if (p != q && !Functor.isFunc(q))
                                return Null;
                        } else
                            q = p;

                        return q;
                    });


                    if (!y.op().conceptualizable || e.isEmpty())
                        break main;
                    else {
                        break; //changed so start again
                    }

                }



            }


        } while ((y != prev) || (tried > 0));

        assert (y != null);

        //if termutators, collect all results. otherwise 'cur' is the only result to return
        int ts = termutators();
        if (ts > 0) {
            return termute(y, each);
        } else {
            if (y!=Null) {
                if (!each.test(y))
                    return false;
            }
        }
        return true;
    }

    private int termutators() {
        return termutator!=null ? termutator.size() : 0;
    }

    private int now() {
        return v!=null ? v.now() : -1;
    }

//    private Term rewrite(List<Term[]> rewrites, Term x) {
//        if (rewrites.isEmpty())
//            return x;
//        Term y = x;
//        //TODO compile these into one replacement transform
//        for (Term[] rw : rewrites)
//            y = y.replace(rw[0], rw[1]).replace(subst);
//        return y;
//    }


    /**
     * returns first result. returns null if no solutions
     */
    public static Term solveFirst(Term x, NAR n) {
        Term[] y = new Term[1];
        Evaluation.eval(x, n, (what) -> {
            if (what instanceof Bool) {
                if (y[0]!=null)
                    return true; //ignore and continue try to find a non-bool solution
            }
            y[0] = what;
            return false;
        });
        return y[0];
    }


    public static Set<Term> answerAll(String s, NAR n) {
        return answerAll($$(s), n);
    }

    /**
     * gathers results from one truth set, ex: +1 (true)
     */
    public static Set<Term> answerAll(Term x, NAR n) {
        final Set[] yy = {null};
        Evaluation.answer(x, n, (y) -> {
            if (yy[0] == null) {
                yy[0] = new UnifiedSet<>(1);
            }
            //TODO extract this to a 'wrap' method
            if (y instanceof Bool) {
                if (y == True)
                    yy[0].add(x);
                else if (y == False) {
                    yy[0].add(x.neg());
                } else {
                    //Null, but continue..
                }
            } else {
                yy[0].add(y);
            }
            return true;
        });

        Set z = yy[0];
        return z == null ? java.util.Set.of(x) : yy[0];
    }


    private void ensureReady() {
        if (v == null) {
            v = new Versioning<>(Param.UnificationStackMax, Param.EVALUATION_TTL);
            subst = new VersionMap<>(v);
            termutator = new FasterList(1);
        }
    }


    private Evaluation clear() {
        if (v != null) {
            termutator.clear();
            v.reset();
            subst.clear();
        }
        return this;
    }



    /**
     * assign 1 variable
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term y) {
        ensureReady();
        return subst.tryPut(x, y);
    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
        ensureReady();
        return subst.tryPut(x, xx) && subst.tryPut(y, yy);
    }

    /**
     * 2-ary AND
     */
    public Predicate<VersionMap<Term, Term>> assign(Term x, Term xx, Term y, Term yy) {
        return m -> m.tryPut(x, xx) && m.tryPut(y, yy);
    }


    /**
     * OR, forked
     * TODO limit when # termutators exceed limit
     */
    public void isAny(Collection<Predicate<VersionMap<Term, Term>>> r) {
        ensureReady();
        termutator.add(r);
    }

    public static Predicate<VersionMap<Term, Term>> assign(Term x, Term y) {
        return (subst) -> subst.tryPut(x, y);
    }


    private static boolean canEval(Term x) {
        return x.hasAll(Op.FuncBits);
    }


}
