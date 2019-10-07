package nars.term;

import jcog.Util;
import jcog.func.TriFunction;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.atom.AbstractAtomic;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.functor.AbstractInlineFunctor;
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
abstract public class Functor extends AbstractAtomic implements BiFunction<Evaluation, Subterms, Term>, Term {

    protected Functor(String atom) {
        this(fName(atom));
    }

    protected Functor(Atom atom) {
        super(atom.bytes());
    }


    @Override
    public final Op op() {
        return ATOM;
    }

    @Deprecated public static Subterms args(Term x) {
        return args((Compound)x);
    }
    @Deprecated public static Term[] argsArray(Term x) {
        return args(x).arrayShared();
    }

    /**
     * returns the arguments of an operation (task or target)
     */
    public static Subterms args(Compound x) {
        assert (x.op() == INH && x.subIs(1, ATOM));
        return x.sub(0).subterms();
    }

    @Nullable public static Subterms args(Compound x, int requireArity) {
        Subterms s = args(x);
        return s.subs()==requireArity ?  s : null;
    }

    public static Atomic func(Term x) {
        return isFunc(x) ? (Atomic) x.sub(1) : Bool.Null;
    }

    public static boolean isFunc(Term x) {
        if (x instanceof Compound && x.op() == INH && x.hasAll(Op.FuncBits)) {
            Subterms xx = x.subterms();
            return xx.subIs(0, PROD) && xx.subIs(1, ATOM);
        }
        return false;
    }

    protected static Atom fName(String termAtom) {
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
    public static LambdaFunctor f(Atom termAtom, Function<Subterms, Term> f) {
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
        return f1(fName(termAtom), /*safeFunctor*/(ff));
    }

    public static AbstractInlineFunctor f1Inline(String termAtom, Function<Term, Term> ff) {
        return new AbstractInlineFunctor1.MyAbstractInlineFunctor1Inline(termAtom, ff);
    }


    public static AbstractInlineFunctor f2Inline(String termAtom, BiFunction<Term, Term, Term> ff) {
        return new AbstractInlineFunctor2.MyAbstractInlineFunctor2Inline(termAtom, ff);
    }

    public static <X extends Term> LambdaFunctor f1Const(String termAtom, Function<X, Term> ff) {
        return f1(fName(termAtom), (Term x) ->
                ((x == null) || x.hasVars()) ? null : ff.apply((X) x)
        );
    }

    /**
     * a functor involving a concept resolved by the 1st argument target
     */
    public static LambdaFunctor f1Concept(String termAtom, NAR nar, BiFunction<Concept, NAR, Term> ff) {
        return f1(fName(termAtom), t -> {
            Concept c = nar.conceptualizeDynamic(t);
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


//    private static Function<Term, Term> safeFunctor(Function<Term, Term> ff) {
//        return x -> x == null ? null : ff.apply(x);
//    }

    /**
     * three argument functor (convenience method)
     */
    public static LambdaFunctor f3(Atom termAtom, TriFunction<Term, Term, Term, Term> ff) {
        return f(termAtom, 3, (tt) -> ff.apply(tt.sub(0), tt.sub(1), tt.sub(2)));
    }

    public static LambdaFunctor f2Or3(String termAtom, Function<Subterms, Term> ff) {
        return f(fName(termAtom), 2, 3, ff::apply);
    }

}
