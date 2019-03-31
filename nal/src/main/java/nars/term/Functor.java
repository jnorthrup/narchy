package nars.term;

import jcog.Util;
import jcog.func.TriFunction;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.Operator;
import nars.concept.PermanentConcept;
import nars.eval.Evaluation;
import nars.link.TermLinker;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.functor.AbstractInlineFunctor1;
import nars.term.functor.AbstractInlineFunctor2;
import nars.term.functor.LambdaFunctor;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.*;
import static nars.term.Terms.atomOrNull;

/**
 * a functor is a target transform which immediately returns
 * a result Term from the TermContainer arguments of
 * a function target, for example: f(x) or f(x, y).
 */
abstract public class Functor extends NodeConcept implements PermanentConcept, BiFunction<Evaluation, Subterms, Term>, Atomic {

    protected Functor(String atom) {
        this(fName(atom));
    }

    protected Functor(Atom atom) {
        super(atom, TermLinker.NullLinker);
    }

    @Override
    public final Op op() {
        return ATOM;
    }

    public static Atomic func(Term x) {
        return isFunc(x) ? (Atomic) x.sub(1) : Bool.Null;
    }

    public static boolean isFunc(Term x) {
        if (x.op() == INH && x.hasAll(Op.FuncBits)) {
            Subterms xx = x.subterms();
            return xx.subIs(0, PROD) && xx.subIs(1, ATOM);
        }
        return false;
    }

    public static Term[] funcArgsArray(Term x) {
        return Operator.args(x).arrayShared();
    }

    @Nullable public static Term[] funcArgsArray(Term x, int requireN) {
        Subterms a = Operator.args(x);
        if (a.subs()==requireN)
            return a.arrayShared();
        else
            return null;
    }

    @Override
    public final byte[] bytes() {
        return ((Atomic) term).bytes();
    }


    @Override
    public Term term() {
        return this;
    }
    @Override
    public final int opX() {
        return term.opX();
    }

    public static Atom fName(String termAtom) {
        return atomOrNull(Atomic.the(termAtom));
    }

    /**
     * creates a new functor from a target name and a lambda
     */
    public static LambdaFunctor f(String termAtom, Function<Subterms, Term> f) {
        return f(fName(termAtom), f);
    }

    /**
     * creates a new functor from a target name and a lambda
     */
    private static LambdaFunctor f(Atom termAtom, Function<Subterms, Term> f) {
        return new LambdaFunctor(termAtom, f);
    }

    public static LambdaFunctor f(String termAtom, int arityRequired, Function<Subterms, Term> ff) {
        return f(fName(termAtom), arityRequired, ff);
    }

    private static LambdaFunctor f(Atom termAtom, int arityRequired, Function<Subterms, Term> ff) {
        return f(termAtom, tt ->
                (tt.subs() == arityRequired) ? ff.apply(tt) : Bool.Null
        );
    }
    private static LambdaFunctor f(Atom termAtom, int minArity, int maxArity, Function<Subterms, Term> ff) {
        return f(termAtom, (tt) -> {
            int n = tt.subs();
            return ((n >= minArity) && ( n<=maxArity)) ? ff.apply(tt) : Bool.Null;
        });
    }
    /**
     * zero argument (void) functor (convenience method)
     */
    private static LambdaFunctor f0(Atom termAtom, Supplier<Term> ff) {
        return f(termAtom, 0, (tt) -> ff.get());
    }

    public static LambdaFunctor f0(String termAtom, Supplier<Term> ff) {
        return f0(fName(termAtom), ff);
    }

    public static LambdaFunctor r0(String termAtom, Supplier<Runnable> ff) {
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
    public static LambdaFunctor f1(Atom termAtom, Function<Term, Term> ff) {
        return f(termAtom, 1, (tt) -> ff.apply(tt.sub(0)));
    }

    public static LambdaFunctor f1(String termAtom, Function<Term, Term> ff) {
        return f1(fName(termAtom), safeFunctor(ff));
    }

    public static Functor f1Inline(String termAtom, Function<Term, Term> ff) {
        return new AbstractInlineFunctor1.MyAbstractInlineFunctor1Inline(termAtom, ff);
    }

    public static Functor f2Inline(String termAtom, BiFunction<Term, Term, Term> ff) {
        return new AbstractInlineFunctor2.MyAbstractInlineFunctor2Inline(termAtom, ff);
    }

    public static <X extends Term> LambdaFunctor f1Const(String termAtom, Function<X, Term> ff) {
        return f1(fName(termAtom), (Term x) -> {
            if (x == null || x.hasVars())
                return null;
            return ff.apply((X) x);
        });
    }


    private static Function<Term, Term> safeFunctor(Function<Term, Term> ff) {
        return x ->
                (x == null) ? null
                        :
                        ff.apply(x);
    }

    /**
     * a functor involving a concept resolved by the 1st argument target
     */
    public static LambdaFunctor f1Concept(String termAtom, NAR nar, BiFunction<Concept, NAR, Term> ff) {
        return f1(fName(termAtom), t -> {
            Concept c = nar.concept(t);
            return c != null ? ff.apply(c, nar) : null;
        });
    }


    /**
     * two argument functor (convenience method)
     */
    private static LambdaFunctor f2(Atom termAtom, BiFunction<Term, Term, Term> ff) {
        return f(termAtom, 2, tt -> ff.apply(tt.sub(0), tt.sub(1)));
    }

    /**
     * two argument functor (convenience method)
     */
    public static LambdaFunctor f2(String termAtom, BiFunction<Term, Term, Term> ff) {
        return f2(fName(termAtom), ff);
    }
    public static LambdaFunctor f3(String termAtom, TriFunction<Term, Term, Term, Term> ff) {
        return f3(fName(termAtom), ff);
    }

    /**
     * three argument functor (convenience method)
     */
    public static LambdaFunctor f3(Atom termAtom, TriFunction<Term, Term, Term, Term> ff) {
        return f(termAtom, 3, (tt) -> ff.apply(tt.sub(0), tt.sub(1), tt.sub(2)));
    }

    public static LambdaFunctor f2Or3(String termAtom, Function<Term[], Term> ff) {
        return f2Or3(fName(termAtom), ff);
    }

    private static LambdaFunctor f2Or3(Atom termAtom, Function<Term[], Term> ff) {
        return f(termAtom, 2, 3, (tt) -> ff.apply(tt.arrayShared()));
    }


//    static class TheAbstractInlineFunctor1Inline extends AbstractInlineFunctor1.MyAbstractInlineFunctor1Inline implements The {
//        public TheAbstractInlineFunctor1Inline(String termAtom, Function<Term, Term> ff) {
//            super(termAtom, ff);
//        }
//    }

}
