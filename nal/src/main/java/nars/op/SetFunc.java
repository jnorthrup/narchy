package nars.op;

import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static nars.Op.INT;
import static nars.Op.Null;
import static nars.time.Tense.DTERNAL;

public class SetFunc {

    public static final Functor union = new Functor.BinaryFunctor("union") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Nullable
        @Override public Term apply(Term a, Term b) {

            return union(a.op(), a.subterms(), b.subterms() );
        }

    };

    /**
     * all X which are in the first term AND not in the second term
     */
    public static final Functor differ = new Functor.BinaryFunctor("differ") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;// || o == INT;
        }

        @Override
        public Term apply(Term a, Term b) {
            return Op.differenceSet(a.op(), a, b);
        }
    };



    public static final Functor intersect = new Functor.BinaryFunctor("intersect") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
    //        Op aop = a.op();
    //        if (b.op() == aop)

            if (a instanceof Int.IntRange && b.op()==INT) {
               return ((Int.IntRange)a).intersect(b);
            }

            return intersect(a.op(), a.subterms(), b.subterms());
    //        else
    //            return null;
        }

    };

    public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        if (a instanceof Term && a.equals(b))
            return (Term) a;


        Set<Term> cc = Subterms.intersect(a, b);
        if (cc == null) return Null;

        int ssi = cc.size();
        if (ssi == 0) return Null;

        Term[] c = cc.toArray(new Term[ssi]);
        if (ssi > 1)
            Arrays.sort(c);

        return o.the(c);
    }

    public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        boolean bothTerms = a instanceof Term && b instanceof Term;
        if (bothTerms && a.equals(b))
            return (Term) a;

        TreeSet<Term> t = new TreeSet<>();
        a.copyInto(t);
        b.copyInto(t);
        if (bothTerms) {
            int as = a.subs();
            int bs = b.subs();
            int maxSize = Math.max(as, bs);
            if (t.size() == maxSize) {
                //the smaller is contained by the larger other
                //so return an input value rather than constructing a duplicate
                return (Term) (as > bs ? a : b);
            }
        }
        return o.the(DTERNAL, t);
    }

    /**
     * sort(input, [mappingFunction=identity], output)
     * input: a compound of >1 items
     * output: a product containing the inputs, sorted according to (the most) natural ordering
     */
    public static Functor sort(NAR nar) {
        return new Functor.UnaryParametricBidiFunctor("sort") {

            @Override
            protected Term compute(Term x, Term param) {
                int n = x.subs();
                if (n < 2)
                    return Null; //invalid

                if (x.hasAny(Op.varBits))
                    return null; //incomputable

                List<Term> l = new FasterList<>(n);
                ((FasterList<Term>) l).addingAll(x.subterms().arrayShared());
                Comparator<Term> cmp;
                if (param instanceof Atomic && !param.hasVars()) {
                    //TODO cache intermediate results if n >> 2
                    cmp = Comparator.comparing((Term t) -> eval(t, (Atomic) param)).thenComparing((Term t) -> t);
                } else
                    return Null; //TODO support other comparator patterns, ex: x(a,#1)

                l.sort(cmp);
                return $.pFast(l);
            }

            private Term eval(Term t, Atomic atom) {
                Term tt = $.func(atom, t);
                return tt.eval(nar);
            }

            @Override
            protected Term uncompute(Term x, Term param, Term y) {
                //deduce specific terms present in 'y' but not 'x'

                //HACK simple case of 1
                if (!y.hasVars() && x.vars() == 1) {
                    Subterms xx = x.subterms();
                    Subterms yy = y.subterms();
                    List<Term> missing = new FasterList(1);
                    for (Term sy : yy) {
                        if (!xx.contains(sy)) {
                            missing.add(sy);
                        }
                    }
                    if (missing.size() == 1) {
                        Term[] xxx = xx.terms((n, xs) -> xs.op().var);
                        if (xxx.length == 1) {
                            Evaluation.the().replace(xxx[0], missing.get(0));
                            return null;
                        }
                    }
                }

                //TODO if y.subs()==1 then there is only one solution

                return null;
            }
        };
    }

}
