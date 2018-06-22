package nars.op;

import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static nars.Op.INT;
import static nars.Op.Null;
import static nars.time.Tense.DTERNAL;

public class SetFunc {

    public static final Functor union = new BinarySetFunctor("union") {

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
    public static final Functor differ = new BinarySetFunctor("differ") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
            return Op.differenceSet(a.op(), a, b);
        }
    };



    public static final Functor intersect = new BinarySetFunctor("intersect") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
    
    

            if (a instanceof Int.IntRange && b.op()==INT) {
               return ((Int.IntRange)a).intersect(b);
            }

            return intersect(a.op(), a.subterms(), b.subterms());
    
    
        }

    };

    public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        if (a instanceof Term && a.equals(b))
            return (Term) a;

        SortedSet<Term> cc = Subterms.intersectSorted(a, b);
        if (cc == null)
            return Null;

        int ssi = cc.size();
        switch (ssi) {
            case 0: return Null;
            case 1: return cc.first();
            default:
                return Op.compoundExact(o, DTERNAL, cc.toArray(Op.EmptyTermArray));
        }


    }

    public static Term union(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        boolean bothTerms = a instanceof Term && b instanceof Term;
        if (bothTerms && a.equals(b))
            return (Term) a;

        TreeSet<Term> t = new TreeSet<>();
        a.addTo(t);
        b.addTo(t);
        if (bothTerms) {
            int as = a.subs();
            int bs = b.subs();
            int maxSize = Math.max(as, bs);
            if (t.size() == maxSize) {
                
                
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
                    return Null; 

                if (x.hasAny(Op.varBits))
                    return null; 

                FasterList<Term> l = new FasterList<>(n);
                l.addingAll(x.subterms().arrayShared());
                Comparator<Term> cmp;
                if (param instanceof Atomic && !param.hasVars()) {
                    
                    cmp = Comparator.comparing((Term t) -> eval(t, (Atomic) param)).thenComparing((Term t) -> t);
                } else
                    return Null; 

                l.sort(cmp);
                return $.pFast(l);
            }

            private Term eval(Term t, Atomic atom) {
                Term tt = $.func(atom, t);
                return tt.eval(nar, false);
            }

            @Override
            protected Term uncompute(Evaluation e, Term x, Term param, Term y) {
                

                
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
                            e.replace(xxx[0], missing.get(0));
                            return null;
                        }
                    }
                }

                

                return null;
            }
        };

    }

    abstract static class BinarySetFunctor extends Functor implements Functor.InlineFunctor {

        protected BinarySetFunctor( String id) {
            super(id);
        }

        public boolean validOp(Op o) {
            return true;
        }

        @Nullable
        @Override
        @Deprecated public final Term apply(Evaluation e, Subterms x) {
            return applyInline(x);
        }

        @Nullable
        @Override
        public final Term applyInline(Subterms x) {
            if (x.subs() != 2)
                throw new UnsupportedOperationException("# args must equal 2");


            Term a = x.sub(0);
            Term b = x.sub(1);
            if ((a instanceof Variable) || (b instanceof Variable))
                return null;

            if (!validOp(a.op()) || !validOp(b.op()))
                return Null;

            return apply(a, b);
        }

        @Nullable
        public abstract Term apply(Term a, Term b);
    }


}
