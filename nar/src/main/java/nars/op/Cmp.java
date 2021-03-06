package nars.op;

import nars.$;
import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.IdempotInt;
import nars.term.functor.SimpleBinaryFunctor;

import static nars.term.atom.IdempotentBool.*;

/**
 * general purpose comparator: cmp(x, y, x.compareTo(y))
 */
public class Cmp extends SimpleBinaryFunctor {

    public static final Functor cmp = new Cmp();

    static final IdempotInt Zero = IdempotInt.the(0);

    private Cmp() {
        super("cmp");
    }

    private static Term swap(Term x, Term y, int c) {
        return $.INSTANCE.func(cmp, y, x, IdempotInt.the(-c));
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

            int c = x.compareTo(y);
            if (e.is(xy, IdempotInt.the(c))) {
                return c > 0 ? swap(x, y, c) : null;
            } else
                return Null; //conflict with the correct value
        } else {
            if (!(xy instanceof IdempotInt))
                return Null;
            if (!x.hasVars() && !y.hasVars()) {
                //check for truth, and canonical ordering
                int c = x.compareTo(y);
                //                if (c > 0)
                //                    return swap(x, y, c);
                //                else
                return ((IdempotInt) xy).i != c ? False : True;

            } else if (x instanceof Variable || y instanceof Variable) {
                int c = x.compareTo(y);
                if (c > 0)
                    return swap(x, y, c);
            }
        }

        return super.apply3(e, x, y, xy);
    }

    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        if (x.equals(y))
            return IdempotInt.the(0);
        return !x.hasVars() && !y.hasVars() ? IdempotInt.the(x.compareTo(y)) : null;
    }

}
