package nars.term.functor;

import nars.term.util.transform.InlineFunctor;

abstract public class InlineCommutiveBinaryBidiFunctor extends CommutiveBinaryBidiFunctor implements InlineFunctor {

    protected InlineCommutiveBinaryBidiFunctor(String name) {
        super(name);
    }
}
