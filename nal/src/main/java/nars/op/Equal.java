package nars.op;

import nars.The;
import nars.subterm.Subterms;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;

import static nars.Op.False;
import static nars.Op.True;

public class Equal extends Functor.InlineCommutiveBinaryBidiFunctor implements The {

    public static final Equal the = new Equal();

    private Equal() {
        super("equal");
    }

    @Override
    public Term applyInline(Subterms args) {
        if (args.subs()==2) {
            Term x = args.sub(0);
            Term y = args.sub(1);
            if (x.equals(y)) return True;
            if (x instanceof Variable || y instanceof Variable) return null;
            return False;
        }
        //TODO support N-ary equality
        return null;
    }

    @Override
    protected Term apply2(Evaluation e, Term x, Term y) {
        if (x.equals(y))
            return True; //fast equality pre-test


        boolean xVar = x.op().var;
        boolean yVar = y.op().var;
        if (xVar ^ yVar) {
            if (xVar) {
                if (e != null) {
                    e.replace(x, y);
                    //return True;
                }
                return null;
            } else {
                if (e != null) {
                    e.replace(y, x);
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
            return null;
        }

        return False;
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
