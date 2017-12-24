package nars.term;

import jcog.Util;
import jcog.util.TriFunction;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.PermanentConcept;
import nars.concept.builder.ConceptBuilder;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.pred.AbstractPred;
import nars.term.sub.Subterms;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.Null;
import static nars.term.Terms.atomOrNull;
import static nars.term.atom.Atomic.the;

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
        return atomOrNull(the(termAtom));
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
        return f(termAtom, (tt) -> {
            if (tt.subs() != arityRequired)
                return null;
            //throw new RuntimeException(termAtom + " requires " + arityRequired + " arguments: " + Arrays.toString(tt));

            return ff.apply(tt);
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
        return f0(fName, () -> new AbstractPred<Object>($.inst($.quote(Util.uuid64()), fName)) {

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

    /**
     * two argument functor (convenience method)
     */
    public static LambdaFunctor f3(@NotNull Atom termAtom, @NotNull TriFunction<Term, Term, Term, Term> ff) {
        return f(termAtom, 3, (tt) -> ff.apply(tt.sub(0), tt.sub(1), tt.sub(2)));
    }

    /**
     * two argument non-variable integer functor (convenience method)
     */
    @FunctionalInterface
    public interface IntIntFunc {
        int apply(int x, int y);
    }

    public static Concept f2Int(@NotNull String termAtom, @NotNull IntIntFunc ff) {
        return f2(fName(termAtom), (xt, yt) -> {
            try {
                return $.the(ff.apply($.intValue(xt), $.intValue(yt)));
            } catch (NumberFormatException ignored) {
                return null;
            }
        });
    }


    public static final class LambdaFunctor extends Functor {

        @NotNull
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

}
