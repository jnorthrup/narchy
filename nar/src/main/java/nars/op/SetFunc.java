package nars.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Idempotent;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.functor.UnaryParametricBidiFunctor;
import nars.term.util.SetSectDiff;
import nars.term.util.transform.InlineFunctor;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static nars.term.atom.IdempotentBool.Null;

public final class SetFunc{

    public static final Functor union = new BinarySetFunctor("union") {

        @Override
        public boolean validOp(Op o) {
            return o.commutative;
        }

        @Override
        public @Nullable Term apply(Term a, Term b) {

            return Terms.union(a.op(), a.subterms(), b.subterms() );
        }

    };

    public static final Functor unionSect = new AbstractBinarySetFunctor("unionSect") {
        @Override
        public @Nullable Term apply(Term a, Term b, Subterms s) {
            return SetSectDiff.sect(a, b, true, s);
        }
    };
    public static final Functor interSect = new AbstractBinarySetFunctor("interSect") {
        @Override
        public @Nullable Term apply(Term a, Term b, Subterms s) {
            return SetSectDiff.sect(a, b, false, s);
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
            return Terms.intersect(a.op(), a.subterms(), b.subterms());
        }

    };

    /**
     * sort(input, [mappingFunction=identity], output)
     * input: a compound of >1 items
     * output: a product containing the inputs, sorted according to (the most) natural ordering
     */
    public static Functor sort(NAR nar) {
        return new UnaryParametricBidiFunctor("sort") {

            @Override
            protected Term compute(Term x, Term param) {
                int n = x.subs();
                if (n < 2)
                    return Null; 

                if (x.hasAny(Op.Variable))
                    return null;

                FasterList<Term> l = new FasterList<Term>(n);
                l.addingAll(x.subterms().arrayShared());
                Comparator<Term> cmp;
                if (param instanceof Atomic && !param.hasVars()) {
                    
                    cmp = Comparator.comparing(new Function<Term, Term>() {
                        @Override
                        public Term apply(Term t) {
                            return eval(t, (Atomic) param);
                        }
                    }).thenComparing(new Function<Term, Term>() {
                        @Override
                        public Term apply(Term t) {
                            return t;
                        }
                    });
                } else
                    return Null; 

                l.sort(cmp);
                return $.INSTANCE.pFast(l);
            }

            private Term eval(Term t, Atomic atom) {
                return nar.eval($.INSTANCE.func(atom, t));
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
                        Term[] xxx = xx.terms(new IntObjectPredicate<Term>() {
                            @Override
                            public boolean accept(int n, Term xs) {
                                return xs.op().var;
                            }
                        });
                        if (xxx.length == 1) {
                            return e.is(xxx[0], missing.get(0)) ? null : Null;
                        }
                    }
                }

                

                return null;
            }
        };

    }

    abstract static class AbstractBinarySetFunctor extends Functor implements InlineFunctor<Evaluation>, Idempotent {

        protected AbstractBinarySetFunctor(String id) {
            super(id);
        }

        public boolean validOp(Op o) {
            return true;
        }

        @Deprecated
        @Override
        public final @Nullable Term apply(Evaluation e, Subterms x) {
            return applyInline(x);
        }

        @Override
        public @Nullable Term applyInline(Subterms x) {

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

        protected abstract Term apply(Term a, Term b, Subterms x);

    }

    abstract static class BinarySetFunctor extends AbstractBinarySetFunctor {


        protected BinarySetFunctor( String id) {
            super(id);
        }


        @Override
        public final @Nullable Term applyInline(Subterms x) {
            if (x.subs() != 2)
                throw new UnsupportedOperationException("# args must equal 2");
            return super.applyInline(x);
        }

        protected final Term apply(Term a, Term b, Subterms x) {
            return apply(a, b);
        }

        public abstract @Nullable Term apply(Term a, Term b);
    }


}
