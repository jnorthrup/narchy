package nars.op;

import nars.Op;
import nars.eval.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Int;
import nars.term.functor.SimpleBinaryFunctor;

import static nars.Op.INT;
import static nars.term.atom.Bool.*;

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
        Op xyo = xy.op();

        boolean xVar = x instanceof Variable, yVar = y instanceof Variable;

        if (xy.equals(zero)) {

            if ((!xVar && !yVar) || (xVar && yVar)) {
                if (x.equals(y)) {
                    return True; //obvious
                } else {
                    return Equal.the(x, y);
                }
            } else if (xVar) {
                return e.is(x, y) ? True : Null;
            } else if (yVar) {
                return e.is(y, x) ? True : Null;
            }
        }

        if (xVar && yVar)
            return null; //nothing to do

        if (!x.hasVars() && !y.hasVars()) {

            int c = x.compareTo(y);

            if (xyo == INT) {
                if (c != ((Int) xy).id)
                    return False;
                else {
//                        if (c > 0) {
//                            //just in case
//                            if (!e.is($.func(cmp, x, y, xy), reverse(x, y, c)))
//                                return Null;
//                        }
                    return True;
                }
            } else if (!xy.hasVars())
                return Null; //some nonsense non-integer constant

            if (xyo.var) {
                if (e.is(xy, Int.the(c))) {
//                    if (c > 0) {
//                        return reverse(x, y, c);
//                    } else {
                        return null;
//                    }
                } else {
                    return Null; //conflict with the correct value
                }
            }
        }

        return super.apply3(e, x, y, xy);
    }

//    private Term reverse(Term x, Term y, int c) {
//        return $.func(cmp, y, x, Int.the(-c));
//    }

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
