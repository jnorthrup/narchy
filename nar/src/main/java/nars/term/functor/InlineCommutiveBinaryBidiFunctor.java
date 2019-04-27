package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.util.transform.InlineFunctor;

abstract public class InlineCommutiveBinaryBidiFunctor extends CommutiveBinaryBidiFunctor implements InlineFunctor<Evaluation> {

    protected InlineCommutiveBinaryBidiFunctor(String name) {
        super(name);
    }
}
