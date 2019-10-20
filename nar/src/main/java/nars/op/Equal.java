package nars.op;

import nars.$;
import nars.Op;
import nars.Idempotent;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.theInt;
import nars.term.functor.CommutiveBinaryBidiFunctor;
import nars.term.functor.InlineCommutiveBinaryBidiFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INH;
import static nars.Op.INT;
import static nars.op.Cmp.Zero;
import static nars.term.atom.theBool.*;

public final class Equal extends InlineCommutiveBinaryBidiFunctor implements Idempotent {

    public static final Equal equal = new Equal();

    private Equal() {
        super("equal");
    }

    public static Term the(Term x, Term y) {
        @Nullable var p = pretest(x, y);
        return p != null ? p : CommutiveBinaryBidiFunctor.the(equal, x, y);
    }

    private static @Nullable Term pretest(Term x, Term y) {
        if (x == Null || y == Null) return Null;
        if (x.equals(y)) return True;
        if (x.equalsNeg(y)) return False;
        if (!x.hasVars() && !y.hasVars()) return False; //constant in-equal
        return null;
    }

    public static Term cmp(Term a, Term b, int c) {
        if (a.compareTo(b) > 0) {
            c *= -1;
            var x = a;
            a = b;
            b = x;
        }

        return _cmp(a, b, c);
    }

    public static Term _cmp(Term a, Term b, int c) {
        return $.func(Cmp.cmp, a, b, theInt.the(c));
    }

    public static Term the(Evaluation e, Term x, Term y) {

        boolean xVar = x instanceof Variable, yVar = y instanceof Variable;

        if (xVar && !yVar && e.is(x,y))
            return True;
        if (yVar && !xVar && e.is(y,x))
            return True;

        return Equal.the(x, y); //reduce to equal
    }

    @Override
    protected final Term apply2(Evaluation e, Term x, Term y) {
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
        var p = pretest(x, y);
        if (p != null)
            return p;

        Op xOp = x.op(), yOp = y.op();
        boolean xIsVar = xOp.var, yIsVar = yOp.var;
        boolean xHasVar = xIsVar || x.hasVars(), yHasVar = yIsVar || y.hasVars();


        if (xIsVar && !yHasVar) {
            return e.is(x, y) ? True : Null;
        } else if (yIsVar && !xHasVar) {
            return e.is(y, x) ? True : Null;
        }


        if (x.volume() < y.volume()) {
            //swap for canonical comparison
            var z = x;
            x = y;
            y = z;
            xHasVar = true;
            yHasVar = false;
            var zOp = xOp;
            xOp = yOp;
            yOp = zOp;
        }


        if (xHasVar && xOp == INH) {
            //algebraic solutions TODO use symbolic algebra system
            Term xf = Functor.func(x);
            if (xf.equals(MathFunc.add)) {
                var xa = Functor.args((Compound) x, 2);
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (yOp == INT && xa0 instanceof Variable && xa1.op() == INT) {
                    return e.is(xa0, theInt.the(((theInt) y).i - ((theInt) xa1).i)) ? True : Null; //"equal(add(#x,a),y)"
                }

                if (xa1 instanceof Variable && xa0.equals(y)) {
                    //equal(add(#x,#y),#x) |- is(#y, 0)
                    return e.is(xa1, Zero) ? True : Null;
                }
                if (xa0 instanceof Variable && xa1.equals(y)) {
                    //equal(add(#y,#x),#x) |- is(#y, 0)
                    return e.is(xa0, Zero) ? True : Null;  //can this happen?
                }
                //includes: (#x,add(#x,#x)) |- is(#x, 0)


//                if (xa1 instanceof Variable && xa0.op() == INT) { //shouldnt be necessary if sorted in correct order
//                    throw new TODO();
//                }

            } else if (xf.equals(MathFunc.mul)) {
                var xa = Functor.args((Compound) x, 2);
                Term xa0 = xa.sub(0), xa1 = xa.sub(1);
                if (yOp == INT && xa0 instanceof Variable && xa1 instanceof theInt)
                    return e.is(xa0, $.the(((double) ((theInt) y).i) / ((theInt) xa1).i)) ? True : Null; //"equal(mul(#x,a),b)"

                if (yOp == INT && xa1 instanceof Variable && xa0 instanceof theInt)
                    return e.is(xa1, $.the(((double) ((theInt) y).i) / ((theInt) xa0).i)) ? True : Null; //"equal(mul(a,#x),b)"

                //TODO (#x,mul(#x,#y)) |- is(#y, 1)
                //TODO (#x,mul(#x,#x)) |- is(#x, 1)
            }
        }


        //indeterminable
        return xHasVar || yHasVar ? null : False;

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
