package nars.term;

import jcog.Util;
import jcog.util.TriFunction;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.The;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.Operator;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.eval.Evaluation;
import nars.link.TermLinker;
import nars.op.Equal;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.*;
import static nars.term.Terms.atomOrNull;

/**
 * a functor is a term transform which immediately returns
 * a result Term from the TermContainer arguments of
 * a function term, for example: f(x) or f(x, y).
 */
abstract public class Functor extends NodeConcept implements PermanentConcept, BiFunction<Evaluation, Subterms, Term>, Atomic {

    protected Functor(@NotNull String atom) {
        this(fName(atom));
    }

    protected Functor(@NotNull Atom atom) {
        super(atom, TermLinker.NullLinker, ConceptBuilder.NullConceptBuilder);
    }

    public static Atomic func(Term x) {
        return isFunc(x) ? (Atomic) x.sub(1) : Op.Null;
    }
    public static boolean isFunc(Term x) {
        return (x.hasAll(Op.FuncBits) && x.op()==INH && x.sub(0).op()==PROD && x.sub(1).op()==ATOM );
    }

    public static Term[] funcArgsArray(Term x) {
        return Operator.args(x).arrayShared();
    }

    @Override
    public final byte[] bytes() {
        return ((Atomic) term).bytes();
    }


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
    private static LambdaFunctor f(@NotNull Atom termAtom, @NotNull Function<Subterms, Term> f) {
        return new LambdaFunctor(termAtom, f);
    }

    public static LambdaFunctor f(@NotNull String termAtom, int arityRequired, @NotNull Function<Subterms, Term> ff) {
        return f(fName(termAtom), arityRequired, ff);
    }

    private static LambdaFunctor f(@NotNull Atom termAtom, int arityRequired, @NotNull Function<Subterms, Term> ff) {
        return f(termAtom, tt ->
                (tt.subs() == arityRequired) ? ff.apply(tt) : Null
        );
    }
    private static LambdaFunctor f(@NotNull Atom termAtom, int minArity, int maxArity, @NotNull Function<Subterms, Term> ff) {
        return f(termAtom, (tt) -> {
            int n = tt.subs();
            return ((n >= minArity) && ( n<=maxArity)) ? ff.apply(tt) : Null;
        });
    }
    /**
     * zero argument (void) functor (convenience method)
     */
    private static LambdaFunctor f0(@NotNull Atom termAtom, @NotNull Supplier<Term> ff) {
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

    public static Functor f1Inline(@NotNull String termAtom, @NotNull Function<Term, Term> ff) {
        return new MyAbstractInlineFunctor1Inline(termAtom, ff);
    }

    public static Functor f2Inline(@NotNull String termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return new MyAbstractInlineFunctor2Inline(termAtom, ff);
    }

    public static <X extends Term> LambdaFunctor f1Const(@NotNull String termAtom, @NotNull Function<X, Term> ff) {
        return f1(fName(termAtom), (Term x) -> {
            if (x == null || x.hasVars())
                return null;
            return ff.apply((X) x);
        });
    }


    private static Function<Term, Term> safeFunctor(@NotNull Function<Term, Term> ff) {
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
    private static LambdaFunctor f2(@NotNull Atom termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
        return f(termAtom, 2, tt -> ff.apply(tt.sub(0), tt.sub(1)));
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

    private static LambdaFunctor f2Or3(@NotNull Atom termAtom, @NotNull Function<Term[], Term> ff) {
        return f(termAtom, 2, 3, (tt) -> ff.apply(tt.arrayShared()));
    }
    /**
     * two argument non-variable integer functor (convenience method)
     */
    @FunctionalInterface
    public interface IntIntToIntFunction {
        int apply(int x, int y);
    }

//    public static Functor f2Int(String termAtom, boolean commutive, @Nullable IntPredicate identityComponent, IntPredicate zeroIfArgIs, IntIntToIntFunction ff) {
//        Atom f = fName(termAtom);
//        return f2(f, (xt, yt) -> {
//            boolean xi = xt.op() == INT;
//            boolean yi = yt.op() == INT;
//            if (xi && yi) {
//                int xid = ((Int)xt).id;
//                int yid = ((Int)yt).id;
//                return Int.the( ff.apply(xid, yid ) );
//            } else {
//                if (identityComponent!=null) {
//                    if (xi) {
//                        int xid = ((Int) xt).id;
//                        if (zeroIfArgIs.test(xid))
//                            return Int.the(0);
//
//                        if (identityComponent.test(xid))
//                            return yt;
//                    }
//                    if (yi) {
//                        int yid = ((Int) yt).id;
//                        if (zeroIfArgIs.test(yid))
//                            return Int.the(0);
//
//                        if (identityComponent.test(yid))
//                            return xt;
//                    }
//                }
//
//                if (commutive && xt.compareTo(yt) > 0) {
//                    return $.func(f, yt, xt);
//                }
//                return null;
//            }
//        });
//    }

    /** marker interface for functors which are allowed to be applied during
     * transformation or term construction processes.
     * these are good for simple functors that are guaranteed to return quickly.
     */
    public interface InlineFunctor extends BiFunction<Evaluation, Subterms,Term> {

        /** dont override this one, override the Subterms arg version */
        default /*final */ Term applyInline(Term args) {
            return applyInline(args.subterms());
        }
        default Term applyInline(Subterms args) {
            return apply(null, args);
        }

    }


    abstract static class AbstractInlineFunctor extends Functor implements Functor.InlineFunctor {

        AbstractInlineFunctor(String atom) {
            super(atom);
        }
    }
    abstract public static class AbstractInlineFunctor1 extends AbstractInlineFunctor {

        protected AbstractInlineFunctor1(String atom) {
            super(atom);
        }

        protected abstract Term apply1(Term arg);

        @Override
        public final Term applyInline(Subterms args) {
            if (args.subs()!=1) return Null;
            return apply1(args.sub(0));
        }

        @Override
        public final Term apply(Evaluation e, Subterms terms) {
            return terms.subs() != 1 ? Null : apply1(terms.sub(0));
        }

    }

    abstract public static class AbstractInlineFunctor2 extends AbstractInlineFunctor {

        protected AbstractInlineFunctor2(String atom) {
            super(atom);
        }

        @Override
        final public Term apply(Evaluation e, Subterms terms) {
            return terms.subs() != 2 ? Null : apply(terms.sub(0), terms.sub(1));
        }

        protected abstract Term apply(Term a, Term b);
    }


    public static final class LambdaFunctor extends Functor implements The {

        private final Function<Subterms, Term> f;

        LambdaFunctor(@NotNull Atom termAtom, @NotNull Function<Subterms, Term> f) {
            super(termAtom);
            this.f = f;
        }

        @Nullable
        @Override
        public final Term apply(Evaluation e, Subterms terms) {
            return f.apply(terms);
        }
    }





    /** (potentially) reversible function */
    public abstract static class UnaryBidiFunctor extends Functor {

        public UnaryBidiFunctor(Atom atom) {
            super(atom);
        }

        public UnaryBidiFunctor(String atom) { super(atom); }

        @Nullable
        @Override
        public final Term apply(Evaluation e, Subterms terms) {
            int s = terms.subs();
            switch (s) {
                case 1:
                    return apply1(terms.sub(0));
                case 2:
                    return apply2(e, terms.sub(0), terms.sub(1));
                default:
                    return Null; 
            }
        }

        Term apply1(Term x) {
            if (x.op().var)
                return null; 
            else {
                return compute(x); 
            }
        }

        protected abstract Term compute(Term x);

        /** override for reversible functions, though it isnt required */
        protected Term uncompute(Term x, Term y) {
            return null;
        }

        protected Term apply2(Evaluation e, Term x, Term y) {
            boolean xVar = x.op().var;
            if (y.op().var) {
                
                if (xVar) {
                    return null; 
                } else {
                    Term XY = compute(x);
                    if (XY!=null) {
                        return e.is(y, XY) ?
                                null : Null;
                    } else {
                        return null; 
                    }
                }
            } else {
                if (x.hasAny(Op.Variable)) {
                    Term X = uncompute(x, y);
                    if (X!=null) {
                        return e.is(x, X) ?
                            null : Null;
                    } else {
                        return null; 
                    }
                } else {
                    
                    Term yActual = compute(x);
                    if (yActual == null)
                        return null;  
                    else
                        return Equal.the(y,yActual);
//                        if (XY.equals(y)) {
//                        return True;
//                    } else {
//                        return False;
//                    }
                }
            }
        }
    }

    /** UnaryBidiFunctor with constant-like parameter */
    public abstract static class UnaryParametricBidiFunctor extends Functor {

        public UnaryParametricBidiFunctor(Atom atom) {
            super(atom);
        }

        public UnaryParametricBidiFunctor(String atom) { super(atom); }

        @Nullable
        @Override
        public final Term apply(Evaluation e,Subterms terms) {
            int s = terms.subs();
            switch (s) {
                case 2:
                    return apply1(terms.sub(0), terms.sub(1));
                case 3:
                    return apply2(e, terms.sub(0), terms.sub(1), terms.sub(2));
                default:
                    return Null; 
            }
        }

        Term apply1(Term x, Term parameter) {
            return !x.op().var ? compute(x, parameter) : null;
        }

        protected abstract Term compute(Term x, Term parameter);

        /** override for reversible functions, though it isnt required */
        protected Term uncompute(Evaluation e, Term x, Term param, Term y) {
            return null;
        }

        Term apply2(Evaluation e, Term x, Term param, Term y) {
            boolean xVar = x.op().var;
            if (y.op().var) {
                
                if (xVar) {
                    return null; 
                } else {
                    Term XY = compute(x, param);
                    if (XY!=null) {
                        return e.is(y, XY) ? null : Null;
                    } else {
                        return null; 
                    }
                }
            } else {
                if (x.hasAny(Op.Variable)) {
                    Term X = uncompute(e, x, param, y);
                    if (X!=null) {
                        return e.is(x, X) ? null : Null;
                    } else {
                        return null; 
                    }
                } else {
                    
                    Term XY = compute(x, param);
                    if (XY != null) {
                        return XY.equals(y) ? True  : Null;
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    /** Functor template for a binary functor with bidirectional parameter cases */
    public abstract static class BinaryBidiFunctor extends Functor implements The {

        public BinaryBidiFunctor(String name) {
            this(fName(name));
        }

        BinaryBidiFunctor(Atom atom) {
            super(atom);
        }

        @Nullable
        @Override
        public final Term apply(Evaluation e, Subterms terms) {
            int s = terms.subs();
            switch (s) {
                case 2:
                    return apply2(e, terms.sub(0), terms.sub(1));
                case 3:
                    return apply3(e, terms.sub(0), terms.sub(1), terms.sub(2));
                default:
                    return Null; 
            }
        }

        Term apply2(Evaluation e, Term x, Term y) {
            if (x.op().var || y.op().var)
                return null; 
            else {
                return compute(e, x,y);
            }
        }

        protected abstract Term compute(Evaluation e, Term x, Term y);

        Term apply3(Evaluation e, Term x, Term y, Term xy) {
            boolean xVar = x.op().var;
            boolean yVar = y.op().var;
            if (xy.op().var) {
                
                if (xVar || yVar) {
                    return null; 
                } else {
                    Term XY = compute(e, x, y);
                    if (XY!=null) {
                        return e.is(xy, XY) ? null : Null;
                    } else {
                        return null; 
                    }
                }
            } else {
                if (xVar && !yVar) {
                    return computeXfromYandXY(e, x, y, xy);
                } else if (yVar && !xVar) {
                    return computeYfromXandXY(e, x, y, xy);
                } else if (!yVar && !xVar) {
                    
                    Term XY = compute(e, x, y);
                    assert(XY!=null);
                    if (XY.equals(xy)) {
                        
                        return null; //true, keep
                    } else {
                        
                        return Null;
                    }
                } else {
                    return computeFromXY(e, x, y, xy);
                }
            }
        }

        protected abstract Term computeFromXY(Evaluation e, Term x, Term y, Term xy);

        protected abstract Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy);

        protected abstract Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy);
    }

    public abstract static class CommutiveBinaryBidiFunctor extends BinaryBidiFunctor {


        CommutiveBinaryBidiFunctor(String name) {
            super(name);
        }

        public CommutiveBinaryBidiFunctor(Atom atom) {
            super(atom);
        }


        @Override
        protected Term apply2(Evaluation e, Term x, Term y) {
            if (x.compareTo(y) > 0) {
                //return $.func((Atomic) term(), y, x);
                Term z = x;
                x = y;
                y = z;
            }

            return super.apply2(e, x, y);
        }

        @Override
        protected final Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy) {
            return computeXfromYandXY(e, y, x, xy);
        }
    }

    abstract public static class InlineCommutiveBinaryBidiFunctor extends CommutiveBinaryBidiFunctor implements InlineFunctor {

        protected InlineCommutiveBinaryBidiFunctor(String name) {
            super(name);
        }
    }

    protected static class MyAbstractInlineFunctor1Inline extends AbstractInlineFunctor1 {
        private @NotNull
        final Function<Term, Term> ff;

        MyAbstractInlineFunctor1Inline(@NotNull String termAtom, @NotNull Function<Term, Term> ff) {
            super(termAtom);
            this.ff = ff;
        }

        @Override
        public final Term apply1(Term arg) {
            return ff.apply(arg);
        }

    }

    static class TheAbstractInlineFunctor1Inline extends MyAbstractInlineFunctor1Inline implements The {
        public TheAbstractInlineFunctor1Inline(@NotNull String termAtom, @NotNull Function<Term, Term> ff) {
            super(termAtom, ff);
        }
    }

    private static class MyAbstractInlineFunctor2Inline extends AbstractInlineFunctor2 {
        private @NotNull
        final BiFunction<Term, Term, Term> ff;

        MyAbstractInlineFunctor2Inline(@NotNull String termAtom, @NotNull BiFunction<Term, Term, Term> ff) {
            super(termAtom);
            this.ff = ff;
        }

        @Override protected final Term apply(Term a, Term b) {
            return ff.apply(a, b);
        }
    }
}
