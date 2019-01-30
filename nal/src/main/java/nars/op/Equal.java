package nars.op;

import nars.$;
import nars.Op;
import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Int;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INT;
import static nars.term.atom.Bool.*;

public final class Equal extends Functor.InlineCommutiveBinaryBidiFunctor implements The {

    public static final Equal the = new Equal();

    private Equal() {
        super("equal");
    }

    public static Term the(Term x, Term y) {
        return $.func(the, x, y);
    }

    @Override
    final protected Term apply2(Evaluation e, Term x, Term y) {
        return compute(e, x, y);
    }

    @Override
    public Term applyInline(Subterms args) {
        if (args.subs() == 2) {

            Term x = args.sub(0), y = args.sub(1);

            Term p = pretest(x, y);
            if (p != null)
                return p;

            if (!x.hasVars() && !y.hasVars())
                return False; //constant in-equal

        }
        //TODO support N-ary equality
        return null;
    }


    @Override
    protected Term compute(Evaluation e, Term x, Term y) {
        Term p = pretest(x, y);
        if (p != null)
            return p;

        boolean xHasVar = x.hasVars();
        boolean yHasVar = y.hasVars();
        if (xHasVar || yHasVar) {
            //algebraic solutions TODO use symbolic algebra system
            Term xf = Functor.func(x);
            if (xf.equals(MathFunc.add)) {
                Term[] xa = Functor.funcArgsArray(x);
                if (xa.length == 2 && xa[1].op() == INT && xa[0].op().var) {
                    if (y.op() == INT) {
                        //"equal(add(#x,a),b)"
                        return e.is(xa[0], Int.the(((Int) y).id - ((Int) xa[1]).id)) ? True : Null;
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
        } else if (xHasVar || yHasVar) {
            //indeterminable
            return null;
        } else {
            return False;
        }

    }

    @Nullable
    private Term pretest(Term x, Term y) {
        /** null != null, like NaN!=NaN .. it represents an unknokwn or invalid value.  who can know if it equals another one */
        if (x == Null || y == Null)
            return Null;

        if (x.equals(y))
            return True; //fast equality pre-test
        if (x.equalsNeg(y))
            return False;

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

    /**
     * general purpose comparator: cmp(x, y, x.compareTo(y))
     */
    public final static Functor cmp = new Functor.SimpleBinaryFunctor("cmp") {

        final Int zero = Int.the(0);

        @Override
        protected Term apply3(Evaluation e, Term x, Term y, Term xy) {
            Op xyo = xy.op();
            if (xyo==INT && xy.equals(zero)) {
                if (x.op() == INT && y.op().var) {
                    return e.is(y, x) ? True : Null;
                } else if (x.op().var && y.op()==INT) {
                    return e.is(x, y) ? True : Null;
                }
            }

            if (!x.hasVars() && !y.hasVars()) {
                int c = x.compareTo(y);

                if (x.op() == INT && y.op() == INT && xy.op()==INT) {
                    return (Integer.compare(((Int) x).id, ((Int) y).id) == ((Int) xy).id) ? True : False;
                }

                if (c == 0) {

                    if (!xy.op().var && !xy.equals(zero))
                        return False; //incorrect value
                    else {
                        return e.is(xy, zero) ? True : Null;
                    }
                } else if (c > 0) {

                    //TODO needs eval context to be remapped
//                    //swap argument order
//                    Term s = x;
//                    x = y;
//                    y = s;
//
//                    if (xyo == INT) {
//                        int xyi = -((Int) xy).id;
//                        if (x.op() == INT && y.op() == INT) {
//                            return (Integer.compare(((Int) x).id, ((Int) y).id) == xyi) ? True : False;
//                        }
//                        xy = Int.the(xyi);
//                    } else if (!xyo.var) {
//                        xy = $.varDep("cmp_tmp"); //erase constant, forcing recompute
//                    }
//                    return $.func(Equal.cmp, x, y, xy);
                }
            }
            return super.apply3(e, x, y, xy);
        }

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            if (x.equals(y))
                return Int.the(0);
            if (!x.hasVars() && !y.hasVars()) {
                return Int.the(x.compareTo(y));
            } else {
                return null;
            }
        }

    };


}
