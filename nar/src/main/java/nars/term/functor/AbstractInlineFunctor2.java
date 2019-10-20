package nars.term.functor;

import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.theBool;

import java.util.function.BiFunction;

public abstract class AbstractInlineFunctor2 extends AbstractInlineFunctor {

    protected AbstractInlineFunctor2(Atom atom) {
        super(atom);
    }
    protected AbstractInlineFunctor2(String atom) {
        super(atom);
    }

    @Override
    public final Term apply(Evaluation e, Subterms terms) {
        return terms.subs() != 2 ? theBool.Null : apply(terms.sub(0), terms.sub(1));
    }

    protected abstract Term apply(Term a, Term b);

    public static class MyAbstractInlineFunctor2Inline extends AbstractInlineFunctor2 {
        final BiFunction<Term, Term, Term> ff;

        public MyAbstractInlineFunctor2Inline(String termAtom, BiFunction<Term, Term, Term> ff) {
            super(termAtom);
            this.ff = ff;
        }

        @Override protected final Term apply(Term a, Term b) {
            return ff.apply(a, b);
        }
    }
}
