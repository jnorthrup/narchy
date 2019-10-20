package nars.op;

import nars.$;
import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Int;
import nars.term.functor.SimpleBinaryFunctor;

import static nars.term.atom.Bool.*;

/**
 * general purpose comparator: cmp(x, y, x.compareTo(y))
 */
public class Cmp extends SimpleBinaryFunctor {

    public static final Functor cmp = new Cmp();

    static final Int Zero = Int.the(0);

    private Cmp() {
        super("cmp");
    }

    private static Term swap(Term x, Term y, int c) {
        return $.func(cmp, y, x, Int.the(-c));
    }

    @Override
    protected Term apply3(Evaluation e, Term x, Term y, Term xy) {

        if (xy.equals(Zero))
            return Equal.the(e, x, y);


        if (xy instanceof Variable) {
            //assign answer
            if (x.equals(y))
                return e.is(xy, Zero) ? null : Null;

            boolean xVar = x instanceof Variable, yVar = y instanceof Variable;

            if (xVar || yVar || x.hasVars() || y.hasVars())
                return null; //indeterminate

            var c = x.compareTo(y);
            if (e.is(xy, Int.the(c))) {
                return c > 0 ? swap(x, y, c) : null;
            } else
                return Null; //conflict with the correct value
        } else {
            if (!(xy instanceof Int))
                return Null;
            if (!x.hasVars() && !y.hasVars()) {
                //check for truth, and canonical ordering
                var c = x.compareTo(y);
                //                if (c > 0)
                //                    return swap(x, y, c);
                //                else
                return ((Int) xy).i != c ? False : True;

            } else if (x instanceof Variable || y instanceof Variable) {
                var c = x.compareTo(y);
                if (c > 0)
                    return swap(x, y, c);
            }
        }

        return super.apply3(e, x, y, xy);
    }

    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        if (x.equals(y))
            return Int.the(0);
        return !x.hasVars() && !y.hasVars() ? Int.the(x.compareTo(y)) : null;
    }

}
