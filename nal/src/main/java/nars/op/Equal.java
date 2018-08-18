package nars.op;

import nars.$;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Int;

import static nars.Op.*;

public final class Equal extends Functor.InlineCommutiveBinaryBidiFunctor implements The {

    public static final Equal the = new Equal();

    private Equal() {
        super("equal");
    }

    public static Term the(Term x, Term y) {
        return $.func(the, x, y);
    }

    @Override final protected Term apply2(Evaluation e, Term x, Term y) {
        return compute(e, x,y);
    }

    @Override
    public Term applyInline(Subterms args) {
        if (args.subs()==2) {

            Term x = args.sub(0), y = args.sub(1);

            if (x == Null || y == Null)
                return Null;

            if (x.equals(y)) return True;
            if (x.equalsNeg(y)) return False;

            if (!x.hasVars() && !y.hasVars())
                return False; //constant in-equal

        }
        //TODO support N-ary equality
        return null;
    }


    @Override
    protected Term compute(Evaluation e, Term x, Term y) {

        /** null != null, like NaN!=NaN .. it represents an unknokwn or invalid value.  who can know if it equals another one */
        if (x == Null || y == Null)
            return Null;

        if (x.equals(y))
            return True; //fast equality pre-test
        if (x.equalsNeg(y))
            return False;

        if (x.hasVars() || y.hasVars()) {
            //algebraic solutions TODO use symbolic algebra system
            Term xf = Functor.func(x);
            if (xf.equals(MathFunc.add)) {
                Term[] xa = Functor.funcArgsArray(x);
                if (xa.length == 2 && xa[1].op()==INT && xa[0].op().var) {
                    if (y.op()==INT) {
                        //"equal(add(#x,a),b)"
                        return e.is(xa[0], Int.the( ((Int)y).id - ((Int)xa[1]).id )) ? True : Null;
                    }
                }
            }

        }

        boolean xVar = x.op().var, yVar = y.op().var;
        if (xVar ^ yVar) {

            if (xVar) {
                if (e != null) {
                    return e.is(x, y) ? True : Null;
                }
            } else {
                if (e != null) {
                    return e.is(y, x) ? True : Null;
                }
            }

            //indeterminable in non-evaluation context
            return null;
        } else if (xVar && yVar) {
            //indeterminable
            return null;
        } else {
            return False;
        }

    }

    @Override
    protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
        return null;
    }

    @Override
    protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
        return xy == True ? y : null;
    }

}
