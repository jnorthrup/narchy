package nars.op;

import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Int;

import static nars.Op.INT;

public enum MathFunc { ;

    public final static Functor _add =
            Functor.f2Int("add", true, (i) -> i == 0, (n) -> false, (x, y) -> x + y);

    public final static Functor add =
            new Functor.CommutiveBinaryBidiFunctor("add") {

                @Override
                protected Term apply2(Term x, Term y) {
                    if (x.op()==INT && ((Int)x).id==0) return y; //identity
                    if (y.op()==INT && ((Int)y).id==0) return x; //identity
                    return super.apply2(x, y);
                }

                @Override
                protected Term compute(Term x, Term y) {
                    if (x.op()==INT && y.op()==INT) {

                        int xx = ((Int) x).id;
                        if (xx == 0) return y;

                        int yy = ((Int) y).id;
                        if (yy == 0) return x;

                        return Int.the(xx + yy);

                    } else {
                        return null; //uncomputable; unchanged
                    }
                }

                @Override
                protected Term computeFromXY(Term x, Term y, Term xy) {
                    return null; //infinite possibilities, dont bother
                }

                @Override
                protected Term computeXfromYandXY(Term x, Term y, Term xy) {
                    if (y.op()==INT && xy.op()==INT) {
                        int XY = ((Int)xy).id;
                        int Y = ((Int)y).id;
                        if (Y == 0) return xy;

                        int X = XY - Y;
                        return Evaluation.solve(s->
                            s.replace(x, Int.the(X))
                        );
                    }
                    return null; //uncomputable; unchanged
                }

            };

    public final static Functor mul =
            Functor.f2Int("mul", true, (i) -> i == 1, (n) -> n == 0, (x, y) -> x * y);

}
