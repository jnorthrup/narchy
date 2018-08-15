package nars.op;

import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Evaluation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Int;

import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Prolog contains its own library of list programs, such as:
 * append(X, Y, Z)
 * appending
 * Y
 *  onto
 * X
 * gives
 * Z
 * reverse(X, Y)
 * reverse of
 * X
 *  is
 * Y
 * length(X, N)
 * length of
 * X
 *  is
 * N
 * member(U, X)
 * U
 *  is in
 * X
 * non_member
 * (U, X)
 * U
 *  is not in
 * X
 * sort(X, Y
 * )
 * sorting
 * X
 *  gives
 * Y
 */
public enum ListFunc {
    ;

    /**
     * emulates prolog append/3
     */
    public final static Functor append = new Functor.BinaryBidiFunctor("append") {

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            Term[] xx = x.op() == PROD ? x.subterms().arrayShared() : new Term[]{x};
            if (xx.length == 0) return y;
            Term[] yy = y.op() == PROD ? y.subterms().arrayShared() : new Term[]{y};
            if (yy.length == 0) return x;
            return $.pFast(Terms.concat(xx, yy));
        }

        @Override
        protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {

            int l = xy.subs();
            if (l == 0) {
                e.replace(
                        x, Op.EmptyProduct,
                        y, Op.EmptyProduct
                );
                return null;
            } else if (l == 1) {
                e.replace(
                        Evaluation.subst(
                                x, Op.EmptyProduct,
                                y, xy),
                        Evaluation.subst(
                                x, xy,
                                y, Op.EmptyProduct)

                );
                return null;
            } else {
                Subterms xys = xy.subterms();
                e.replace(
                        Util.map(-1, l, finalI ->
                                        Evaluation.subst(
                                                x, $.pFast(xys.terms((xyi, ii) -> xyi <= finalI)),
                                                y, $.pFast(xys.terms((xyi, ii) -> xyi > finalI)))
                                ,
                                Predicate[]::new

                        ));
                return null;
            }

        }

        @Override
        protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {

            Term yy;
            if (y.op() != PROD)
                yy = $.pFast(y);
            else
                yy = y;

            int ys = yy.subs();

            int remainderLength = xy.subs() - ys;
            if (remainderLength >= 0) {
                if (yy.subterms().ANDwith((yi, yii) -> xy.sub(remainderLength + yii).equals(yi))) {
                    if (remainderLength == 0) {
                        e.replace(x, Op.EmptyProduct);
                        return null;
                    } else {
                        e.replace(x, $.pFast(xy.subterms().terms((i, ii) -> i < ys)));
                        return null;
                    }
                }
            }
            return y.hasAny(Op.varBits) || xy.hasAny(Op.varBits) ?
                    null
                    :
                    Null;
        }

        @Override
        protected Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy) {

            Term xx;
            if (x.op() != PROD)
                xx = $.pFast(x);
            else
                xx = x;

            int xs = xx.subs();
            int remainderLength = xy.subs() - xs;
            if (remainderLength >= 0) {
                if (xx.subterms().ANDwith((xi, xii) -> xy.sub(xii).equals(xi))) {

                    if (remainderLength == 0) {
                        e.replace(y, Op.EmptyProduct);
                        return null;
                    } else {
                        e.replace(y, $.pFast(xy.subterms().terms((i, ii) -> i >= xs)));
                        return null;
                    }
                }
            }
            return x.hasAny(Op.varBits) || xy.hasAny(Op.varBits) ?
                    null
                    :
                    Null;
        }
    };


    public static Functor sub = Functor.f2("sub", (x, n) -> {
        if (n.op() == INT) {
            return x.sub(((Int) n).id, Null);
        } else {
            return null;
        }
    });
    public static Functor subs = Functor.f2Or3("subs", (Term[] args) -> {
        if (args.length == 2) {

            Term x = args[0];
            Term n = args[1];
            if (n.op() == INT) {
                int nn = ((Int) n).id;
                Subterms xx = x.subterms();
                int m = xx.subs();
                if (nn < m) {
                    return PROD.the(xx.toArraySubRange(nn, m));
                } else {
                    return Null;
                }
            } else {
                return null;
            }
        } else {
            throw new TODO();
        }
    });
}
