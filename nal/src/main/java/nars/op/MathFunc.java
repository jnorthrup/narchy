package nars.op;

import nars.$;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INT;
import static nars.Op.Null;

public enum MathFunc { ;

//    public final static Functor _add =
//            Functor.f2Int("add", true, (i) -> i == 0, (n) -> false, (x, y) -> x + y);

    public final static Functor add =
            new ArithmeticCommutiveBinaryBidiFunctor("add") {

                @Override
                @Nullable protected Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {

                    if (!(xi && yi) && x.equals(y))
                        return $.func("mul", x, Int.TWO); //if not fully numeric, convert add(%x,%x) to mul(%x, 2)

                    if (xi && xx == 0)
                        return y;

                    if (yi && yy == 0)
                        return x;

                    return null;
                }

                /** solve the result */
                @Override protected Term compute(int xx, int yy) {
                    return Int.the(xx + yy);
                }


                /** solve one of the values given the result and the value of another */
                @Override protected Term uncompute(int xy, int x) {
                    return Int.the(xy - x);
                }


            };

    public final static Functor mul =
            new ArithmeticCommutiveBinaryBidiFunctor("mul") {


                @Override
                @Nullable protected Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {
                    if (xi) {
                        switch (xx) {
                            case 1: return y; //identity
                            case 0: return Int.ZERO;
                        }
                    }
                    if (yi) {
                        switch (yy) {
                            case 1: return x;
                            case 0: return Int.ZERO;
                        }
                    }
                    return null;
                }


                @Override
                protected Term compute(int xx, int yy) {
                    return Int.the(xx*yy);
                }

                @Override
                protected Term uncompute(int xy, int xx) {
                    if (xx == 0)
                        return Null; //TODO some indicator of div by zero?
                    else
                        return Int.the(xy/xx); //TODO non-Integer for fractional results?
                }
            };

            //Functor.f2Int("mul", true, (i) -> i == 1, (n) -> n == 0, (x, y) -> x * y);

    abstract static class ArithmeticCommutiveBinaryBidiFunctor extends Functor.InlineCommutiveBinaryBidiFunctor {

        ArithmeticCommutiveBinaryBidiFunctor(String name) {
            super(name);
        }

        abstract protected Term compute(int xx, int yy);
        abstract protected Term uncompute(int xy, int xx);

        @Override
        protected Term apply2(Term x, Term y) {
            return compute(x, y); //skip the superclass's var check
        }

        @Override
        protected Term compute(Term x, Term y) {

            boolean xi = x.op() == INT;
            int xx = xi ? ((Int) x).id : Integer.MIN_VALUE;
            boolean yi = y.op() == INT;
            int yy = yi ? ((Int) y).id : Integer.MIN_VALUE;

            if (xi || yi) {
                Term preReduction = preFilter(x, xx, xi, y, yy, yi);
                if (preReduction != null)
                    return preReduction;
            }

            if (xi && yi) {

                if (xx == 0) return y;


                if (yy == 0) return x;


                return compute(xx, yy);

            } else {
                if (y.compareTo(x) < 0) {
                    //swap order to canonical natural ordering
                    Term t = x;
                    x = y;
                    y = t;

                }

                if (x.contains(term) || y.contains(term)) {
                    //TODO extract and re-arrange any commutive instances of this same function
                    System.out.println("simplify: " + x + "," + y);
                }

                return null;
            }
        }

        /** return non-null value to return a specific result */
        @Nullable
        protected Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {
            return null;
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

                Term X = uncompute(XY, Y);

                return Evaluation.solve(s->
                    s.replace(x, X)
                );
            }
            return null; //uncomputable; unchanged
        }


    }
}
