package nars.concept.dynamic;

import jcog.data.list.FasterList;
import nars.*;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicImplTest {

    @Test
    void testDynamicImplSubj() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("(x ==> a)", 1f, 0.9f);
        n.believe("(y ==> a)", 1f, 0.9f);
        //            n.believe("a:y", 1f, 0.9f);
        //            n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("(z ==> a)", 0f, 0.9f);
        n.believe("(--z ==> a)", 0.75f, 0.9f);
        //            n.believe("a:(--,z)", 1f, 0.9f);
        //            n.believe("x:b", 1f, 0.9f);
        //            n.believe("y:b", 1f, 0.9f);
        //            n.believe("z:b", 0f, 0.9f);
        n.run(2);
        for (long now : new long[]{0, n.time() /* 2 */, ETERNAL}) {

            //AND
            {
                Term pp = $$("((x && y) ==> a)");
                assertTrue(n.conceptualize(pp).beliefs() instanceof DynamicTruthBeliefTable);
                assertEquals($.t(1f, 0.81f), n.beliefTruth(pp, now));

                Term pn = $$("((x && z) ==> a)");
                assertEquals($.t(0f, 0.81f), n.beliefTruth(pn, now));

                Term pnn = $$("((x && --z) ==> a)");
                assertEquals($.t(0.75f, 0.81f), n.beliefTruth(pnn, now));
            }

            //OR
            {
                Term NppAndNeg = $$("(--(--x && --y) ==> a)");
                assertEquals($.t(1f, 0.81f), n.beliefTruth(NppAndNeg, now));

                Term NppOrPos = $$("((x || y) ==> a)");
                assertEquals($.t(1f, 0.81f), n.beliefTruth(NppOrPos, now));
            }


            { //Unknowable cases
                Term NppOrPosNeg = $$("((x || --y) ==> a)");
                assertEquals(null /* $.t(1f, 0.81f) */, n.beliefTruth(NppOrPosNeg, now));


                Term NppAndPos = $$("(--(x && y) ==> a)");
                assertEquals(null, n.beliefTruth(NppAndPos, now));


                Term NppOrNeg = $$("((--x || --y) ==> a)");
                assertEquals(null, n.beliefTruth(NppOrNeg, now));
            }




            //                assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
        }

    }

    @Test
    void testDynamicImplSubjTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(1);
    }

    @Test
    void testDynamicImplSubjNegTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(-1);
    }

    @Test
    void testDynamicImplPredTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(2);
    }

    static void testDynamicImplSubjPredTemporalExact(int mode) throws Narsese.NarseseException {
        List<String> todo = new FasterList();

        int[] ii = {1, 0, 2, -2, -1, 3, -3, DTERNAL, XTERNAL};
        int[] oo = {1, 0, 2, -2, -1, 3, -3, DTERNAL, XTERNAL};
        for (int outer : oo) {
            for (int inner : ii) {
                NAR n = NARS.shell();


                int XA, YA, XY;
                if (inner != XTERNAL && outer != XTERNAL && inner != DTERNAL && outer != DTERNAL) {
                    XA = inner >= 0 ? outer + inner : outer - inner;
                    YA = inner >= 0 ? XA - inner : XA + inner;
                    XY = XA - outer;
                } else if (inner == XTERNAL || outer == XTERNAL) {
                    todo.add(inner + " " + outer); //throw new TODO();
                    continue;
                } else {
                    if (inner == DTERNAL) {
                        if (outer == DTERNAL) {
                            XA = YA = XY = DTERNAL;
                        } else {
                            XY = 0; // DTERNAL; ?
                            XA = YA = outer;
                        }
                    } else if (outer == DTERNAL) {
                        todo.add(inner + " " + outer); //throw new TODO();
                        continue;
                        //                        XA = XY = inner;
                        //                        YA = DTERNAL;
                        //                        xy = "((x &&+1 y)==>a)"; //override
                    } else {
                        todo.add(inner + " " + outer); //throw new TODO();
                        continue;

                    }
                }
                String x, y, xy;

                switch (mode) {
                    case +1:
                        x = dtdt("(x ==>" + dtStr(XA) + " a)");
                        y = dtdt("(y ==>" + dtStr(YA) + " a)");
                        xy = dtdt("((x &&" + dtStr(XY) + " y) ==>" + dtStr(YA) + " a)");
                        break;
                    case -1:
                        x = dtdt("(x ==>" + dtStr(XA) + " a)");
                        y = dtdt("(y ==>" + dtStr(YA) + " a)");
                        xy = dtdt("((--,((--,x) &&" + dtStr(XY) + " (--,y))) ==>" + dtStr(YA) + " a)");
                        break;
                    case 2:
                        x = dtdt("(a ==>" + dtStr(XA) + " x)");
                        y = dtdt("(a ==>" + dtStr(YA) + " y)");
                        xy = dtdt("(a ==>" + dtStr(YA) + " (x &&" + dtStr(XY) + " y))");
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                System.out.println("i=" + inner + " o=" + outer + "\t"
                        + x + " " + y + " " + xy);


                Term pt_p = $$(xy);
                assertEquals(xy, pt_p.toString());
                assertEquals(x, $$(x).toString());
                assertEquals(y, $$(y).toString());
                n.believe(x, 1f, 0.9f);
                n.believe(y, 1f, 0.9f);

                Task pt_p_Task = n.match(pt_p, BELIEF, 0);

                assertTrue(n.concept(pt_p).beliefs() instanceof DynamicBeliefTable); //match first then concept(), tests if the match was enough to conceptualize

                assertNotNull(pt_p_Task);

                String cccase = inner + " " + outer + "\t" + x + " " + y + " " + xy;
                assertEquals(pt_p.toString(), pt_p_Task.term().toString(),
                        cccase);
                assertEquals(2, pt_p_Task.stamp().length);
                assertEquals(0.81f, pt_p_Task.conf());
            }
        }

        System.err.println("TODO:");
        todo.forEach(System.err::println);

        //assert (todo.isEmpty());

        //Term p_tp = $$("((x && y) ==>+1 a)");
        //Term pttp = $$("((x &&+1 y) ==>+1 a)");
    }


    static private String dtdt(String xy) {
        xy = xy.replace(" ==>+0 ", "=|>");
        xy = xy.replace(" &&+0 ", "&|");
        xy = xy.replace("x && y", "x&&y");
        xy = xy.replace(" ==> ", "==>");
        xy = xy.replace("(--,((--,x) && (--,y)))", "(||,x,y)");
        return xy;
    }

    static private String dtStr(int dt) {
        switch (dt) {
            case DTERNAL:
                return "";
            case XTERNAL:
                return "+-";
        }
        return (dt >= 0 ? "+" : "") + (dt);
    }

}
