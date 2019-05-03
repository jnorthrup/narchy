package nars.op;

import nars.$;
import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.functor.InlineCommutiveBinaryBidiFunctor;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INT;
import static nars.term.atom.Bool.*;
import static nars.term.functor.CommutiveBinaryBidiFunctor.commute;

public enum MathFunc { ;




    public final static Functor add =
            new ArithmeticCommutiveBinaryBidiFunctor("add") {

                @Override
                @Nullable protected Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {

                    if (!(xi && yi) && x.equals(y))
                        return $.func((Atomic) mul.term, x, Int.TWO);

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
                            case 1: return y; 
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
                        return Null; 
                    else
                        return Int.the(xy/xx); 
                }
            };

    public static Term add(Term x, Term y) {
        boolean xInt = x.op() == INT;
        if (xInt) {
            int X = ((Int) x).i;
            if (X == 0) return y;
        }
        if (y.op()==INT) {
            int Y = ((Int) y).i;
            if (Y == 0) return x;
            if (xInt && ((Int) x).i ==Y) return mul(x, Int.the(2));
        }


        return $.func(add, commute(x, y));
    }

    public static Term mul(Term x, Term y) {
        if (x.op()==INT) {
            int X = ((Int) x).i;
            if (X == 0) return Int.ZERO;
            if (X == 1) return y;
        }

        if (y.op()==INT) {
            int Y = ((Int) y).i;
            if (Y == 0) return Int.ZERO;
            if (Y == 1) return x;
        }

        return $.func(mul, commute(x, y));
    }

    /** TODO abstract CommutiveBooleanBidiFunctor */
    public static final class XOR extends InlineCommutiveBinaryBidiFunctor implements The {

        public static final XOR the = new XOR();

        private XOR() {
            super("xor");
        }

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x instanceof Bool && y instanceof Bool && x!=Null && y!=Null) {
                return x!=y ? True : False;
            }
            return null;
        }

        @Override
        protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
            return null;
        }

        @Override
        protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
            if (y instanceof Bool && xy instanceof Bool && y!=Null && xy!=null) {
                //TODO assert that if x is not a Bool, it will evaluate to True or False according to xy
            }
            return null;
        }
    }


    abstract static class ArithmeticCommutiveBinaryBidiFunctor extends InlineCommutiveBinaryBidiFunctor implements The /* THE */ {

        ArithmeticCommutiveBinaryBidiFunctor(String name) {
            super(name);
        }

        @Override
        public Term applyInline(Subterms args) {
            return args.subs() == 2 && args.AND(x -> x.op()==INT) ? super.applyInline(args) : null;
        }

        abstract protected Term compute(int xx, int yy);
        abstract protected Term uncompute(int xy, int xx);

        @Override
        protected Term apply2(Evaluation e, Term x, Term y) {
            return compute(e, x, y);
        }

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {

            boolean xi = x.op() == INT;
            int xx = xi ? ((Int) x).i : Integer.MIN_VALUE;
            boolean yi = y.op() == INT;
            int yy = yi ? ((Int) y).i : Integer.MIN_VALUE;

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
                if (y.compareTo(x) < 0) {
                    
                    Term t = x;
                    x = y;
                    y = t;

                    changed = true;
                }

                




                return changed ? $.func((Atomic)term, x, y) : null;

            }
        }

        /** return non-null value to return a specific result */
        @Nullable Term preFilter(Term x, int xx, boolean xi, Term y, int yy, boolean yi) {
            return null;
        }

        @Override
        protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {
            return null; 
        }

        @Override
        protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {
            if (y.op()==INT && xy.op()==INT) {
                int XY = ((Int)xy).i;
                int Y = ((Int)y).i;
                if (Y == 0) return xy;

                Term X = uncompute(XY, Y);

                return e.is(x, X) ? null : Null;
            }
            return null; 
        }


    }

}
