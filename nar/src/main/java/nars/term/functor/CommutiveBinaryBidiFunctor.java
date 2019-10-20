package nars.term.functor;

import nars.$;
import nars.eval.Evaluation;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;

public abstract class CommutiveBinaryBidiFunctor extends BinaryBidiFunctor {


    CommutiveBinaryBidiFunctor(String name) {
        super(name);
    }

    public CommutiveBinaryBidiFunctor(Atom atom) {
        super(atom);
    }

    public static Term the(Atomic func, Term x, Term y) {
        if (x.compareTo(y) > 0) {
            var z = x;
            x = y;
            y = z;
        }
        return $.func(func, x, y);
    }

    @Override
    protected Term apply2(Evaluation e, Term x, Term y) {
        if (x.compareTo(y) > 0) {
            //return $.func((Atomic) target(), y, x);
            var z = x;
            x = y;
            y = z;
        }

        return super.apply2(e, x, y);
    }

    @Override
    protected final Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy) {
        return computeXfromYandXY(e, y, x, xy);
    }
}
