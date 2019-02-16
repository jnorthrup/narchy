package nars.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.SetSectDiff;
import nars.term.util.builder.HeapTermBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static nars.term.atom.Bool.Null;

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
    public static final Functor unionSect = new AbstractBinarySetFunctor("unionSect") {

        @Nullable
        @Override public Term apply(Term a, Term b, Subterms s) {

            Op op = s.sub(2).equals(Op.SECTe.strAtom) ? Op.SECTe : Op.SECTi;
            return SetSectDiff.intersect(HeapTermBuilder.the, op, true, a, b);
        }
    };
    /**
     * all X which are in the first target AND not in the second target
     */
    public static final Functor differ = new BinarySetFunctor("differ") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
            return SetSectDiff.differenceSet(a.op(), a, b);
        }
    };



    public static final Functor intersect = new BinarySetFunctor("intersect") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public Term apply(Term a, Term b) {
            return intersect(a.op(), a.subterms(), b.subterms());
        }

    };

    static @Nullable Set<Term> intersect(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {
        if ((a.structure() & b.structure()) != 0) {

            Predicate<Term> contains = a.subs() > 2 ? (a.toSet()::contains) : a::contains;
            Set<Term> ab = b.toSet(contains);
            if (ab != null)
                return ab;
        }
        return null;
    }

    public static Term intersect(/*@NotNull*/ Op o, Subterms a, Subterms b) {
        if (a instanceof Term && a.equals(b))
            return (Term) a;

        Set<Term> cc = intersect(b, a);
        if (cc == null)
            return Null;

        int ssi = cc.size();
        switch (ssi) {
            case 0: return Null;
            case 1: return cc.iterator().next();
            default:
                return Op.compound(o, cc.toArray(Op.EmptyTermArray));
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
        return o.the(t);
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
                return $.func(atom, t).eval(nar);
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
                            return e.is(xxx[0], missing.get(0)) ? null : Null;
                        }
                    }
                }

                

                return null;
            }
        };

    }

    abstract static class AbstractBinarySetFunctor extends Functor implements Functor.InlineFunctor, The {

        protected AbstractBinarySetFunctor(String id) {
            super(id);
        }

        public boolean validOp(Op o) {
            return true;
        }

        @Nullable
        @Override
        @Deprecated
        public final Term apply(Evaluation e, Subterms x) {
            return applyInline(x);
        }

        @Nullable
        @Override
        public Term applyInline(Subterms x) {

            Term a = x.sub(0);
//            if (a instanceof Variable)
//                return null;
            if (!validOp(a.op()))
                return Null;

            Term b = x.sub(1);
//            if (b instanceof Variable)
//                return null;
            if (!validOp(b.op()))
                return Null;

            return apply(a, b, x);
        }

        abstract protected Term apply(Term a, Term b, Subterms x);

    }

    abstract static class BinarySetFunctor extends AbstractBinarySetFunctor {


        protected BinarySetFunctor( String id) {
            super(id);
        }


        @Nullable
        @Override
        public final Term applyInline(Subterms x) {
            if (x.subs() != 2)
                throw new UnsupportedOperationException("# args must equal 2");
            return super.applyInline(x);
        }

        protected final Term apply(Term a, Term b, Subterms x) {
            return apply(a, b);
        }

        @Nullable
        public abstract Term apply(Term a, Term b);
    }


}
