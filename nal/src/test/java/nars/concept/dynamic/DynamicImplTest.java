package nars.concept.dynamic;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.*;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

class DynamicImplTest extends AbstractDynamicTaskTest {

    @Test void eligibleDynamicImpl() {
        //((--,(([add(#1,2)]<->[#1])&|equal(#1,0)))=|>([add(#1,2)]<->[#1]))
        String t = "((x && y) ==> a)";
        assertDynamicTable(t);
        assertDynamicTable("(((x,#1) && y) ==> a)"); //#1 not shared between components
        assertFalse(isDynamicTable("((#1 && (y,#1)) ==> a)")); //raw depvar componnet
        assertFalse(isDynamicTable("(((x,#1) && (y,#1)) ==> a)"));
        assertFalse(isDynamicTable("(((x,$1) && y) ==> (a,$1))")); //indepvar shared between subj and impl
    }



    @Test void eligibleDynamicImpl2() {
        assertFalse(isDynamicTable("(((x,#1) && y) ==> (a,#1))")); //depvar shared between subj and impl
        String s = "(((x,#1) && (y,#1)) ==> (a,#1))";
        assertDynamicTable(s); //all share the variable so it could be
    }


    @Test
    void testDynamicImplSubj() throws Narsese.NarseseException {
        n.believe("(x ==> a)", 1f, 0.9f);
        n.believe("(y ==> a)", 1f, 0.9f);
        //            n.believe("a:y", 1f, 0.9f);
        //            n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("(z ==> a)", 0f, 0.9f);
        n.believe("(w ==> a)", 0f, 0.9f);
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
                assertTrue(isDynamicTable(pp));
                assertEquals($.t(1f, 0.81f), n.beliefTruth(pp, now));

//                Term pn = $$("((x && z) ==> a)");
//                assertEquals($.t(0f, 0.81f), n.beliefTruth(pn, now));

                Term pnn = $$("((x && --z) ==> a)");
                assertEquals($.t(0.75f, 0.81f), n.beliefTruth(pnn, now));


                assertEquals($.t(0f, 0.81f), n.beliefTruth($$("((z && w) ==> a)"), now));
                assertEquals($.t(0f, 0.81f), n.beliefTruth($$("((z || w) ==> a)"), now));
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
    @Test
    void testDynamicImplPredNegTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(-2);
    }

    static void testDynamicImplSubjPredTemporalExact(int mode) throws Narsese.NarseseException {
        List<String> todo = new FasterList();

        int[] ii = {1, 0, 2, -2, -1, 3, -3, DTERNAL, XTERNAL};
        int[] oo = {1, 0, 2, -2, -1, 3, -3, DTERNAL, XTERNAL};
        for (int outer : oo) {
            for (int inner : ii) {


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
                    case -2:
                        x = dtdt("(a ==>" + dtStr(XA) + " x)");
                        y = dtdt("(a ==>" + dtStr(YA) + " y)");
                        xy = dtdt("(a ==>" + dtStr(YA) + " ((--,x) &&" + dtStr(XY) + " (--,y)))");
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }




                Term pt_p = $$(xy);
                assertEquals(xy, pt_p.toString());
                assertEquals(x, $$(x).toString());
                assertEquals(y, $$(y).toString());


                testImpl(mode, outer, inner, x, y, xy, pt_p);
            }
        }

        System.err.println("TODO:");
        todo.forEach(System.err::println);

        //assert (todo.isEmpty());

        //Term p_tp = $$("((x && y) ==>+1 a)");
        //Term pttp = $$("((x &&+1 y) ==>+1 a)");
    }

    static String dts(int dt) {
        if (dt == DTERNAL)
            return "ETE";
        else
            return String.valueOf(dt);
    }

    private static void testImpl(int mode, int outer, int inner, String x, String y, String xy, Term pt_p) throws Narsese.NarseseException {
        String cccase = dts(inner) + "\t" + dts(outer) + "\t\t" + x + "\t" + y + "\t" + xy;
        System.out.println(cccase);

        for (float xf : new float[] { 1, 0 }) {
            for (float yf : new float[] { 1, 0 }) {
                NAR n = NARS.shell();
                //n.log();
                n.believe(x, xf, 0.9f);
                n.believe(y, yf, 0.9f);

                Task task = n.answer(pt_p, BELIEF, 0);
                //System.out.println(task);

                Truth truth = n.truth(pt_p, BELIEF, 0);

                assertTrue(((BeliefTables)n.concept(pt_p).beliefs()).tableFirst(DynamicTruthTable.class)!=null);; //match first then concept(), tests if the match was enough to conceptualize

                assertNotNull(task);

                assertEquals(pt_p.toString(), task.term().toString(), cccase);
                assertEquals(2, task.stamp().length);
                assertEquals(truth, task.truth());

                boolean intersection = mode>0; //else union
                float fxy = intersection ? Util.and(xf, yf) : Util.or(xf, yf);
                if (mode == -2) {
                    //negated pred
                    fxy = 1f - fxy;
                }
                assertEquals(fxy, task.freq(), 0.01f);

                assertEquals(0.81f, task.conf(), 0.01f);
            }
        }
    }


    static private String dtdt(String xy) {
        xy = xy.replace(" ==>+0 ", "=|>");
        xy = xy.replace(" &&+0 ", "&|");
        xy = xy.replace("x && y", "x&&y");
        xy = xy.replace(" ==> ", "==>");
        xy = xy.replace("(--,((--,x) && (--,y)))", "(||,x,y)");
        xy = xy.replace("((--,x) && (--,y))", "((--,x)&&(--,y))");
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

    @Test public void testXternal() throws Narsese.NarseseException {

        for (String s : new String[] {
                "((a && b) ==>+- x)",
                "((a &&+- b) ==> x)",
                "((a &&+- b) ==>+- x)"
        }) {
            NAR n = NARS.shell();
            n.believe("(a ==> x)");
            n.believe("(b ==> x)");
            assertDynamicTable($$(s));
            @Nullable Task t = n.answer($$(s), BELIEF, ETERNAL);
            assertEquals("((a&&b)==>x)", t.term().toString());
            assertEquals(ETERNAL, t.start());
            assertEquals(2, t.stamp().length);
            assertEquals(1f, t.truth().freq());
            assertEquals(0.81f, t.truth().conf(), 0.01f);
        }
    }


}
