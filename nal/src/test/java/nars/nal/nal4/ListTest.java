package nars.nal.nal4;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Solution;
import nars.term.Term;
import nars.term.Terms;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListTest {

    /**
     * emulates prolog append/3
     */
    final static Functor append = Functor.f2Or3("append", (Term[] aa) -> {
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

    @Test
    public void testAppendTransform() {
        NAR n = NARS.shell();
        n.on(append);
        assertEquals(
                Set.of($$("(x,y)")),
                Solution.solve($$("append((x),(y))"), n.concepts.functors));
        assertEquals(
                Set.of($$("append(#x,(y))")),
                Solution.solve($$("append(#x,(y))"), n.concepts.functors));

    }

    @Test
    public void testAppendResult() {
        NAR n = NARS.shell();
        n.on(append);

        //solve result
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),(y),#what)"), n.concepts.functors));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(y),(x,y)) && ((x,y)<->solution))")),
                Solution.solve($$("(append((x),(y),#what) && (#what<->solution))"), n.concepts.functors));

    }


    @Test
    public void testTestResult() {
        NAR n = NARS.shell();
        n.on(append);

        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),(y),(x,y))"), n.concepts.functors));

        assertEquals(
                Set.of($$("append(x,y,(x,y))")),
                Solution.solve($$("append(x,y,(x,y))"), n.concepts.functors));

        assertEquals(
                Set.of(False),
                Solution.solve($$("append((x),(y),(x,y,z))"), n.concepts.functors));

    }

    @Test
    public void testAppendTail() {
        NAR n = NARS.shell();
        n.on(append);

        //solve tail
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),#what,(x,y))"), n.concepts.functors));

        //solve tail with non-list prefix that still matches
        assertEquals(
                Set.of($$("append(x,(y),(x,y))")),
                Solution.solve($$("append(x,#what,(x,y))"), n.concepts.functors));

        //solve tail but fail
        assertEquals(
                Set.of(Null),
                Solution.solve($$("append((z),#what,(x,y))"), n.concepts.functors));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(),(x)) && (()<->solution))")),
                Solution.solve($$("(append((x),#what,(x)) && (#what<->solution))"), n.concepts.functors));

    }

    @Test
    public void testAppendHeadAndTail() {
        NAR n = NARS.shell();
        n.on(append);

        assertEquals(
                Set.of(
                        $$("append((x,y,z),(),(x,y,z))"),
                        $$("append((x,y),(z),(x,y,z))"),
                        $$("append((x),(y,z),(x,y,z))"),
                        $$("append((),(x,y,z),(x,y,z))")
                ),
                Solution.solve($$("append(#x,#y,(x,y,z))"), n.concepts.functors));
    }
    @Test
    public void testAppendHeadAndTailMulti() {
        NAR n = NARS.shell();
        n.on(append);

        assertEquals(
            Set.of(
                    $$("(append((),(x,y),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((),(a,b),(a,b)))"),
                    $$("(append((),(x,y),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((),(x,y),(x,y)),append((),(a,b),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((),(a,b),(a,b)))")
            ),
            Solution.solve($$("(append(#x,#y,(x,y)), append(#a,#b,(a,b)))"), n.concepts.functors));

        assertEquals(
                Set.of(
                        $$("(append((),(x,y),(x,y)),append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)),append((x),(b),(x,b)))")
                ),
                Solution.solve($$("(append(#x,#y,(x,y)), append(#x,#b,(x,b)))"), n.concepts.functors));

        assertEquals(
                Set.of(
                        $$("(append((),(x,y),(x,y)) && append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)) && append((x),(b),(x,b)))")
                ),
                Solution.solve($$("(&&,append(#x,#y,(x,y)),append(#a,#b,(x,b)),equal(#x,#a))"), n.concepts.functors));

    }

    @Test
    public void testAppendHead() {
        NAR n = NARS.shell();
        n.on(append);

        //solve head
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append(#what,(y),(x,y))"), n.concepts.functors));

        assertEquals(
                Set.of($$("append((),(x,y),(x,y))")),
                Solution.solve($$("append(#what,(x,y),(x,y))"), n.concepts.functors));

    }
//    @Test
//    public void test1() {
//        NAR n = NARS.tmp(3);
//        Deriver listDeriver = new Deriver(n, "list.nal");
//
////                "motivation.nal"
////                //, "goal_analogy.nal"
////        ).apply(n).deriver, n) {
//        TestNAR t = new TestNAR(n);
//    }
}
