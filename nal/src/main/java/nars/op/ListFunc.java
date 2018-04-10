package nars.op;

import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Solution;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Int;

import java.util.function.Predicate;

import static nars.Op.*;

public enum ListFunc { ;

    /**
     * emulates prolog append/3
     */
    public final static Functor append = new Functor.BinaryBidiFunctor("append") {

        @Override
        protected Term compute(Term x, Term y) {
            Term[] xx = x.op() == PROD ? x.subterms().arrayShared() : new Term[]{x};
            if (xx.length == 0) return y;
            Term[] yy = y.op() == PROD ? y.subterms().arrayShared() : new Term[]{y};
            if (yy.length == 0) return x;
            return $.pFast(Terms.concat(xx, yy));
        }

        @Override
        protected Term computeFromXY(Term x, Term y, Term xy) {

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
                Subterms xys = xy.subterms();
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

        }

        @Override
        protected Term computeXfromYandXY(Term x, Term y, Term xy) {
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
                                s.replace(x, $.pFast(xy.subterms().terms((i, ii) -> i < ys)))
                        );
                }
            }
            return Null; //impossible
        }

        @Override
        protected Term computeYfromXandXY(Term x, Term y, Term xy) {
            //solve TAIL
            Term xx;
            if (x.op() != PROD)
                xx = $.pFast(x);
            else
                xx = x;

            int xs = xx.subs();
            int remainderLength = xy.subs() - xs;
            if (remainderLength >= 0) {
                if (xx.subterms().ANDwith((xi, xii) -> xy.sub(xii).equals(xi))) {
                    //the prefix matches
                    if (remainderLength == 0)
                        return Solution.solve(s -> s.replace(y, Op.EmptyProduct));
                    else
                        return Solution.solve(s ->
                                s.replace(y, $.pFast(xy.subterms().terms((i, ii) -> i >= xs)))
                        );
                }
            }
            return Null; //impossible

        }
    };


    public static Functor sub = Functor.f2("sub", (x,n)->{
        if (n.op()==INT) {
            return x.sub( ((Int)n).id, Null );
        } else {
            return null;
        }
    });
    public static Functor subs = Functor.f2Or3("subs", (Term[] args)->{
        if (args.length == 2) {
            //from arg N to end
            Term x = args[0];
            Term n = args[1];
            if (n.op()==INT) {
                int nn = ((Int)n).id;
                Subterms xx = x.subterms();
                int m = xx.subs();
                if (nn < m) {
                    return PROD.the(xx.toArraySubRange(nn, m));
                } else {
                    return Null; //OOB or empty range
                }
            } else {
                return null;
            }
        } else {
            throw new TODO();
        }
    });
}
