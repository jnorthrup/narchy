package nars.op;

import jcog.Util;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Solution;
import nars.term.Term;
import nars.term.Terms;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.Op.PROD;

public enum ListFunc { ;

    /**
     * emulates prolog append/3
     */
    public final static Functor append = Functor.f2Or3("append", (Term[] aa) -> {
        Term x = aa[0];
        Term y = aa[1];

        if (aa.length == 2) {
            if (x.op().var || y.op().var) {
                return null; //uncomputable; no change
            } else {
                return _append(x, y);
            }
        } else {
            Term _xy = aa[2];

            if (_xy.op().var) {
                //forwards
                if (x.op().var || y.op().var) {
                    return null; //uncomputable; no change
                } else {
                    return Solution.solve(s -> {
                        Term XY = _append(x, y);
                        s.replace(_xy, XY);
                    });
                }
            } else {
                //backwards

                Term xy;
                if (_xy.op() != PROD)
                    xy = $.pFast(_xy);
                else
                    xy = _xy;

                Subterms xys = xy.subterms();
                Term xx;
                if (x.op() != PROD)
                    xx = $.pFast(x);
                else
                    xx = x;


                if (x.op().var && !y.op().var) {
                    //solve HEAD

                    Term yy;
                    if (y.op() != PROD)
                        yy = $.pFast(y);
                    else
                        yy = y;

                    int ys = yy.subs();

                    int remainderLength = xy.subs() - ys;
                    if (remainderLength >= 0) {
                        if (yy.subterms().ANDwith((yi, yii) -> xy.sub(remainderLength + yii).equals(yi))) {
                            //the suffix matches
                            if (remainderLength == 0)
                                return Solution.solve(s -> s.replace(x, Op.EmptyProduct));
                            else
                                return Solution.solve(s ->
                                        s.replace(x, $.pFast(xys.terms((i, ii) -> i < ys)))
                                );
                        }
                    }
                    return Null; //impossible
                } else if (!x.op().var && y.op().var) {
                    //solve TAIL
                    int xs = xx.subs();
                    int remainderLength = xy.subs() - xs;
                    if (remainderLength >= 0) {
                        if (xx.subterms().ANDwith((xi, xii) -> xy.sub(xii).equals(xi))) {
                            //the prefix matches
                            if (remainderLength == 0)
                                return Solution.solve(s -> s.replace(y, Op.EmptyProduct));
                            else
                                return Solution.solve(s ->
                                        s.replace(y, $.pFast(xys.terms((i, ii) -> i >= xs)))
                                );
                        }
                    }
                    return Null; //impossible

                } else if (x.op().var && y.op().var) {

                    int l = xy.subs();
                    if (l == 0) {
                        return Solution.solve(s ->
                                s.replace(
                                        x, Op.EmptyProduct,
                                        y, Op.EmptyProduct)
                        );
                    } else if (l == 1) {
                        return Solution.solve(s ->
                                s.replace(
                                        s.subst(
                                                x, Op.EmptyProduct,
                                                y, xy),
                                        s.subst(
                                                x, xy,
                                                y, Op.EmptyProduct)
                                )
                        );
                    } else {
                        return Solution.solve(s ->
                                s.replace(
                                        Util.map(-1, l, finalI ->
                                                        s.subst(
                                                                x, $.pFast(xys.terms((xyi, ii) -> xyi <= finalI)),
                                                                y, $.pFast(xys.terms((xyi, ii) -> xyi > finalI)))
                                                ,
                                                Predicate[]::new
                                        )
                                ));
                    }
                } else {
                    if (x.equals(EmptyProduct) && y.equals(xy)) return null; //true, unchanged
                    if (y.equals(EmptyProduct) && x.equals(xy)) return null; //true, unchanged

                    //Term[] xx = x.op() == PROD ? x.subterms().arrayShared() : new Term[]{x};
                    Term[] yy = y.op() == PROD ? y.subterms().arrayShared() : new Term[]{y};
                    if (_xy.equals($.pFast(Terms.concat(xx.subterms().arrayShared(), yy)))) {
                        return null; //true, unchanged
                    } else {
                        return False;
                    }
                }

            }
        }
    });

    private static Term _append(Term x, Term y) {
        Term[] xx = x.op() == PROD ? x.subterms().arrayShared() : new Term[]{x};
        Term[] yy = y.op() == PROD ? y.subterms().arrayShared() : new Term[]{y};
        return $.pFast(Terms.concat(xx, yy));
    }

}
