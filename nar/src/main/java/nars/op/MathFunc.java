package nars.op;

import nars.$;
import nars.Idempotent;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.theBool;
import nars.term.atom.theInt;
import nars.term.functor.CommutiveBinaryBidiFunctor;
import nars.term.functor.InlineCommutiveBinaryBidiFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INT;
import static nars.term.atom.theBool.*;

public enum MathFunc {
    ;


    public static final Functor mul =
            new ArithmeticCommutiveBinaryBidiFunctor("mul") {


                @Override
                protected @Nullable Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {
                    if (xi) {
                        switch (xx) {
                            case 1:
                                return y;
                            case 0:
                                return theInt.ZERO;
                        }
                    }
                    if (yi) {
                        switch (yy) {
                            case 1:
                                return x;
                            case 0:
                                return theInt.ZERO;
                        }
                    }
                    return null;
                }


                @Override
                protected Term compute(int xx, int yy) {
                    return theInt.the(xx * yy);
                }

                @Override
                protected Term uncompute(int xy, int xx) {
                    return xx == 0 ? Null : theInt.the(xy / xx);
                }
            };
    public static final Functor add = new ArithmeticCommutiveBinaryBidiFunctor("add") {

        @Override
        protected @Nullable Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {

            if (xi && xx == 0)
                return y;

            if (yi && yy == 0)
                return x;

            if (!(xi && yi) && x.equals(y))
                return the(mul, x, theInt.TWO);

            return null;
        }

        /** solve the result */
        @Override
        protected Term compute(int xx, int yy) {
            return theInt.the(xx + yy);
        }


        /** solve one of the values given the result and the value of another */
        @Override
        protected Term uncompute(int xy, int x) {
            return theInt.the(xy - x);
        }


    };

    public static Term add(Term x, Term y) {
        var xInt = x.op() == INT;
        if (xInt) {
            var X = ((theInt) x).i;
            if (X == 0) return y;
        }
        if (y.op() == INT) {
            var Y = ((theInt) y).i;
            if (Y == 0) return x;
            if (xInt && ((theInt) x).i == Y) return mul(x, theInt.the(2));
        }


        return CommutiveBinaryBidiFunctor.the(add, x, y);
    }

    public static Term mul(Term x, Term y) {
        if (x.op() == INT) {
            var X = ((theInt) x).i;
            if (X == 0) return theInt.ZERO;
            if (X == 1) return y;
        }

        if (y.op() == INT) {
            var Y = ((theInt) y).i;
            if (Y == 0) return theInt.ZERO;
            if (Y == 1) return x;
        }

        return CommutiveBinaryBidiFunctor.the(mul, x, y);
    }

    /**
     * TODO abstract CommutiveBooleanBidiFunctor
     */
    public static final class XOR extends InlineCommutiveBinaryBidiFunctor implements Idempotent {

        public static final XOR xor = new XOR();

        private XOR() {
            super("xor");
        }

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x instanceof theBool && y instanceof theBool && x != Null && y != Null) {
                return x != y ? True : False;
            }
            return null;
        }

        @Override
        protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
            return null;
        }

        @Override
        protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
            if (y instanceof theBool && xy instanceof theBool && y != Null) {
                //TODO assert that if x is not a Bool, it will evaluate to True or False according to xy
            }
            return null;
        }
    }


    abstract static class ArithmeticCommutiveBinaryBidiFunctor extends InlineCommutiveBinaryBidiFunctor implements Idempotent /* THE */ {

        ArithmeticCommutiveBinaryBidiFunctor(String name) {
            super(name);
        }



        @Override
        public Term applyInline(Subterms args) {
            return args.subs() == 2 && args.AND(x -> x.op() == INT) ? super.applyInline(args) : null;
        }

        protected abstract Term compute(int xx, int yy);

        protected abstract Term uncompute(int xy, int xx);

        @Override
        protected Term apply2(Evaluation e, Term x, Term y) {
            return compute(e, x, y);
        }

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {

            var xi = x.op() == INT;
            var xx = xi ? ((theInt) x).i : Integer.MIN_VALUE;
            var yi = y.op() == INT;
            var yy = yi ? ((theInt) y).i : Integer.MIN_VALUE;

            if (xi || yi) {
                var preReduction = preFilter(x, xx, xi, y, yy, yi);
                if (preReduction != null)
                    return preReduction;
            }

            if (xi && yi) {

                if (xx == 0) return y;


                if (yy == 0) return x;


                return compute(xx, yy);

            } else {
                var changed = false;
                if (x.compareTo(y) > 0) {

                    var t = x;
                    x = y;
                    y = t;

                    changed = true;
                }


                return changed ? $.func(this, x, y) : null;

            }
        }

        /**
         * return non-null value to return a specific result
         */
        @Nullable Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {
            return null;
        }

        @Override
        protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
            return null;
        }

        @Override
        protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
            if (y.op() == INT && xy.op() == INT) {
                var XY = ((theInt) xy).i;
                var Y = ((theInt) y).i;
                if (Y == 0) return xy;

                var X = uncompute(XY, Y);

                return e.is(x, X) ? null : Null;
            }
            return null;
        }


    }

}
