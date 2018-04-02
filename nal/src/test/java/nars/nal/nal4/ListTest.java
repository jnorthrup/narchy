package nars.nal.nal4;

import jcog.TODO;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Solution;
import nars.term.Term;
import nars.term.atom.Atom;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static nars.$.$$;
import static nars.Op.Null;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListTest {

    /**
     * emulates prolog append/3
     */
    final static Functor append = Functor.f3((Atom) $.the("append"), (x, y, xy) -> {
        if (xy.op().var) {
            //forward
            if (x.op().var || y.op().var) {
                return null; //uncomputable; no change
            } else {
                throw new TODO();
            }
        } else {
            //reverse

            if (xy.op() != PROD)
                xy = $.pFast(xy);

            if (x.op().var && !y.op().var) {
                //solve head
                throw new TODO();
            } else if (!x.op().var && y.op().var) {
                //solve tail
                Subterms xys = xy.subterms();


                //solve head

                Term _x = x;
                if (x.op() != PROD)
                    x = $.pFast(x);

                int xs = x.subs();
                int remainderLength = xy.subs() - xs;
                if (remainderLength < 0)
                    return Null; //impossible
                else {
                    Solution s = Solution.the();
                    if (s == null) return Null; //unsolvable without permuting solution context

                    Term yy = $.pFast(xys.terms((i, ii) -> i >= xs));
                    s.replace(y, yy);
                    return null;
                }

            } else if (x.op().var && y.op().var) {
                Subterms xys = xy.subterms();
                Solution s = Solution.the();
                if (s == null) return Null; //unsolvable without permuting solution context

                int l = xy.subs();
                if (l == 0) {
                    s.replace(
                            x, Op.EmptyProduct,
                            y, Op.EmptyProduct);
                    return null;
                } else if (l == 1) {
                    s.replace(
                            x, Op.EmptyProduct,
                            y, xy);
                    s.replace(
                            x, xy,
                            y, Op.EmptyProduct);
                    return null;
                } else {
                    Term xx = x;
                    s.replace(
                            Util.map(-1, l+1, finalI ->
                                s.subst(
                                        xx, $.pFast(xys.terms((xyi, ii) -> xyi <= finalI)),
                                        y,  $.pFast(xys.terms((xyi, ii) -> xyi > finalI)))
                                ,
                                Predicate[]::new
                            )
                    );
                    return null;
                }
            } else {
                throw new TODO("append(x,y,xy) -> True or False");
            }

        }
    });

    @Test
    public void testAppend1() {
        NAR n = NARS.shell();
        n.on(append);

        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Solution.solve($$("append((x),#what,(x,y))"), n.concepts.functors));

        //TODO 0 and 1 element xy lists

        assertEquals(
                Set.of(
                        $$("append((x,y,z),(),(x,y,z))"),
                        $$("append((x,y),(z),(x,y,z))"),
                        $$("append((x),(y,z),(x,y,z))"),
                        $$("append((),(x,y,z),(x,y,z))")
                ),
                Solution.solve($$("append(#x,#y,(x,y,z))"), n.concepts.functors));


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
