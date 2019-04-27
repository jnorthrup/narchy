package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.util.transform.InlineFunctor;

public abstract class AbstractInlineFunctor extends Functor implements InlineFunctor<Evaluation> {

    AbstractInlineFunctor(String atom) {
        super(atom);
    }
}
