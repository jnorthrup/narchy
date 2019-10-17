package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.util.transform.InlineFunctor;

public abstract class InlineCommutiveBinaryBidiFunctor extends CommutiveBinaryBidiFunctor implements InlineFunctor<Evaluation> {

    protected InlineCommutiveBinaryBidiFunctor(String name) {
        super(name);
    }
}
