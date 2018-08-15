package nars.term;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.term.atom.Atomic;
import nars.term.util.transform.DirectTermTransform;
import org.eclipse.collections.api.block.predicate.primitive.ObjectBytePredicate;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.ATOM;

public class Evaluation {

    private final FunctorSet functors;
    private final ObjectBytePredicate<Term> each;
    private final Term term;
    private List<Iterable<Predicate<VersionMap<Term, Term>>>> termutator = null;

    private Versioning v;

    private VersionMap<Term, Term> subst;

    ///ArrayHashSet<Term> seen = new ArrayHashSet<>(1); //deduplicator,TODO use different deduplication strategies including lossy ones (Bagutator)

    @Nullable public static Evaluation eval(Term x, NAR nar, ObjectBytePredicate<Term> each) {
        return eval(x, nar::functor, each);
    }

    @Nullable public static Evaluation eval(Term x, Function<Term, Functor> resolver, ObjectBytePredicate<Term> each) {
        if (possiblyNeedsEval(x)) {
            FunctorSet y = new FunctorSet(resolver).discover(x);
            if (!y.isEmpty())
                return new Evaluation(x, y, each);
        }
        return null;
    }

    private Evaluation(Term x, FunctorSet f, ObjectBytePredicate<Term> each) {
        this.term = x;
        this.each = each;
        this.functors = f;
        f.sortDecreasingVolume();
    }

    /** returns first true result. returns null if no solutions */
    public static Term solveFirst(Term x, NAR n) {
        Term[] y = new Term[1];
        Evaluation.eval(x, n, (what, truth) -> {
            if (truth == +1) {
                y[0] = what;
            }
            return true;
        });
        return y[0];
    }


    public static ArrayHashSet<Term> solveAllTrue(Term x, NAR n) {
        return solveAll(x, (byte)1, n);
    }

    /** gathers results from one truth set, ex: +1 (true) */
    public static ArrayHashSet<Term> solveAll(Term x, byte truthSelect, NAR n) {
        ArrayHashSet<Term> y = new ArrayHashSet();
        Evaluation.eval(x, n, (what, truth) -> {
            if (truth == truthSelect) {
                y.add(what);
            }
            return true;
        });
        return y;
    }


    private void ensureReady() {
        if (v == null) {
            v = new Versioning<>(Param.UnificationStackMax, Param.EVALUATION_TTL);
            subst = new VersionMap<>(v);
            termutator = new FasterList<>(1);
        }
    }








//    private Term _eval(Term c) {
//        Op o = c.op();
//        if (o == NEG) {
//            Term xu = c.unneg();
//            Term yu = _eval(xu);
//            if (xu != yu)
//                return yu.neg();
//            return c; //unchanged
//        }
//
//        /*if (hasAll(opBits))*/
//
//        /*if (subterms().hasAll(opBits))*/
//
//        Subterms uu = c.subterms();
//        Term[] xy = null;
//
//
//        int ellipsisAdds = 0, ellipsisRemoves = 0;
//
//        boolean evalConjOrImpl = !wrapBool && (o == CONJ || o == IMPL);
//        int polarity = 0;
//
//        for (int i = 0, n = uu.subs(); i < n; i++) {
//            Term xi = xy != null ? xy[i] : uu.sub(i);
//            Term yi = possiblyNeedsEval(xi) ? _eval(xi) : xi;
//            if (yi == Null)
//                return Null;
//            if (evalConjOrImpl && yi instanceof Bool && (i == 0 /* impl subj only */ || o == CONJ)) {
//                if (yi == True) {
//                    polarity = +1;
//                } else /*if (yi == False)*/ {
//                    if (o == IMPL)
//                        return Null;
//                    polarity = -1;
//                    break;
//                }
//            } else {
//                if (xi != yi) {
//                    yi = boolWrap(xi, yi);
//                }
//            }
//
//            if (xi != yi) {
//                if (yi == null) {
//
//                } else {
//
//
//                    if (yi instanceof EllipsisMatch) {
//                        int ys = yi.subs();
//                        ellipsisAdds += ys;
//                        ellipsisRemoves++;
//                    }
//
//                    if (xi != yi) {
//                        if (xy == null) {
//                            xy = c.arrayClone();
//                        }
//                        xy[i] = yi;
//                    }
//                }
//            }
//        }
//
//        if (polarity != 0) {
//            if (o == CONJ) {
//                if (polarity < 0) {
//                    return False; //short circuit
//                }
//            } else if (o == IMPL) {
//                assert (polarity > 0); //False and Null already handled
//                return xy[1];
//            }
//        }
//
//
//        Term u;
//        if (xy != null) {
//            if (ellipsisAdds > 0) {
//
//                xy = EllipsisMatch.flatten(xy, ellipsisAdds, ellipsisRemoves);
//            }
//
//            u = o.the(c.dt(), xy);
//            o = u.op();
//            uu = u.subterms();
//        } else {
//            u = c;
//        }
//
//
//        if (o == INH && uu.hasAll(Op.FuncInnerBits)) {
//            Term pred, subj;
//            if ((pred = uu.sub(1)) instanceof Functor && (subj = uu.sub(0)).op() == PROD) {
//
//                Term v = ((BiFunction<Evaluation, Subterms, Term>) pred).apply(this, subj.subterms());
//                if (v != null) {
//                    if (v instanceof AbstractPred) {
//                        u = $.the(((Predicate) v).test(null));
//                    } else {
//                        u = v;
//                    }
//                } /* else v == null, no change */
//            }
//        }
//
////        if (u != c && (u.equals(c) && u.getClass() == c.getClass()))
////            return c;
//
//        return u;
//    }

    private Evaluation clear() {
        if (v != null) {
            termutator.clear();
            v.reset();
            subst.clear();
        }
        return this;
    }

//    private boolean get(Term _x, Predicate<Term> each) {
//
//        Term x = eval(_x);
//
//        int np = procs();
//        if (np == 0)
//            return each.test(x); //done
//
//        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;
//
//        Iterable[] aa = new Iterable[np];
//        for (int i = 0; i < np; i++)
//            aa[i] = ArrayIterator.iterable(termutator.get(i));
//
//        pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(Predicate[]::new, aa);
//
//        int start = v.now();
//
//        nextPermute:
//        while (pp.hasNext()) {
//
//
//
//            v.revert(start);
//
//            Predicate<VersionMap<Term, Term>>[] n = pp.next();
//            assert (n.length > 0);
//
//            for (Predicate p : n) {
//                if (!p.test(subst)) {
//                    return each.test(False);
//                    //continue nextPermute;
//                }
//            }
//
//            Term y = x.replace(subst);
//            if (y == null)
//                continue;
//
//            Term z = eval(y);
//
//            int ps = procs();
//            if (z != null && np == ps && !each.test(z))
//                return false;
//
//            if (np < ps) {
//                int before = v.now();
//                if (!get(z, each))
//                    return false;
//                v.revert(before);
//            }
//
//        }
//
//
//        return true;
//    }
//


    /**
     * assign 1 variable
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term y) {
        return subst.tryPut(x, y);
    }

    /**
     * assign 2-variables at once.
     * returns false if it could not be assigned (enabling callee fast-fail)
     */
    public boolean is(Term x, Term xx, Term y, Term yy) {
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
     */
    public void isAny(Collection<Predicate<VersionMap<Term, Term>>> r) {
        ensureReady();
        termutator.add(r);
    }

    public static Predicate<VersionMap<Term,Term>> assign(Term x, Term y) {
        return (subst) -> subst.tryPut(x, y);
    }




    private static boolean possiblyNeedsEval(Term x) {
        return x.hasAll(Op.FuncBits);
    }



    /** discovers functors within the provided term, or the term itself.
     * transformation results should not be interned, that is why DirectTermTransform used here */
    private static final class FunctorSet extends ArrayHashSet<Term> implements DirectTermTransform {

        private final Function<Term, Functor> resolver;

        FunctorSet(Function<Term, Functor> resolver) {
            this.resolver = resolver;
        }

        public FunctorSet discover(Term x) {
            x.recurseTerms(s->s.hasAll(Op.FuncBits), (xx)->{
                if (!contains(xx)) {
                    if (Functor.isFunc(xx)) {
                        Term yy = this.transform(xx);
                        if (yy!=xx || yy.sub(1) instanceof Functor) {
                            add(yy);
                        }
                    }
                }
                return true;
            }, null);
            return this;
        }




        @Override
        public @Nullable Term transformAtomic(Atomic x) {
            if (x instanceof Functor) {
                return x;
            }

            if (x.op() == ATOM) {
                Functor f = resolver.apply(x);
                if (f != null) {
                    return f;
                }
            }
            return x;
        }

        public void sortDecreasingVolume() {
            if (size() > 1)
                ((FasterList<Term>)list).sortThisByInt(Termlike::volume);
        }
    }
}
