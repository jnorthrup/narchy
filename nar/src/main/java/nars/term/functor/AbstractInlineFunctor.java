package nars.term.functor;

import nars.term.Functor;
import nars.term.util.transform.InlineFunctor;

public abstract class AbstractInlineFunctor extends Functor implements InlineFunctor {

    AbstractInlineFunctor(String atom) {
        super(atom);
    }
}
