package nars.op;

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

    @Override
    public Term applyInline(Subterms args) {
        if (args.subs()==2) {
            Term x = args.sub(0), y = args.sub(1);

            if (x == Null || y == Null)
                return Null;

            if (x.equals(y)) return True;
            //if (x instanceof Variable || y instanceof Variable) return null; //undecidable inline
            if (x.hasVars() || y.hasVars())
                return null; //undecidable inline
            return False;
        }
        //TODO support N-ary equality
        return null;
    }

    @Override
    protected Term apply2(Evaluation e, Term x, Term y) {

        /** null != null, like NaN!=NaN .. it represents an unknokwn or invalid value.  who can know if it equals another one */
        if (x == Null || y == Null)
            return Null;

        if (x.equals(y))
            return True; //fast equality pre-test


        boolean xVar = x.op().var;
        boolean yVar = y.op().var;
        if (xVar ^ yVar) {
            if (xVar) {
                if (e != null) {
                    e.is(x, y);
                    //return True;
                }
                return null;
            } else {
                if (e != null) {
                    e.is(y, x);
                    //return True;
                }
                return null;
            }
        }


        return super.apply2(e, x, y);
    }


    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        if (x.equals(y))
            return True;

        if (x.hasVars() || y.hasVars()) {
            if (x.equalsNeg(y))
                return False; //experimental assumption: x!=--x

            //try algebraic reductions
            //HACK hardcoded but would be nice to use a symbolic algebra system
            Term xf = Functor.func(x);
            if (xf.equals(MathFunc.add)) {
                Term[] xa = Functor.funcArgsArray(x);
                if (xa.length == 2 && xa[1].op()==INT && xa[0].op().var) {
                    if (y.op()==INT) {
                        //"equal(add(#x,a),b)"
                        e.is(xa[0], Int.the( ((Int)y).id - ((Int)xa[1]).id ));
                        return True;
                    }
                }
            }

            return null;
        }

        return False; //constant
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
