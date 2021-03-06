package nars.term.functor;

import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import nars.term.util.transform.InstantFunctor;

import java.util.function.UnaryOperator;

public abstract class AbstractInlineFunctor1 extends AbstractInlineFunctor {

    protected AbstractInlineFunctor1(String atom) {
        super(atom);
    }

    protected abstract Term apply1(Term arg);

    @Override
    public final Term applyInline(Subterms args) {
        if (args.subs() != 1) return IdempotentBool.Null;
        return apply1(args.sub(0));
    }

    @Override
    public final Term apply(Evaluation e, Subterms terms) {
        return terms.subs() != 1 ? IdempotentBool.Null : apply1(terms.sub(0));
    }

    public static class MyAbstractInlineFunctor1Inline extends AbstractInlineFunctor1 {
        final UnaryOperator<Term> ff;

        public MyAbstractInlineFunctor1Inline(String termAtom, UnaryOperator<Term> ff) {
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
