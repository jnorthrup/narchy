package nars.op;

import nars.$;
import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Int;
import nars.term.functor.SimpleBinaryFunctor;

import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;

/**
 * general purpose comparator: cmp(x, y, x.compareTo(y))
 */
public class Cmp extends SimpleBinaryFunctor {

    public final static Functor cmp = new Cmp();

    static final Int zero = Int.the(0);

    private Cmp() {
        super("cmp");
    }

    @Override
    protected Term apply3(Evaluation e, Term x, Term y, Term xy) {


        boolean xVar = x instanceof Variable, yVar = y instanceof Variable;

        if (xy.equals(zero)) {

            if (xVar==yVar) {
                if (x.equals(y)) {
                    return True; //obvious
                } else {
                    return Equal.the(x, y); //reduce to equal
                }
            } else if (xVar) {
                return e.is(x, y) ? True : Null;
            } else { //if (yVar) {
                return e.is(y, x) ? True : Null;
            }
        }

        if (xy instanceof Variable) {
            if (x.equals(y)) {
                return e.is(xy, zero) ? null : Null;
            }
            if (xVar || yVar || x.hasVars() || y.hasVars())
                return null; //indeterminate

            int c = x.compareTo(y);
            if (e.is(xy, Int.the(c))) {
                    if (c > 0) {
                        return swap(x, y, c);
                    } else {
                  return null;
                    }
            } else {
                return Null; //conflict with the correct value
            }
        }

        if (!x.hasVars() && !y.hasVars() && !xy.hasVars()) {
                //check for truth, and canonical ordering
                int c = x.compareTo(y);
                if (((Int) xy).i != c)
                    return Null;
                if (c > 0) {
                    //swap parameters
                    return swap(x, y, c);
                } else
                    return null;

        }


        return super.apply3(e, x, y, xy);
    }

    private static Term swap(Term x, Term y, int c) {
        return $.func(cmp, y, x, Int.the(-c));
    }

    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        if (x.equals(y))
            return Int.the(0);
        if (!x.hasVars() && !y.hasVars()) {
            return Int.the(x.compareTo(y));
        } else {
            return null;
        }
    }

}
