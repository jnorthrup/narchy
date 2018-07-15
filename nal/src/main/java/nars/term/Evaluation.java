package nars.term;

import jcog.data.iterator.ArrayIterator;
import jcog.data.iterator.CartesianIterator;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.Operator;
import nars.subterm.Subterms;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.unify.match.EllipsisMatch;
import nars.util.term.transform.DirectTermTransform;
import nars.util.term.transform.TermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;

public class Evaluation {

    private static final Functor TRUE = new Functor.TheAbstractInlineFunctor1Inline("\"" + True + '"', x->null);

//    static final ThreadLocal<Evaluation> eval = ThreadLocal.withInitial(Evaluation::new);

    private List<Predicate<VersionMap<Term, Term>>[]> proc = null;

    private Versioning v;

    private VersionMap<Term, Term> subst;

    private boolean wrapBool = false;

    private Evaluation() {

    }

    private void ensureReady() {
        if (v == null) {
             v = new Versioning<>(16, 128);
             subst = new VersionMap<>(v);
             proc = new FasterList<>(1);
        }
    }


    private static Evaluation start(boolean wrapBool) {
        return new Evaluation().wrapBool(wrapBool);
    }

    private static ArrayHashSet<Term> solveAll(Evaluation e, Term x, NAR nar) {
        return solveAll(e, x, nar::functor, true);
    }

    public static ArrayHashSet<Term> solveAll(Term x, NAR nar) {
        return solveAll(null, x, nar);
    }

    private static final TermTransform trueUnwrapper = new TermTransform.NegObliviousTermTransform() {
        @Override
        public @Nullable Term transformCompoundUnneg(Compound x) {
            if (Functor.func(x).equals(TRUE)) {
                return transform(Operator.arg(x, 0));
            }
            return TermTransform.NegObliviousTermTransform.super.transformCompoundUnneg(x);
        }

        @Override
        public boolean eval() {
            return false;
        }
    };
    private static ArrayHashSet<Term> solveAll(Evaluation e, Term x, Function<Term, Functor> resolver, boolean wrapBool) {
        ArrayHashSet<Term> all = new ArrayHashSet<>(1);
        Evaluation.solve(e, x, wrapBool, resolver, (y) -> {
            y = (wrapBool && possiblyNeedsEval(y)) ? trueUnwrapper.transform(y) : y;
            all.add(y);
            return true;
        });
        return !all.isEmpty() ? all : ArrayHashSet.EMPTY;
    }

    public static boolean solve(@Nullable Evaluation e, Term x, boolean wrapBool, Function<Term,Functor> resolver, Predicate<Term> each) {
        Term y = needsEvaluation(x, resolver);
        if (y == null)
            return each.test(x);
        else
            return (e != null ? e.wrapBool(wrapBool) : Evaluation.start(wrapBool)).get(y, each);
    }

    @Nullable
    private static Term needsEvaluation(Term x, Function<Term,Functor> context) {

        if (!possiblyNeedsEval(x))
            return null;

        MyFunctorResolver ft = new MyFunctorResolver(context);
        Term y = ft.transform(x);

        if (y == Null) {
            return Null;
        }

        if (!ft.hasFunctor) {
            return null;
        }

        //TODO add flag if all functors involved are InlineFunctor

        return y;
    }

    public static boolean possiblyNeedsEval(Term x) {
        return x.hasAll(Op.FuncBits);
    }

    public static Term solveAny(Term x, Evaluation e, Function<Term,Functor> resolver, Random random, boolean wrapBool) {
        ArrayHashSet<Term> results = solveAll(e, x, resolver, wrapBool);
        return results.get(random);
    }

    private Term eval(Term x) {
        return boolWrap(x, _eval(x));
    }

    private Term boolWrap(Term x, Term y) {
        if (!(y instanceof Bool) || !wrapBool) {
            return y; //no change
        }

        if (y == Null)
            return Null;
        else {
            // if (y == True || y == False || y.hasAny(Op.BOOL)) {
            {
//                    boolean hasFalse = y == False; || y.ORrecurse(t -> t == False);
//            if (hasFalse)
//                z = False; //TODO maybe record what part causes the falsity

                //determined absolutely true or false: implies that this is the answer to a question
                return $.func(TRUE, y == False ? x.neg() : x);

                //return hasFalse ? False : True;
            }
        }
    }

    private Term _eval(Term c) {
        Op o = c.op();
        if (o == NEG) {
            Term xu = c.unneg();
            Term yu = _eval(xu);
            if (xu != yu)
                return yu.neg();
            return c; //unchanged
        }

        /*if (hasAll(opBits))*/

        /*if (subterms().hasAll(opBits))*/

        Subterms uu = c.subterms();
        Term[] xy = null;


        int ellipsisAdds = 0, ellipsisRemoves = 0;

        boolean evalConjOrImpl = !wrapBool && (o == CONJ || o == IMPL);
        int polarity = 0;

        for (int i = 0, n = uu.subs(); i < n; i++) {
            Term xi = xy != null ? xy[i] : uu.sub(i);
            Term yi = possiblyNeedsEval(xi) ? _eval(xi) : xi;
            if (yi == Null)
                return Null;
            if (evalConjOrImpl && yi instanceof Bool && (i == 0 /* impl subj only */ || o == CONJ)) {
                if (yi == True) {
                    polarity = +1;
                } else /*if (yi == False)*/ {
                    if (o == IMPL)
                        return Null;
                    polarity = -1;
                    break;
                }
            } else {
                if (xi != yi) {
                    yi = boolWrap(xi, yi);
                }
            }

            if (xi != yi) {
                if (yi == null) {

                } else {


                    if (yi instanceof EllipsisMatch) {
                        int ys = yi.subs();
                        ellipsisAdds += ys;
                        ellipsisRemoves++;
                    }

                    if (xi!=yi) {
                        if (xy == null) {
                            xy = c.arrayClone();
                        }
                        xy[i] = yi;
                    }
                }
            }
        }

        if (polarity != 0) {
            if (o == CONJ) {
                if (polarity < 0) {
                    return False; //short circuit
                }
            } else if (o == IMPL) {
                assert(polarity > 0); //False and Null already handled
                return xy[1];
            }
        }


        Term u;
        if (xy != null) {
            if (ellipsisAdds > 0) {

                xy = EllipsisMatch.flatten(xy, ellipsisAdds, ellipsisRemoves);
            }

            u = o.the(c.dt(), xy);
            o = u.op();
            uu = u.subterms();
        } else {
            u = c;
        }


        if (o == INH && uu.hasAll(Op.FuncInnerBits)) {
            Term pred, subj;
            if ((pred = uu.sub(1)) instanceof Functor && (subj = uu.sub(0)).op() == PROD) {

                Term v = ((BiFunction<Evaluation, Subterms, Term>) pred).apply(this, subj.subterms());
                if (v != null) {
                    if (v instanceof AbstractPred) {
                        u = $.the(((Predicate) v).test(null));
                    } else {
                        u = v;
                    }
                } /* else v == null, no change */
            }
        }

//        if (u != c && (u.equals(c) && u.getClass() == c.getClass()))
//            return c;

        return u;
    }

    private Evaluation wrapBool(boolean wrapBool) {
        this.wrapBool = wrapBool;
        return this;
    }

    private Evaluation clear() {
        if (v!=null) {
            proc.clear();
            v.reset();
            subst.clear();
        }
        return this;
    }

    private boolean get(Term _x, Predicate<Term> each) {

        Term x = eval(_x);

        int np = procs();
        if (np == 0)
            return each.test(x); //done

        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;

        Iterable[] aa = new Iterable[np];
        for (int i = 0; i < np; i++)
            aa[i] = ArrayIterator.iterable(proc.get(i));

        pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(Predicate[]::new, aa);

        int start = v.now();

        nextPermute:
        while (pp.hasNext()) {

            Term y;

            v.revert(start);

            Predicate<VersionMap<Term, Term>>[] n = pp.next();
            assert (n.length > 0);

            for (Predicate p : n) {
                if (!p.test(subst)) {
                    return each.test(False);
                    //continue nextPermute;
                }
            }

            y = x.replace(subst);
            if (y == null)
                continue;

            Term z = eval(y);

            int ps = procs();
            if (z != null && np == ps && !each.test(z))
                return false;

            if (np < ps) {
                int before = v.now();
                if (!get(z, each))
                    return false;
                v.revert(before);
            }

        }


        return true;
    }

    private int procs() {
        List<Predicate<VersionMap<Term, Term>>[]> p = this.proc;
        return p!=null ? p.size() : 0;
    }


    public void replace(Term x, Term xx) {
        replace(subst(x, xx));
    }

    public void replace(Term x, Term xx, Term y, Term yy) {
        replace(subst(x, xx, y, yy));
    }

    public void replace(Predicate... r) {
        ensureReady();
        proc.add(r);
    }

    private static Predicate<VersionMap<Term, Term>> subst(Term x, Term xx) {
        return (m) -> {
            Term px = m.get(x);
            if (px != null) {
                return px.equals(xx);
            } else {
                m.tryPut(x, xx);
                return true;
            }
        };
    }

    public static Predicate<VersionMap<Term, Term>> subst(Term x, Term xx, Term y, Term yy) {
        return (m) -> subst(x, xx).test(m) && subst(y, yy).test(m);
    }


    private static final class MyFunctorResolver implements DirectTermTransform {

        private final Function<Term, Functor> resolver;
        boolean hasFunctor;

        MyFunctorResolver(Function<Term,Functor> resolver) {
            this.resolver = resolver;
        }

        @Override
        public boolean eval() {
            return false; //not at this stage
        }

        @Override
        public @Nullable Term transformAtomic(Atomic x) {
            if (x instanceof Functor) {
                hasFunctor = true;
                return x;
            }

            if (x.op() == ATOM) {
                Functor f = resolver.apply(x);
                if (f!=null) {
                    hasFunctor = true;
                    return f;
                }
            }
            return x;
        }

    }
}
