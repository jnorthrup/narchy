package nars.op;

import jcog.TODO;
import nars.$;
import nars.Op;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.IdempotentBool;
import nars.term.atom.IdempotInt;
import nars.term.buffer.Termerator;
import nars.term.functor.BinaryBidiFunctor;
import nars.term.functor.UnaryBidiFunctor;
import nars.term.util.conj.Conj;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.INT;
import static nars.Op.PROD;

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
public final class ListFunc{

    /**
     * emulates prolog append/3
     */
    public static final Functor append = new BinaryBidiFunctor("append") {

        @Override
        protected Term compute(Evaluation e, Term x, Term y) {
            Term[] xx = x.op() == PROD ? x.subterms().arrayShared() : new Term[]{x};
            if (xx.length == 0) return y;
            Term[] yy = y.op() == PROD ? y.subterms().arrayShared() : new Term[]{y};
            if (yy.length == 0) return x;
            return $.INSTANCE.pFast(Terms.concat(xx, yy));
        }

        @Override
        protected Term computeFromXY(Evaluation e, Term x, Term y, Term xy) {

            int l = xy.subs();
            switch (l) {
                case 0:
                    return e.is(x, Op.EmptyProduct, y, Op.EmptyProduct) ? null : IdempotentBool.Null;
                case 1:
                    return e.is(x, Op.EmptyProduct, y, xy) && e.is(x, xy, y, Op.EmptyProduct) ? null : IdempotentBool.Null;
                default:
                    Subterms xys = xy.subterms();

                    List<Predicate<Termerator>> list = new ArrayList<>();
                    for (int i = -1; i < l; i++) {
                        int finalI = i;
                        Predicate<Termerator> assign = Termerator.assign(
                                x, $.INSTANCE.pFast(xys.terms(new IntObjectPredicate<Term>() {
                                    @Override
                                    public boolean accept(int xyi, Term ii) {
                                        return xyi <= finalI;
                                    }
                                })),
                                y, $.INSTANCE.pFast(xys.terms(new IntObjectPredicate<Term>() {
                                    @Override
                                    public boolean accept(int xyi, Term ii) {
                                        return xyi > finalI;
                                    }
                                })));
                        list.add(assign);
                    }
                    e.canBe(list);

                    return null;
            }

        }

        @Override
        protected Term computeXfromYandXY(Evaluation e, Term x, Term y, Term xy) {

            Term yy = y.op() != PROD ? $.INSTANCE.pFast(y) : y;

            int ys = yy.subs();

            int remainderLength = xy.subs() - ys;
            if (remainderLength >= 0)
                if (yy.subterms().ANDi(new ObjectIntPredicate<Term>() {
                    @Override
                    public boolean accept(Term yi, int yii) {
                        return xy.sub(remainderLength + yii).equals(yi);
                    }
                }))
                    return e.is(x, remainderLength == 0 ?
                            Op.EmptyProduct
                            :
                            $.INSTANCE.pFast(xy.subterms().terms(new IntObjectPredicate<Term>() {
                                @Override
                                public boolean accept(int i, Term ii) {
                                    return i < ys;
                                }
                            })))
                                ? null : IdempotentBool.Null;


            return y.hasAny(Op.Variable) || xy.hasAny(Op.Variable) ? null : IdempotentBool.Null;
        }

        @Override
        protected Term computeYfromXandXY(Evaluation e, Term x, Term y, Term xy) {

            Term xx = x.op() != PROD ? $.INSTANCE.pFast(x) : x;

            int xs = xx.subs();
            int remainderLength = xy.subs() - xs;
            if (remainderLength >= 0)
                if (xx.subterms().ANDi(new ObjectIntPredicate<Term>() {
                    @Override
                    public boolean accept(Term xi, int xii) {
                        return xy.sub(xii).equals(xi);
                    }
                }))
                    return e.is(y, (remainderLength == 0) ? Op.EmptyProduct : $.INSTANCE.pFast(xy.subterms().terms(new IntObjectPredicate<Term>() {
                        @Override
                        public boolean accept(int i, Term ii) {
                            return i >= xs;
                        }
                    }))) ? null : IdempotentBool.Null;

            return x.hasAny(Op.Variable) || xy.hasAny(Op.Variable) ? null : IdempotentBool.Null;
        }
    };


    public static final Functor reverse = new UnaryBidiFunctor("reverse") {

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
                        return ((Compound)x).dt(-dt);
                    break;
            }
            return null;
        }

        @Override
        protected Term uncompute(Term x, Term y) {
            return compute(y);
        }
    };

    public static final Functor sub = Functor.f2("sub",
            new BiFunction<Term, Term, Term>() {
                @Override
                public Term apply(Term x, Term n) {
                    return n.op() == INT ? x.sub(((IdempotInt) n).i, IdempotentBool.Null) : null;
                }
            });

    public static final Functor subs = Functor.f2Or3("subs", new Function<Subterms, Term>() {
        @Override
        public Term apply(Subterms args) {
            if (args.subs() == 2) {
                Term n = args.sub(1);
                if (n.op() == INT) {
                    int nn = ((IdempotInt) n).i;
                    Subterms xx = args.sub(0).subterms();
                    int m = xx.subs();
                    return nn < m ? PROD.the(xx.subRangeArray(nn, m)) : IdempotentBool.Null;
                } else {
                    return null;
                }
            } else {
                throw new TODO();
            }
        }
    });
}
