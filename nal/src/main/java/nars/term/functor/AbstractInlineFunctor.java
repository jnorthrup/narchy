package nars.term.functor;

import nars.term.Functor;

public abstract class AbstractInlineFunctor extends Functor implements InlineFunctor {

    AbstractInlineFunctor(String atom) {
        super(atom);
    }
}
