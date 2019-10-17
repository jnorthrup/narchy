package nars.concept.dynamic;

import jcog.data.list.FasterList;
import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.dynamic.AbstractDynamicTruth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;
import static org.junit.jupiter.api.Assertions.*;

class DynamicConjTest {
    @Test
    void testDynamicConjunction2() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        n.run(1);
        long now = n.time();

        assertEquals($.t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now));

        assertEquals($.t(0f, 0.81f), n.beliefTruth("(a:x && (--,a:y))", now));

        assertEquals($.t(1f, 0.81f), n.belief($("(a:x && a:y)"), now).truth());

        assertEquals($.t(0f, 0.81f), n.beliefTruth("(b:x && a:y)", now));
        assertEquals($.t(1f, 0.81f), n.beliefTruth("((--,b:x) && a:y)", now));
        assertEquals($.t(0f, 0.81f), n.beliefTruth("((--,b:x) && (--,a:y))", now));
    }

    @Test
    void testDynamicConjunctionEternalOverride() throws Narsese.NarseseException {
        NAR n = NARS.shell()
                .believe($$("a:x"), 0)
                .believe($$("a:y"), 0);

        final long now = 0; //n.time();
        assertEquals($.t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now));

        {
            //temporal evaluated a specific point
            Task xy = n.belief($("(a:x && a:y)"), now);
            assertEquals("((x-->a)&&(y-->a))", xy.term().toString());
            assertEquals($.t(1f, 0.81f), xy.truth());
        }

        {
            Task xAndNoty = n.belief($("(a:x && --a:y)"), now);
            assertEquals("((--,(y-->a))&&(x-->a))", xAndNoty.term().toString());
            assertEquals($.t(0f, 0.81f), xAndNoty.truth());
        }

        {
            //remain eternal
            Task xy = n.belief($("(a:x && a:y)"), ETERNAL);
            assertNotNull(xy);
            assertEquals(0, xy.start()); //exact time since it was what was stored
            assertEquals("((x-->a)&&(y-->a))", xy.term().toString());
            assertEquals($.t(1f, 0.81f), xy.truth());
        }


        //override or revise dynamic with an input belief
        {
            n.believe($$("--(a:x && a:y)"), 0);
            assertEquals(1, n.concept("(a:x && a:y)").beliefs().taskCount());

            Task ttEte = n.answerBelief($("(a:x && a:y)"), now);
//            assertEquals(1, ttEte.stamp().length);


            assertTrue(ttEte.toString().contains("((x-->a)&&(y-->a)). 0 %"));

            Truth tNow = n.beliefTruth($("(a:x && a:y)"), now);
            assertTrue(
                    $.t(0.32f, 0.93f /*0.87f*/)
                    //$.t(0.00f, 0.90f)
                    //$.t(0.32f, 0.90f /*0.87f*/)
                            .equalTruth(tNow, n), ()->"was " + tNow + " at " + now);

        }
        {
            n.believe($$("--(a:x && a:y)"), 0);
            assertTrue(2 <= n.concept("(a:x && a:y)").beliefs().taskCount());

            Task ttNow = n.answerBelief($("(a:x && a:y)"), now);
            assertTrue(ttNow.isNegative());
            assertTrue(ttNow.toString().contains("((x-->a)&&(y-->a)). 0"), ttNow.toString());
        }


        Task tAfterTask = n.belief($("(a:x && a:y)"), now + 2);
        assertEquals(now + 2, tAfterTask.start());
        assertEquals(now + 2, tAfterTask.end());

        Truth tAfter = n.beliefTruth($("(a:x && a:y)"), now + 2);
        assertNotNull(tAfter);
        assertTrue(tAfter.isNegative());
        //assertTrue($.t(0.19f, 0.88f).equalsIn(tAfter, n), () -> tAfter.toString());

        Truth tLater = n.beliefTruth($("(a:x && a:y)"), now + 5);
        assertTrue(tLater.isPositive() == tAfter.isPositive());
        assertTrue(tLater.conf() < tAfter.conf());
        //assertTrue($.t(0.19f, 0.79f).equalsIn(tLater, n), () -> tLater.toString());
    }

    @Test
    void testDynamicConjunctionEternalTemporalMix() throws Narsese.NarseseException {

//        String xx = "((e&&x)&&(e&&y))";
//        assertEquals(xx, $$("((x&&y)&&e)").toString());

        NAR n = NARS.shell()
                .believe($$("x"), 0)
                .believe($$("y"), 0)
                .believe($$("e"), ETERNAL);

        Term xye = $("(&&,x,y,e)");

        Task atZero = n.belief(xye, 0);
        assertNotNull(atZero);

        Task atOne = n.belief(xye, 1);
        assertNotNull(atOne);

        Task atEte = n.belief(xye, ETERNAL);
        assertNotNull(atEte);

        assertEquals(0, atZero.start());

        assertEquals(1, atOne.start());
        assertEquals(0, atEte.start());

        assertEquals(0.73f, atZero.conf(), 0.01f);
        assertEquals(0.73f, atEte.conf(), 0.01f);
        assertEquals(0.7f, atOne.conf(), 0.15f);
    }

    @Test
    void testDynamicConjunctionTemporalOverride() throws Narsese.NarseseException {
        NAR n = NARS.shell()
                .believe("a:x", 1f, 0.9f)
                .believe("a:y", 1f, 0.9f);

        n.run(1);
        long now = n.time();
        assertEquals($.t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now));

        n.believe($$("--(a:x && a:y)"), now);


        Truth tt = n.belief($("(a:x && a:y)"), now).truth();
        assertTrue($.t(0.32f, 0.93f).equalTruth(tt, n), tt::toString);
    }

    @Test
    void testDynamicConjunction3() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        TaskConcept cc = (TaskConcept) n.conceptualize($("(&&, a:x, a:y, a:z)"));
        Truth now = n.beliefTruth(cc, n.time());
        assertNotNull(now);
        assertTrue($.t(1f, 0.73f).equalTruth(now, 0.1f), now + " truth at " + n.time());


        {
            TaskConcept ccn = (TaskConcept) n.conceptualize($("(&&, a:x, a:w)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertNull(nown);
        }


        Concept ccn = n.conceptualize($("(&&, a:x, (--, a:y), a:z)"));

        {
            Task t = n.belief(ccn.term());
            assertNotNull(t);
            assertEquals(0f, t.freq());
        }

        assertTrue(ccn instanceof TaskConcept);
        Truth nown = n.beliefTruth(ccn, n.time());
        assertEquals("%0.0;.73%", nown.toString());

        n.clear();


        n.believe("a:y", 0, 0.95f);
        n.run(1);
        n.concept("a:y").print();
        Task ay = n.belief($$("a:y"));
        assertTrue(ay.freq() < 0.5f);

//        for (int i = 0; i < 4; i++) {
//            Task y = n.belief(n.conceptualize($("(&&, a:x, a:y, a:z)")), n.time());
//            Truth yt = y.truth();
//            assertTrue(yt.freq() < 0.4f, () -> y.proof());
//        }

    }

    @Test
    void testDynamicConjunctionEternal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("x"));
        n.believe($("y"));
        n.believe($("--z"));


        for (long w: new long[]{ETERNAL, 0, 1}) {
            assertEquals($.t(1, 0.81f), n.truth($("(x && y)"), BELIEF, w));
            assertEquals($.t(0, 0.81f), n.truth($("(x && --y)"), BELIEF, w));
            assertEquals($.t(1, 0.81f), n.truth($("(x && --z)"), BELIEF, w));
        }
    }

    @Test
    void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("(x)"), 0);
        n.believe($("(y)"), 4);
        n.time.dur(8);
        TaskConcept cc = (TaskConcept) n.conceptualize($("((x) && (y))"));

        BeliefTable xtable = cc.beliefs();


        int dur = 0;
        assertEquals(0.81f, xtable.matchExact((long) 0, (long) 0, $("((x) &&+4 (y))"), null, dur, n).conf(), 0.05f);
        assertEquals(0.74f, xtable.matchExact((long) 0, (long) 0, $("((x) &&+6 (y))"), null, dur, n).conf(), 0.07f);
        assertEquals(0.75f, xtable.matchExact((long) 0, (long) 0, $("((x) &&+2 (y))"), null, dur, n).conf(), 0.07f);
        assertEquals(0.75f, xtable.matchExact((long) 0, (long) 0, $("((x) &&+0 (y))"), null, dur, n).conf(), 0.07f);
        assertEquals(0.62f, xtable.matchExact((long) 0, (long) 0, $("((x) &&-32 (y))"), null, dur, n).conf(), 0.2f);

        //TODO test dur = 1, 2, ... etc

    }

    @Test
    void testDynamicConceptValid1() throws Narsese.NarseseException {
        Term c =

                Op.CONJ.the(XTERNAL, new Term[]{$.$("(--,($1 ==>+- (((joy-->fz)&&fwd) &&+- $1)))"), $.$("(joy-->fz)"), $.$("fwd")}).normalize();

        assertTrue(c instanceof Compound, c::toString);
        assertTrue(Task.validTaskTerm(c), () -> c + " should be a valid task target");
    }

    @Test
    void testDynamicConceptValid2() throws Narsese.NarseseException {
        Term c =

                Op.CONJ.the(XTERNAL, new Term[]{$.$("(--,((--,#1)&&#2))"), $.$("(--,#2)"), $.varDep(1)}).normalize();

        assertTrue(c instanceof Compound, c::toString);
        assertTrue(Task.validTaskTerm(c), () -> c + " should be a valid task target");
    }

    @Test
    void testDynamicConjunctionXYZ() throws Narsese.NarseseException {


        NAR n = NARS.shell();
        n.believe("x", 1f, 0.50f);
        n.believe("y", 1f, 0.50f);
        n.believe("z", 0f, 0.81f);
        n.run(1);
        assertEquals(
                "%0.0;.20%", n.beliefTruth(
                        n.conceptualize($("(&&,x,y,z)")
                        ), n.time()).toString()
        );
        {

            Task bXYZ = n.belief($("(&&,x,y,z)"), n.time());
            assertEquals("(&&,x,y,z)", bXYZ.term().toString());
            assertEquals(3, bXYZ.stamp().length);
        }
        {

            Task bXY = n.belief($("(x && y)"), n.time());
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }
        {

            Task bXY = n.belief($("(x && y)"), ETERNAL);
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }

        assertEquals(
                "%0.0;.40%", n.beliefTruth(
                        n.conceptualize($("(&&,y,z)")
                        ), n.time()).toString()
        );
        assertEquals(
                "%1.0;.25%", n.beliefTruth(
                        n.conceptualize($("(&&,x,y)")
                        ), n.time()).toString()
        );
    }


    @Test
    void testDynamicConjConceptWithNegations() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        for (String s: new String[]{
                "((y-->t) &&+1 (t-->happy))",
                "(--(y-->t) &&+1 (t-->happy))",
                "((y-->t) &&+1 --(t-->happy))",
                "(--(y-->t) &&+1 --(t-->happy))",
        }) {
            Concept c = n.conceptualize($.$(s));
            assertTrue(((BeliefTables)c.beliefs()).tableFirst(DynamicTruthTable.class)!=null);
            assertTrue(((BeliefTables)c.goals()).tableFirst(DynamicTruthTable.class)!=null);
        }

    }

    @Test
    void testDynamicConjunctionFactored() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("x"), ETERNAL);
        n.believe($("y"), 0);
        n.believe($("z"), 2);
        n.time.dur(8);

        {
            Task t = n.answerBelief($$("(x&&y)"), 0);
            assertNotNull(t);
            assertTrue(t.isPositive());
        }
        {
            Task t = n.answerBelief($$("(x&&z)"), 2);
            assertNotNull(t);
            assertTrue(t.isPositive());
        }

        {
            Compound xyz = $("(x && (y &&+2 z))");
            assertEquals(
                    "[x @ 0..2, y @ 0..0, z @ 2..2]",
                    //"[(x&&y) @ 0..0, (x&&z) @ 2..2]",
                    conjDynComponents(xyz, 0, 0).toString());
            assertEquals(
                    "[x @ 0..4, y @ 0..2, z @ 2..4]",
                    //"[(x&&y) @ 0..2, (x&&z) @ 2..4]",
                    conjDynComponents(xyz, 0, 2).toString());

            Task t = n.answerBelief(xyz, 0, 2);
            assertNotNull(t);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, t.conf(), 0.4f);
            assertEq("((y &&+2 z)&&x)", t.term());
        }

        int dur = 0; //TODO test other durations
        TaskConcept cc = (TaskConcept) n.conceptualize($("(&&, x, y, z)"));
        BeliefTable xtable = cc.beliefs();
        {
            Term xyz = $("((x && y) &&+2 (x && z))");
            assertEq("((y &&+2 z)&&x)", xyz);
            Task t = xtable.matchExact((long) 0, (long) 0, xyz, null, dur, n);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, t.conf(), 0.4f);
        }
        {
            Task t = xtable.matchExact((long) 0, (long) 0, $("((x && y) &&+2 (x && z))"), null, dur, n);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, t.conf(), 0.4f);
        }


    }

    @Test
    void testDynamicConjunctionFactoredWithAllTemporalEvidence() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("x"), 0, 2);
        n.believe($("y"), 0);
        n.believe($("z"), 2);
        n.time.dur(8);

        {
            Term xyz = $("((y &&+2 z)&&x)");
            Task t = n.answerBelief(xyz, 0, 2);
            assertNotNull(t);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.73f, t.conf(), 0.1f);
            assertEq(xyz, t.term());
        }
        {
            //Term xyz = $("((x&&y) &&+2 z))");
            Term xyz = $("(&&+- ,x,y,z)");
            Task t = n.answerBelief(xyz, 0);
            assertNotNull(t);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.73f, t.conf(), 0.1f);
            assertEq("((y &&+2 z)&&x)", t.term());

        }
        {
            //Term xyz = $("((x&&y) &&+2 z))");
            Term xyz = $("(&&+- ,x,--y,z)");
            Task t = n.answerBelief(xyz, 0);
            assertNotNull(t);
            assertEquals(0f, t.freq(), 0.05f);
            assertEquals(0.73f, t.conf(), 0.1f);
            assertEq("(((--,y) &&+2 z)&&x)", t.term());

        }
    }

    @Test
    void testDynamicConjunction_collapseToRevisionOnIntersect() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("x"), 0, 2, 1f, 0.9f);
        n.believe($("x"), 0, 2, 0.5f, 0.9f);
        n.time.dur(8);

        for (String s : new String[] { "(x &&+1 x)", "(x &&+- x)"}){
            Term xyz = $(s);
            Task t = n.answerBelief(xyz, 0, 2);
            assertNotNull(t, ()->s + " -> null");
            assertEq("x", t.term());
            assertEquals(0.75f, t.freq(), 0.05f);
            assertEquals(0.94f, t.conf(), 0.1f);
        }
    }

    static List<String> components(AbstractDynamicTruth model, Compound xyz, long s, long e) {
        List<String> components = new FasterList();
        model.evalComponents(xyz,s, e,
                (what,whenStart,whenEnd)->{
                    components.add(what + " @ " + whenStart + ".." + whenEnd); return true;
                });
        return components;
    }

    private static List<String> conjDynComponents(Compound xyz, long s, long e) {
        return components(ConjIntersection, xyz, s, e);
    }

    @Test void CoNegatedXternal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("x"), 0, 0);
        n.believe($("--x"), 2,2);
        n.time.dur(2);

        float conf = 0.59f;
        //try all combinations.  there is only one valid result
        for (int i = 0; i < 8; i++) {
            Task t = n.answerBelief($("(x &&+- --x)"), 0, 2);
            assertNotNull(t);
            switch(t.term().toString()) {
                case "(x &&+2 (--,x))":
                    assertEquals(1f, t.freq(), 0.4f);
                    assertEquals(conf, t.conf(), 0.5f);
                    break;
                case "((--,x) &&+2 x)":
                    assertEquals(0f, t.freq(), 0.4f);
                    assertEquals(conf, t.conf(), 0.5f);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

//    @Test public void testDynamicIntersectionInvalidCommon() throws Narsese.NarseseException {
//        //TODO
//        NAR n = NARS.shell();
//        n.believe("(x&&+1):y", 0.75f, 0.50f);
//        n.believe("(x&&+2):z", 0.25f, 0.50f);
//        n.run(1);
//        Term xMinY = $("(x(x ~ y)");
//        Term yMinX = $("(y ~ x)");
//        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(xMinY).beliefs().getClass());
//        assertNull(
//                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
//        );
//    }
}
