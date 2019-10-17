package nars.term.functor;

import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.transform.InstantFunctor;

import java.util.function.Function;

public abstract class AbstractInlineFunctor1 extends AbstractInlineFunctor {

    protected AbstractInlineFunctor1(String atom) {
        super(atom);
    }

    protected abstract Term apply1(Term arg);

    @Override
    public final Term applyInline(Subterms args) {
        if (args.subs() != 1) return Bool.Null;
        return apply1(args.sub(0));
    }

    @Override
    public final Term apply(Evaluation e, Subterms terms) {
        return terms.subs() != 1 ? Bool.Null : apply1(terms.sub(0));
    }

    public static class MyAbstractInlineFunctor1Inline extends AbstractInlineFunctor1 {
        final Function<Term, Term> ff;

        public MyAbstractInlineFunctor1Inline(String termAtom, Function<Term, Term> ff) {
            super(termAtom);
            this.ff = ff;
        }

        @Override
        public final Term apply1(Term arg) {
            return ff.apply(arg);
        }

    }

    public abstract static class AbstractInstantFunctor1 extends AbstractInlineFunctor1 implements InstantFunctor<Evaluation> {

        public AbstractInstantFunctor1(String termAtom) {
            super(termAtom);
        }
    }
}
