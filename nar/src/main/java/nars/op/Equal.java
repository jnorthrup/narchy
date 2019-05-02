package nars.op;

import jcog.TODO;
import nars.$;
import nars.Op;
import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Int;
import nars.term.functor.InlineCommutiveBinaryBidiFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INH;
import static nars.Op.INT;
import static nars.term.atom.Bool.*;

public final class Equal extends InlineCommutiveBinaryBidiFunctor implements The {

    public static final Equal the = new Equal();

    private Equal() {
        super("equal");
    }

    public static Term the(Term x, Term y) {
        @Nullable Term p = pretest(x, y);
        if (p!=null)
            return p;
        else
            return $.func(the, commute(x, y));
    }

    @Override
    final protected Term apply2(Evaluation e, Term x, Term y) {
        return compute(e, x, y);
    }

    @Override
    public Term applyInline(Subterms args) {
        if (args.subs() == 2) {

            Term x = args.sub(0), y = args.sub(1);

            return pretest(x, y);
        }
        //TODO support N-ary equality
        return null;
    }


    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        Term p = pretest(x, y);
        if (p != null)
            return p;

        Op xOp = x.op(), yOp = y.op();
        boolean xIsVar = xOp.var, yIsVar = yOp.var;

        if (xIsVar ^ yIsVar) {

            if (xIsVar) {
//                if (e != null) {
                    return e.is(x, y) ? True : Null;
//                }
            } else {
//                if (e != null) {
                    return e.is(y, x) ? True : Null;
//                }
            }

            //indeterminable in non-evaluation context
//            return null;
        }


        boolean xHasVar = xIsVar || x.hasVars(), yHasVar = yIsVar || y.hasVars();
        if (yHasVar && !xHasVar) {
            //swap
            Term z = x;
            x = y;
            y = z;
            xHasVar = true;
            yHasVar = false;
            Op zOp = xOp;
            xOp = yOp;
            yOp = zOp;
        }


        if (xHasVar && xOp == INH && yOp==INT) {
            //algebraic solutions TODO use symbolic algebra system
            Term xf = Functor.func(x);
            if (xf.equals(MathFunc.add)) {
                Subterms xa = Functor.args((Compound)x, 2);
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (xa0.op().var && xa1.op() == INT)
                    return e.is(xa0, Int.the(((Int) y).id - ((Int) xa1).id)) ? True : Null; //"equal(add(#x,a),b)"
                else if (xa1.op().var && xa0.op() == INT)
                    throw new TODO();
            } else if (xf.equals(MathFunc.mul)) {
                Subterms xa = Functor.args((Compound)x, 2);
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (xa0.op().var && xa1.op() == INT)
                    return e.is(xa0, $.the(((double)((Int) y).id) / ((Int) xa1).id)) ? True : Null; //"equal(mul(#x,a),b)"
            }
        }


        if (xHasVar || yHasVar) {
            //indeterminable
            return null;
        } else {
            return False;
        }

    }

    @Nullable
    private static Term pretest(Term x, Term y) {
        if (x == Null || y == Null) return Null;
        if (x.equals(y)) return True;
        if (x.equalsNeg(y)) return False;
        if (x == True) return y;
        if (y == True) return x;
        if (x == False) return y.neg();
        if (y == False) return x.neg();
        if (!x.hasVars() && !y.hasVars()) return False; //constant in-equal
        return null;
    }

    @Override
    protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
        return null;
    }

    @Override
    protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
        return xy == True ? y : null;
    }

    public static Term cmp(Term a, Term b, int c) {
        if (a.compareTo(b) > 0) {
            c *= -1;
            Term x = a;
            a = b;
            b = x;
        }

        return $.func(Cmp.cmp, a, b, Int.the(c));
    }


}
