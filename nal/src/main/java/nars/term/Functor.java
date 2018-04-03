package nars.term;

import jcog.Util;
import jcog.util.TriFunction;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.index.term.TermContext;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.pred.AbstractPred;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import static nars.Op.*;
import static nars.term.Terms.atomOrNull;

/**
 * a functor is a term transform which immediately returns
 * a result Term from the TermContainer arguments of
 * a function term, for example: f(x) or f(x, y).
 */
abstract public class Functor extends NodeConcept implements PermanentConcept, Function<Subterms, Term>, Atomic {

    protected Functor(@NotNull String atom) {
        this(fName(atom));
    }

    protected Functor(@NotNull Atom atom) {
        super(atom, ConceptBuilder.Null);
    }

    @Override
    public final byte[] bytes() {
        return ((Atomic) term).bytes();
    }


//    @Override
//    public boolean equals(Object obj) {
//        return this == obj || obj instanceof Term && term.equals(obj);
//    }

    @Override
    public final Term term() {
        return this;
    }


    @Override
    public final int opX() {
        return term.opX();
    }

    static Atom fName(String termAtom) {
        return atomOrNull(Atomic.the(termAtom));
    }

    /**
     * creates a new functor from a term name and a lambda
     */
    public static LambdaFunctor f(@NotNull String termAtom, @NotNull Function<Subterms, Term> f) {
        return f(fName(termAtom), f);
    }

    /**
     * creates a new functor from a term name and a lambda
     */
    public static LambdaFunctor f(@NotNull Atom termAtom, @NotNull Function<Subterms, Term> f) {
        return new LambdaFunctor(termAtom, f);
    }

    public static LambdaFunctor f(@NotNull String termAtom, int arityRequired, @NotNull Function<Subterms, Term> ff) {
        return f(fName(termAtom), arityRequired, ff);
    }

    public static LambdaFunctor f(@NotNull Atom termAtom, int arityRequired, @NotNull Function<Subterms, Term> ff) {
        return f(termAtom, (tt) ->
                (tt.subs() == arityRequired) ? ff.apply(tt) : Null
        );
    }
    public static LambdaFunctor f(@NotNull Atom termAtom, int minArity, int maxArity, @NotNull Function<Subterms, Term> ff) {
        return f(termAtom, (tt) -> {
            int n = tt.subs();
            return ((n >= minArity) && ( n<=maxArity)) ? ff.apply(tt) : Null;
        });
    }
    /**
     * zero argument (void) functor (convenience method)
     */
    public static LambdaFunctor f0(@NotNull Atom termAtom, @NotNull Supplier<Term> ff) {
        return f(termAtom, 0, (tt) -> ff.get());
    }

    public static LambdaFunctor f0(@NotNull String termAtom, @NotNull Supplier<Term> ff) {
        return f0(fName(termAtom), ff);
    }

    public static LambdaFunctor r0(@NotNull String termAtom, @NotNull Supplier<Runnable> ff) {
        Atom fName = fName(termAtom);
        return f0(fName, () -> new AbstractPred<>($.inst($.quote(Util.uuid64()), fName)) {

            @Override
            public boolean test(Object o) {
                try {
                    Runnable r = ff.get();
                    r.run();
                    return true;
                } catch (Throwable t) {
                    t.printStackTrace();
                    return false;
                }
            }
        });
    }

    /**
     * one argument functor (convenience method)
     */
    public static LambdaFunctor f1(@NotNull Atom termAtom, @NotNull Function<Term, Term> ff) {
        return f(termAtom, 1, (tt) -> ff.apply(tt.sub(0)));
    }

    public static LambdaFunctor f1(@NotNull String termAtom, @NotNull Function<Term, Term> ff) {
        return f1(fName(termAtom), safeFunctor(ff));
    }

    public static <X extends Term> LambdaFunctor f1Const(@NotNull String termAtom, @NotNull Function<X, Term> ff) {
        return f1(fName(termAtom), (Term x) -> {
            if (x == null || x.vars() > 0)
                return null;
            return ff.apply((X) x);
        });
    }


    static Function<Term, Term> safeFunctor(@NotNull Function<Term, Term> ff) {
        return x ->
                (x == null) ? null
                        :
                        ff.apply(x);
    }

    /**
     * a functor involving a concept resolved by the 1st argument term
     */
    public static LambdaFunctor f1Concept(@NotNull String termAtom, NAR nar, @NotNull BiFunction<Concept, NAR, Term> ff) {
        return f1(fName(termAtom), t -> {
            Concept c = nar.concept(t);
            if (c != null) {
                return ff.apply(c, nar);
            } else {
                return null;
            }
        });
    }


    /**
     * two argument functor (convenience method)
     */
    public static LambdaFunctor f2(@NotNull Atom termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f(termAtom, 2, (tt) -> ff.apply(tt.sub(0), tt.sub(1)));
    }

    /**
     * two argument functor (convenience method)
     */
    public static LambdaFunctor f2(@NotNull String termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f2(fName(termAtom), ff);
    }
    public static LambdaFunctor f3(@NotNull String termAtom, @NotNull TriFunction<Term, Term, Term, Term> ff) {
        return f3(fName(termAtom), ff);
    }

    /**
     * three argument functor (convenience method)
     */
    public static LambdaFunctor f3(@NotNull Atom termAtom, @NotNull TriFunction<Term, Term, Term, Term> ff) {
        return f(termAtom, 3, (tt) -> ff.apply(tt.sub(0), tt.sub(1), tt.sub(2)));
    }

    public static LambdaFunctor f2Or3(@NotNull String termAtom, @NotNull Function<Term[], Term> ff) {
        return f2Or3(fName(termAtom), ff);
    }

    public static LambdaFunctor f2Or3(@NotNull Atom termAtom, @NotNull Function<Term[], Term> ff) {
        return f(termAtom, 2, 3, (tt) -> ff.apply(tt.arrayShared()));
    }
    /**
     * two argument non-variable integer functor (convenience method)
     */
    @FunctionalInterface
    public interface IntIntToIntFunction {
        int apply(int x, int y);
    }

    public static Concept f2Int(String termAtom, boolean commutive, @Nullable IntPredicate identityComponent, IntPredicate zeroIfArgIs, IntIntToIntFunction ff) {
        Atom f = fName(termAtom);
        return f2(f, (xt, yt) -> {
            boolean xi = xt.op() == INT;
            boolean yi = yt.op() == INT;
            if (xi && yi) {
                int xid = ((Int)xt).id;
                int yid = ((Int)yt).id;
                return Int.the( ff.apply(xid, yid ) );
            } else {
                if (identityComponent!=null) {
                    if (xi) {
                        int xid = ((Int) xt).id;
                        if (zeroIfArgIs.test(xid))
                            return Int.the(0);

                        if (identityComponent.test(xid))
                            return yt;
                    }
                    if (yi) {
                        int yid = ((Int) yt).id;
                        if (zeroIfArgIs.test(yid))
                            return Int.the(0);

                        if (identityComponent.test(yid))
                            return xt;
                    }
                }

                if (commutive && xt.compareTo(yt) > 0) {
                    return $.func(f, yt, xt);
                }
                return null;
            }
        });
    }


    public static final class LambdaFunctor extends Functor {

        private final Function<Subterms, Term> f;

        public LambdaFunctor(@NotNull Atom termAtom, @NotNull Function<Subterms, Term> f) {
            super(termAtom);
            this.f = f;
        }

        @Nullable
        @Override
        public final Term apply(Subterms terms) {
            return f.apply(terms);
        }
    }

    /**
     * Created by me on 12/12/15.
     */
    public abstract static class UnaryFunctor extends Functor {

        protected UnaryFunctor(@NotNull String id) {
            super(id);
        }

        @Nullable
        @Override
        public final Term apply(Subterms x) {
            if (x.subs() != 1)
                return null;
            //throw new UnsupportedOperationException("# args must equal 1");

            return apply(x.sub(0));
        }

        @Nullable
        public abstract Term apply(Term x);
    }

    /**
     * Created by me on 12/12/15.
     */
    public abstract static class BinaryFunctor extends Functor {

        protected BinaryFunctor(@NotNull String id) {
            super(id);
        }

        public boolean validOp(Op o) {
            return true;
        }

        @Nullable
        @Override
        public final Term apply(Subterms x) {
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

    public abstract static class FunctorResolver implements TermContext {

        @Override
        public Term applyTermIfPossible(Term x, Op supertermOp, int subterm) {
            //only need to resolve for the predicate subterm of an INH
            if (subterm==1 && supertermOp==INH && x.op()==ATOM) {
                Termed y = apply(x);
                if (y != null) {
                    Term yt = y.term();
                    if (yt instanceof Functor)
                        return yt;
                }
            }

            return x;
        }
    }


    /** Functor template for a binary functor with bidirectional parameter cases */
    public abstract static class BidiBinaryFunctor extends Functor {

        public BidiBinaryFunctor(String name) {
            this(fName(name));
        }

        public BidiBinaryFunctor(Atom atom) {
            super(atom);
        }

        @Nullable
        @Override
        public final Term apply(Subterms terms) {
            int s = terms.subs();
            switch (s) {
                case 2:
                    return apply2(terms.sub(0), terms.sub(1));
                case 3:
                    return apply3(terms.sub(0), terms.sub(1), terms.sub(2));
                default:
                    return Null; //invalid
            }
        }

        protected Term apply2(Term x, Term y) {
            if (x.op().var || y.op().var)
                return null; //do nothing
            else {
                return compute(x,y); //replace with result
            }
        }

        protected abstract Term compute(Term x, Term y);

        protected Term apply3(Term x, Term y, Term xy) {
            boolean xVar = x.op().var;
            boolean yVar = y.op().var;
            if (xy.op().var) {
                //forwards
                if (xVar || yVar) {
                    return null; //uncomputable; no change
                } else {
                    return Solution.solve(s -> {
                        s.replace(xy, compute(x, y));
                    });
                }
            } else {
                if (xVar && !yVar) {
                    return computeXfromYandXY(x, y, xy);
                } else if (yVar && !xVar) {
                    return computeYfromXandXY(x, y, xy);
                } else if (!yVar && !xVar) {
                    if (compute(x, y).equals(xy)) {
                        //equal
                        return null; //unchanged
                    } else {
                        //inequal
                        return False;
                    }
                } else {
                    return computeFromXY(x, y, xy); //all variables
                }
            }
        }

        protected abstract Term computeFromXY(Term x, Term y, Term xy);

        protected abstract Term computeXfromYandXY(Term x, Term y, Term xy);

        protected abstract Term computeYfromXandXY(Term x, Term y, Term xy);
    }
}
