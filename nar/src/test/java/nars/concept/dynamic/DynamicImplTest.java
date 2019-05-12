package nars.concept.dynamic;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.*;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.dynamic.DynamicStatementTruth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.concept.dynamic.DynamicConjTest.components;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.*;

class DynamicImplTest extends AbstractDynamicTaskTest {

    static void testDynamicImplSubjPredTemporalExact(int mode, boolean truthIntersectOrUnion) throws Narsese.NarseseException {
        assert (mode != 0);
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
                        xy = dtdt("(a ==>" + dtStr(YA) +
                                (XA <= YA ?
                                        " (x &&" + dtStr(XY) + " y))" :
                                        " (y &&" + dtStr(XY) + " x))"));
                        break;
                    case -2:
                        x = dtdt("(a ==>" + dtStr(XA) + " x)");
                        y = dtdt("(a ==>" + dtStr(YA) + " y)");
                        xy = dtdt("(a ==>" + dtStr(YA) +
                                //" ((--,x) &&" + dtStr(XY) + " (--,y)))"
                                (XA <= YA ?
                                        " ((--,x) &&" + dtStr(XY) + " (--,y)))" :
                                        " ((--,y) &&" + dtStr(XY) + " (--,x)))")
                        );
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }


                Term pt_p = $$(xy);
                assertEquals(xy, pt_p.toString());
                assertEquals(x, $$(x).toString());
                assertEquals(y, $$(y).toString());


                testImpl(mode, outer, inner, x, y, xy, pt_p, truthIntersectOrUnion);
            }
        }

        System.err.println("TODO:");
        todo.forEach(System.err::println);

        //assert (todo.isEmpty());

        //Term p_tp = $$("((x && y) ==>+1 a)");
        //Term pttp = $$("((x &&+1 y) ==>+1 a)");
    }

    private static void testImpl(int mode, int outer, int inner, String x, String y, String xy, Term pt_p, boolean truthIntersectOrUnion) throws Narsese.NarseseException {
        String cccase = Tense.dtStr(inner) + '\t' + Tense.dtStr(outer) + "\t\t" + x + '\t' + y + '\t' + xy;
        System.out.println(cccase);

        for (float xf : new float[]{1, 0}) {
            for (float yf : new float[]{1, 0}) {
                NAR n = NARS.shell();
                //n.log();
                n.believe(x, xf, 0.9f);
                n.believe(y, yf, 0.9f);

                assertTrue(((BeliefTables) n.conceptualizeDynamic(pt_p).beliefs()).tableFirst(DynamicTruthTable.class) != null);
                ; //match first then concept(), tests if the match was enough to conceptualize

                Task task = n.answer(pt_p, BELIEF, 0);
                assertNotNull(task);

                assertEquals(pt_p.toString(), task.term().toString(), cccase);
                assertEquals(2, task.stamp().length, cccase);

                Truth truth = n.truth(pt_p, BELIEF, 0);
                assertEquals(truth, task.truth(), cccase);


                float fxy = truthIntersectOrUnion ? Util.and(xf, yf) : Util.or(xf, yf);
                if (mode == -2) {
                    //negated pred
                    fxy = 1f - fxy;
                }
                assertEquals(fxy, task.freq(), 0.01f, () -> cccase + "\n\tin: " + task);

                assertEquals(0.81f, task.conf(), 0.01f, cccase);
            }
        }
    }

    static private String dtdt(String xy) {
        xy = xy.replace(" ==>+0 ", "=|>");
        xy = xy.replace(" &&+0 ", "&&");
        xy = xy.replace("x && y", "x&&y");
        xy = xy.replace(" ==> ", "==>");
        xy = xy.replace("(--,((--,x)&&(--,y)))", "(x||y)");
        xy = xy.replace("(--,((--,x) && (--,y)))", "(x||y)");
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

    static private Task assertBelief(long when, String answerTermExpected, float freqExpected, float confExpected, NAR n) {
        return assertBelief(answerTermExpected, when, answerTermExpected, freqExpected, confExpected, n);
    }

    static private Task assertBelief(String inputTerm, long when, String answerTermExpected, float freqExpected, float confExpected, NAR n) {
        assertDynamicTable(n, $$(inputTerm));
        Task t = n.answer($$(inputTerm), BELIEF, when);
        assertNotNull(t);
        assertEquals(answerTermExpected, t.term().toString());
        assertEquals(when, t.start());
        int stampLen = 2;
        assertEquals(stampLen, t.stamp().length);
        assertEquals(freqExpected, t.truth().freq(), 0.01f);
        assertEquals(confExpected, t.truth().conf(), 0.01f);
        return t;
    }

    @Test
    void eligibleDynamicImpl() {
        assertDynamicTable("((x && y) ==> a)");
        assertDynamicTable("(a ==> (x && y))");
        assertDynamicTable("(((x,#1) && y) ==> a)"); //#1 not shared between components
    }

    @Test
    void ineligibleDynamicImpl1() {
        assertNotDynamicTable("(((x,#1) && (y,#1)) ==> a)"); //depvar shared between terms
        assertNotDynamicTable("((#1 && (y,#1)) ==> a)"); //raw depvar componnet
        assertNotDynamicTable("(((x,$1) && y) ==> (a,$1))"); //indepvar shared between subj and impl

        assertNotDynamicTable("(((x,#1) && (y,#2)) ==> z(#2))"); //depvar imbalance, unnormalizable in some cases
        assertNotDynamicTable("(((x,#1) && (y,#2)) ==> z(#1))"); //depvar imbalance, unnormalizable in some cases
    }

    private void assertNotDynamicTable(String t) {
        assertFalse(isDynamicTable(t));
    }

    @Test
    void eligibleDynamicImpl2() {
        assertNotDynamicTable("(((x,#1) && (y,#2)) ==> z)"); //depvar unique between subj components
        assertNotDynamicTable("(((x,#1) && (y,#2)) ==> (z,#3))"); //depvar unique between subj components
        assertNotDynamicTable("(((x,#1) && (y,#2)) ==> (z,#1))"); //depvar unique between subj components
    }

    @Test
    void eligibleDynamicImpl3() {
        assertNotDynamicTable("(((x,#1) && y) ==> (a,#1))"); //depvar shared between subj and impl
    }

    @Test
    void eligibleDynamicImpl4() {
        assertDynamicTable("(((x,#1) && (y,#1)) ==> (a,#1))"); //all share the variable so it could be
    }

    @Test
    void testDynamicImplSubj() {
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
                Term pn = $$("((x || z) ==> a)");
                assertTrue(isDynamicTable(pn));

                assertEquals($.t(1f, 0.81f), n.beliefTruth(pp, now));

                assertEquals($.t(0f, 0.81f), n.beliefTruth(pn, now));

                {
                    Term pnnConj = $$("((x && --z) ==> a)");
                    Truth pnnConjTruth = n.beliefTruth(pnnConj, now);
                    assertEquals($.t(/* union */ 1, 0.81f), pnnConjTruth);
                }
                {
                    Term pnnDisj = $$("((x || --z) ==> a)");
                    Truth pnnDisjTruth = n.beliefTruth(pnnDisj, now);
                    assertEquals($.t(/* intersection */ 0.75f, 0.81f), pnnDisjTruth);
                }


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
        testDynamicImplSubjPredTemporalExact(1, false /* reversed */);
    }

    @Test
    void testDynamicImplSubjNegTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(-1, true /* reversed */);
    }

    @Test
    void testDynamicImplPredTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(2, true);
    }

    @Test
    void testDynamicImplPredNegTemporalExact() throws Narsese.NarseseException {
        testDynamicImplSubjPredTemporalExact(-2, false);
    }

    @Test
    public void testXternalSubj() throws Narsese.NarseseException {

        for (String s : new String[]{
                "((a && b) ==>+- x)",
                "((a &&+- b) ==> x)",
                "((a &&+- b) ==>+- x)"
        }) {

            assertBelief(s,
                    ETERNAL, "((a&&b)==>x)", 1, 0.81f, NARS.shell()
                            .believe("(a ==> x)")
                            .believe("(b ==> x)")
            );
        }
    }

    @Test
    public void testXternalPred() throws Narsese.NarseseException {


        for (String s : new String[]{
                "(x ==>+- (a && b))",
                "(x ==>+- (a &&+- b))",
                "(x ==>+- (a &&+- b))"
        }) {

            assertBelief(s, ETERNAL, "(x==>(a&&b))", 1, 0.81f,
                    NARS.shell().believe("(x ==> a)").believe("(x ==> b)")
            );
        }
    }

    @Test
    void testReconstructImplConjTemporal() {
        //((((left-right)-->fz) &&+6555 (left-->fz))==>(right-->fz)). -280⋈41620 %.14;.24%
        //((--,(left-->fz)) ==>+300 (right-->fz)). 7460⋈44240 %.40;.43%
        //DONT produce: ((((left-right)-->fz) &&+6555 (left-->fz))&&(--,(left-->fz)))
    }


    @Test
    void testDynamicConjunctionFactoredInImpl() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("(x==>a)"), ETERNAL);
        n.believe($("(y ==>+2 a)"), 0);
        n.believe($("(z =|> a)"), 2);
        n.time.dur(8);

        {

            Compound xyz = $("((x && (y &&+2 z))=|>a)");
            assertEquals(
                    //"[((x&&y) ==>+2 a) @ 0, ((x&&z)=|>a) @ 0]",
                    "[(x==>a) @ 0..0, (y ==>+2 a) @ 0..0, (z=|>a) @ 0..0]",
                    components(DynamicStatementTruth.ImplSubjConj, xyz, 0, 0).toString());
            Task t = n.answer(xyz, BELIEF, 0);
            assertNotNull(t);
            assertEquals(xyz, t.term());
        }
    }

    /**
     * subject: &&=union, ||=intersection
     */
    @Test
    void testDifferenceBetweenIntersectionAndUnionInSubj() throws Narsese.NarseseException {
        assertBelief(ETERNAL, "((a&&b)==>x)",
                1, 0.81f,
                NARS.shell().believe("(a ==> x)").believe("(b ==> x)")
        );
        assertBelief(ETERNAL, "((a&&b)==>x)",
                0, 0.81f,
                NARS.shell().believe("--(a ==> x)").believe("--(b ==> x)")
        );
    }

    /**
     * subject: &&=union, ||=intersection
     */
    @Test
    void testDifferenceBetweenIntersectionAndUnionInSubj2() throws Narsese.NarseseException {
        //here is where the real "difference between union and intersection" is tested

        //conj = union
        assertBelief(ETERNAL, "((a&&b)==>x)",
                1, 0.81f,
                NARS.shell().believe("--(a ==> x)").believe("(b ==> x)")
        );
        //disj = intersection
        assertBelief(ETERNAL, "((a||b)==>x)",
                0, 0.81f,
                NARS.shell().believe("--(a ==> x)").believe("(b ==> x)")
        );

    }

    /**
     * predicate: &&=intersection, ||=union
     */
    @Test
    void testDifferenceBetweenIntersectionAndUnionInPred() throws Narsese.NarseseException {
        assertBelief(ETERNAL, "(x==>(a&&b))",
                1, 0.81f,
                NARS.shell().believe("(x ==> a)").believe("(x ==> b)")
        );
        assertBelief(ETERNAL, "(x==>(a&&b))",
                0, 0.81f,
                NARS.shell().believe("--(x ==> a)").believe("(x ==> b)")
        );

        assertBelief(ETERNAL, "(x==>((--,a)&&b))",
                1, 0.81f,
                NARS.shell().believe("--(x ==> a)").believe("(x ==> b)")
        );

        //"(x==>(a||b))":
        assertBelief(ETERNAL, "(--,(x==>((--,a)&&(--,b))))",
                1, 0.81f,
                NARS.shell().believe("--(x ==> a)").believe("(x ==> b)")
        );
    }

}
