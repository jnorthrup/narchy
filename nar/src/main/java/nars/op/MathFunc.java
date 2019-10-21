package nars.op;

import nars.$;
import nars.Idempotent;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import nars.term.atom.IdempotInt;
import nars.term.functor.CommutiveBinaryBidiFunctor;
import nars.term.functor.InlineCommutiveBinaryBidiFunctor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.INT;
import static nars.term.atom.IdempotentBool.*;

public final class MathFunc{


    public static final Functor mul =
            new ArithmeticCommutiveBinaryBidiFunctor("mul") {


                @Override
                protected @Nullable Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {
                    if (xi) {
                        switch (xx) {
                            case 1:
                                return y;
                            case 0:
                                return IdempotInt.ZERO;
                        }
                    }
                    if (yi) {
                        switch (yy) {
                            case 1:
                                return x;
                            case 0:
                                return IdempotInt.ZERO;
                        }
                    }
                    return null;
                }


                @Override
                protected Term compute(int xx, int yy) {
                    return IdempotInt.the(xx * yy);
                }

                @Override
                protected Term uncompute(int xy, int xx) {
                    return xx == 0 ? Null : IdempotInt.the(xy / xx);
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
                return the(mul, x, IdempotInt.TWO);

            return null;
        }

        /** solve the result */
        @Override
        protected Term compute(int xx, int yy) {
            return IdempotInt.the(xx + yy);
        }


        /** solve one of the values given the result and the value of another */
        @Override
        protected Term uncompute(int xy, int x) {
            return IdempotInt.the(xy - x);
        }


    };

    public static Term add(Term x, Term y) {
        boolean xInt = x.op() == INT;
        if (xInt) {
            int X = ((IdempotInt) x).i;
            if (X == 0) return y;
        }
        if (y.op() == INT) {
            int Y = ((IdempotInt) y).i;
            if (Y == 0) return x;
            if (xInt && ((IdempotInt) x).i == Y) return mul(x, IdempotInt.the(2));
        }


        return CommutiveBinaryBidiFunctor.the(add, x, y);
    }

    public static Term mul(Term x, Term y) {
        if (x.op() == INT) {
            int X = ((IdempotInt) x).i;
            if (X == 0) return IdempotInt.ZERO;
            if (X == 1) return y;
        }

        if (y.op() == INT) {
            int Y = ((IdempotInt) y).i;
            if (Y == 0) return IdempotInt.ZERO;
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
            if (x instanceof IdempotentBool && y instanceof IdempotentBool && x != Null && y != Null) {
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
            if (y instanceof IdempotentBool && xy instanceof IdempotentBool && y != Null) {
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
            return args.subs() == 2 && args.AND(new Predicate<Term>() {
                @Override
                public boolean test(Term x) {
                    return x.op() == INT;
                }
            }) ? super.applyInline(args) : null;
        }

        protected abstract Term compute(int xx, int yy);

        protected abstract Term uncompute(int xy, int xx);

        @Override
        protected Term apply2(Evaluation e, Term x, Term y) {
            return compute(e, x, y);
        }

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {

            boolean xi = x.op() == INT;
            int xx = xi ? ((IdempotInt) x).i : Integer.MIN_VALUE;
            boolean yi = y.op() == INT;
            int yy = yi ? ((IdempotInt) y).i : Integer.MIN_VALUE;

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
                boolean changed = false;
                if (x.compareTo(y) > 0) {

                    Term t = x;
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
                int XY = ((IdempotInt) xy).i;
                int Y = ((IdempotInt) y).i;
                if (Y == 0) return xy;

                Term X = uncompute(XY, Y);

                return e.is(x, X) ? null : Null;
            }
            return null;
        }


    }

}
