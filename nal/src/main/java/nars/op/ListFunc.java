package nars.op;

import jcog.TODO;
import jcog.version.VersionMap;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Int;
import nars.term.util.Conj;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
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
                return e.is(x, Op.EmptyProduct,y, Op.EmptyProduct) ? null : Null;
            } else if (l == 1) {
                return e.is(x, Op.EmptyProduct, y, xy) && e.is(x, xy, y, Op.EmptyProduct) ? null : Null;
            } else {
                Subterms xys = xy.subterms();

                Collection<Predicate<VersionMap<Term,Term>>> OR = IntStream.range(-1, l).mapToObj(finalI ->
                        e.assign(
                                x, $.pFast(xys.terms((xyi, ii) -> xyi <= finalI)),
                                y, $.pFast(xys.terms((xyi, ii) -> xyi > finalI)))
                ).collect(toList());

                e.isAny( OR );

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
                        return e.is(x, Op.EmptyProduct) ? null : Null;
                    } else {
                        return e.is(x, $.pFast(xy.subterms().terms((i, ii) -> i < ys))) ? null : Null;
                    }
                }
            }
            return y.hasAny(Op.varBits) || xy.hasAny(Op.varBits) ? null : Null;
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
                        return e.is(y, Op.EmptyProduct) ? null : Null;
                    } else {
                        return e.is(y, $.pFast(xy.subterms().terms((i, ii) -> i >= xs))) ? null : Null;
                    }
                }
            }
            return x.hasAny(Op.varBits) || xy.hasAny(Op.varBits) ? null : Null;
        }
    };


    public static Functor reverse = new Functor.UnaryBidiFunctor("reverse") {

        @Override
        protected Term compute(Term x) {
            Op o = x.op();
            switch (o) {
                case PROD:
                    if (x.subs() > 1)
                        return PROD.the(x.subterms().reversed());
                    break;
                case INH:
                case IMPL:
                    return o.the(x.dt(),x.subterms().reversed());
                case CONJ:
                    int dt = x.dt();
                    if (!Conj.concurrent(dt))
                        return x.dt(-dt);
                    break;
            }
            return null;
        }

        @Override
        protected Term uncompute(Term x, Term y) {
            return compute(y);
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
