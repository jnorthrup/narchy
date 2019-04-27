package nars.term.functor;

import nars.eval.Evaluation;
import nars.term.Term;
import nars.term.atom.Atom;

public abstract class CommutiveBinaryBidiFunctor extends BinaryBidiFunctor {


    CommutiveBinaryBidiFunctor(String name) {
        super(name);
    }

    public CommutiveBinaryBidiFunctor(Atom atom) {
        super(atom);
    }

    public static Term[] commute(Term x, Term y) {
        return x.compareTo(y) <= 0 ? new Term[]{x, y} : new Term[]{y, x};
    }


    @Override
    protected Term apply2(Evaluation e, Term x, Term y) {
        if (x.compareTo(y) > 0) {
            //return $.func((Atomic) target(), y, x);
            Term z = x;
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
