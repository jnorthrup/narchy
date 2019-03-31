package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.Term;

public abstract class SimpleBinaryFunctor extends BinaryBidiFunctor {

    public SimpleBinaryFunctor(String name) {
        super(name);
    }

    @Override
    protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
        return null;
    }

    @Override
    protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
        return null;
    }

    @Override
    protected Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy) {
        return null;
    }
}
