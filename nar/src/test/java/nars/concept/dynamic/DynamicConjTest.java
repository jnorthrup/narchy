package nars.concept.dynamic;

import jcog.data.list.FasterList;
import jcog.util.ObjectLongLongPredicate;
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
import java.util.function.Supplier;

import static nars.$.*;
import static nars.$.*;
import static nars.Op.BELIEF;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.dynamic.DynamicConjTruth.ConjIntersection;
import static org.junit.jupiter.api.Assertions.*;

class DynamicConjTest extends AbstractDynamicTaskTest {

    @Test
    void testDynamicConjunction2() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        n.run(1);
        long now = n.time();

        assertEquals($.INSTANCE.t(1f, 0.81f), n.beliefTruth(INSTANCE.$("(a:x && a:y)"), now));

        assertEquals($.INSTANCE.t(0f, 0.81f), n.beliefTruth("(a:x && (--,a:y))", now));

        assertEquals($.INSTANCE.t(1f, 0.81f), n.belief(INSTANCE.$("(a:x && a:y)"), now).truth());

        assertEquals($.INSTANCE.t(0f, 0.81f), n.beliefTruth("(b:x && a:y)", now));
        assertEquals($.INSTANCE.t(1f, 0.81f), n.beliefTruth("((--,b:x) && a:y)", now));
        assertEquals($.INSTANCE.t(0f, 0.81f), n.beliefTruth("((--,b:x) && (--,a:y))", now));
    }

    @Test
    void testDynamicConjunctionEternalOverride() throws Narsese.NarseseException {
        NAR n = NARS.shell()
                .believe(INSTANCE.$$("a:x"), 0)
                .believe(INSTANCE.$$("a:y"), 0);

        final long now = 0; //n.time();
        assertEquals($.INSTANCE.t(1f, 0.81f), n.beliefTruth(INSTANCE.$("(a:x && a:y)"), now));

        {
            //temporal evaluated a specific point
            Task xy = n.belief(INSTANCE.$("(a:x && a:y)"), now);
            assertEquals("((x-->a)&&(y-->a))", xy.term().toString());
            assertEquals($.INSTANCE.t(1f, 0.81f), xy.truth());
        }

        {
            Task xAndNoty = n.belief(INSTANCE.$("(a:x && --a:y)"), now);
            assertEquals("((--,(y-->a))&&(x-->a))", xAndNoty.term().toString());
            assertEquals($.INSTANCE.t(0f, 0.81f), xAndNoty.truth());
        }

        {
            //remain eternal
            Task xy = n.belief(INSTANCE.$("(a:x && a:y)"), ETERNAL);
            assertNotNull(xy);
            assertEquals(0, xy.start()); //exact time since it was what was stored
            assertEquals("((x-->a)&&(y-->a))", xy.term().toString());
            assertEquals($.INSTANCE.t(1f, 0.81f), xy.truth());
        }


        //override or revise dynamic with an input belief
        {
            n.believe(INSTANCE.$$("--(a:x && a:y)"), 0);
            assertEquals(1, n.concept("(a:x && a:y)").beliefs().taskCount());

            Task ttEte = n.answerBelief(INSTANCE.$("(a:x && a:y)"), now);
//            assertEquals(1, ttEte.stamp().length);


            assertTrue(ttEte.toString().contains("((x-->a)&&(y-->a)). 0 %"));

            Truth tNow = n.beliefTruth(INSTANCE.$("(a:x && a:y)"), now);
            assertTrue(
                    $.INSTANCE.t(0.32f, 0.93f /*0.87f*/)
                    //$.t(0.00f, 0.90f)
                    //$.t(0.32f, 0.90f /*0.87f*/)
                            .equalTruth(tNow, n), new Supplier<String>() {
                        @Override
                        public String get() {
                            return "was " + tNow + " at " + now;
                        }
                    });

        }
        {
            n.believe(INSTANCE.$$("--(a:x && a:y)"), 0);
            assertTrue(2 <= n.concept("(a:x && a:y)").beliefs().taskCount());

            Task ttNow = n.answerBelief(INSTANCE.$("(a:x && a:y)"), now);
            assertTrue(ttNow.isNegative());
            assertTrue(ttNow.toString().contains("((x-->a)&&(y-->a)). 0"), ttNow::toString);
        }


        Task tAfterTask = n.belief(INSTANCE.$("(a:x && a:y)"), now + 2);
        assertEquals(now + 2, tAfterTask.start());
        assertEquals(now + 2, tAfterTask.end());

        Truth tAfter = n.beliefTruth(INSTANCE.$("(a:x && a:y)"), now + 2);
        assertNotNull(tAfter);
        assertTrue(tAfter.isNegative());
        //assertTrue($.t(0.19f, 0.88f).equalsIn(tAfter, n), () -> tAfter.toString());

        Truth tLater = n.beliefTruth(INSTANCE.$("(a:x && a:y)"), now + 5);
        assertTrue(tLater.isPositive() == tAfter.isPositive());
        assertTrue(tLater.conf() < tAfter.conf());
        //assertTrue($.t(0.19f, 0.79f).equalsIn(tLater, n), () -> tLater.toString());
    }

    @Test
    void testDynamicConjunctionEternalTemporalMix() throws Narsese.NarseseException {

//        String xx = "((e&&x)&&(e&&y))";
//        assertEquals(xx, $$("((x&&y)&&e)").toString());

        NAR n = NARS.shell()
                .believe(INSTANCE.$$("x"), 0)
                .believe(INSTANCE.$$("y"), 0)
                .believe(INSTANCE.$$("e"), ETERNAL);

        Term xye = INSTANCE.$("(&&,x,y,e)");

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
        assertEquals($.INSTANCE.t(1f, 0.81f), n.beliefTruth(INSTANCE.$("(a:x && a:y)"), now));

        n.believe(INSTANCE.$$("--(a:x && a:y)"), now);


        Truth tt = n.belief(INSTANCE.$("(a:x && a:y)"), now).truth();
        assertTrue($.INSTANCE.t(0.32f, 0.93f).equalTruth(tt, n), tt::toString);
    }

    @Test
    void testDynamicConjunction3() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        TaskConcept cc = (TaskConcept) n.conceptualize(INSTANCE.$("(&&, a:x, a:y, a:z)"));
        Truth now = n.beliefTruth(cc, n.time());
        assertNotNull(now);
        assertTrue($.INSTANCE.t(1f, 0.73f).equalTruth(now, 0.1f), new Supplier<String>() {
            @Override
            public String get() {
                return now + " truth at " + n.time();
            }
        });


        {
            TaskConcept ccn = (TaskConcept) n.conceptualize(INSTANCE.$("(&&, a:x, a:w)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertNull(nown);
        }


        Concept ccn = n.conceptualize(INSTANCE.$("(&&, a:x, (--, a:y), a:z)"));

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
        Task ay = n.belief(INSTANCE.$$("a:y"));
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
        n.believe(INSTANCE.$("x"));
        n.believe(INSTANCE.$("y"));
        n.believe(INSTANCE.$("--z"));


        for (long w: new long[]{ETERNAL, 0, 1}) {
            assertEquals($.INSTANCE.t(1, 0.81f), n.truth(INSTANCE.$("(x && y)"), BELIEF, w));
            assertEquals($.INSTANCE.t(0, 0.81f), n.truth(INSTANCE.$("(x && --y)"), BELIEF, w));
            assertEquals($.INSTANCE.t(1, 0.81f), n.truth(INSTANCE.$("(x && --z)"), BELIEF, w));
        }
    }

    @Test
    void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe(INSTANCE.$("(x)"), 0);
        n.believe(INSTANCE.$("(y)"), 4);
        n.time.dur(8);
        TaskConcept cc = (TaskConcept) n.conceptualize(INSTANCE.$("((x) && (y))"));

        BeliefTable xtable = cc.beliefs();


        int dur = 0;
        assertEquals(0.81f, xtable.matchExact((long) 0, (long) 0, INSTANCE.$("((x) &&+4 (y))"), null, dur, n).conf(), 0.05f);
        assertEquals(0.74f, xtable.matchExact((long) 0, (long) 0, INSTANCE.$("((x) &&+6 (y))"), null, dur, n).conf(), 0.07f);
        assertEquals(0.75f, xtable.matchExact((long) 0, (long) 0, INSTANCE.$("((x) &&+2 (y))"), null, dur, n).conf(), 0.07f);
        assertEquals(0.75f, xtable.matchExact((long) 0, (long) 0, INSTANCE.$("((x) &&+0 (y))"), null, dur, n).conf(), 0.07f);
        assertEquals(0.62f, xtable.matchExact((long) 0, (long) 0, INSTANCE.$("((x) &&-32 (y))"), null, dur, n).conf(), 0.2f);

        //TODO test dur = 1, 2, ... etc

    }

    @Test
    void testDynamicConceptValid1() throws Narsese.NarseseException {
        Term c =

                Op.CONJ.the(XTERNAL, $.INSTANCE.$("(--,($1 ==>+- (((joy-->fz)&&fwd) &&+- $1)))"), $.INSTANCE.$("(joy-->fz)"), $.INSTANCE.$("fwd")).normalize();

        assertTrue(c instanceof Compound, c::toString);
        assertTrue(Task.validTaskTerm(c), new Supplier<String>() {
            @Override
            public String get() {
                return c + " should be a valid task target";
            }
        });
    }

    @Test
    void testDynamicConceptValid2() throws Narsese.NarseseException {
        Term c =

                Op.CONJ.the(XTERNAL, $.INSTANCE.$("(--,((--,#1)&&#2))"), $.INSTANCE.$("(--,#2)"), $.INSTANCE.varDep(1)).normalize();

        assertTrue(c instanceof Compound, c::toString);
        assertTrue(Task.validTaskTerm(c), new Supplier<String>() {
            @Override
            public String get() {
                return c + " should be a valid task target";
            }
        });
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
                        n.conceptualize(INSTANCE.$("(&&,x,y,z)")
                        ), n.time()).toString()
        );
        {

            Task bXYZ = n.belief(INSTANCE.$("(&&,x,y,z)"), n.time());
            assertEquals("(&&,x,y,z)", bXYZ.term().toString());
            assertEquals(3, bXYZ.stamp().length);
        }
        {

            Task bXY = n.belief(INSTANCE.$("(x && y)"), n.time());
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }
        {

            Task bXY = n.belief(INSTANCE.$("(x && y)"), ETERNAL);
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }

        assertEquals(
                "%0.0;.40%", n.beliefTruth(
                        n.conceptualize(INSTANCE.$("(&&,y,z)")
                        ), n.time()).toString()
        );
        assertEquals(
                "%1.0;.25%", n.beliefTruth(
                        n.conceptualize(INSTANCE.$("(&&,x,y)")
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
            Concept c = n.conceptualize($.INSTANCE.$(s));
            assertTrue(((BeliefTables)c.beliefs()).tableFirst(DynamicTruthTable.class)!=null);
            assertTrue(((BeliefTables)c.goals()).tableFirst(DynamicTruthTable.class)!=null);
        }

    }

    @Test
    void testDynamicConjunctionFactored() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe(INSTANCE.$("x"), ETERNAL);
        n.believe(INSTANCE.$("y"), 0);
        n.believe(INSTANCE.$("z"), 2);
        n.time.dur(8);

        {
            Task t = n.answerBelief(INSTANCE.$$("(x&&y)"), 0);
            assertNotNull(t);
            assertTrue(t.isPositive());
        }
        {
            Task t = n.answerBelief(INSTANCE.$$("(x&&z)"), 2);
            assertNotNull(t);
            assertTrue(t.isPositive());
        }

        {
            Compound xyz = INSTANCE.$("(x && (y &&+2 z))");
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
        TaskConcept cc = (TaskConcept) n.conceptualize(INSTANCE.$("(&&, x, y, z)"));
        BeliefTable xtable = cc.beliefs();
        {
            Term xyz = INSTANCE.$("((x && y) &&+2 (x && z))");
            assertEq("((y &&+2 z)&&x)", xyz);
            Task t = xtable.matchExact((long) 0, (long) 0, xyz, null, dur, n);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, t.conf(), 0.4f);
        }
        {
            Task t = xtable.matchExact((long) 0, (long) 0, INSTANCE.$("((x && y) &&+2 (x && z))"), null, dur, n);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.81f, t.conf(), 0.4f);
        }


    }

    @Test
    void testDynamicConjunctionFactoredWithAllTemporalEvidence() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe(INSTANCE.$("x"), 0, 2);
        n.believe(INSTANCE.$("y"), 0);
        n.believe(INSTANCE.$("z"), 2);
        n.time.dur(8);

        {
            Term xyz = INSTANCE.$("((y &&+2 z)&&x)");
            Task t = n.answerBelief(xyz, 0, 2);
            assertNotNull(t);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.73f, t.conf(), 0.1f);
            assertEq(xyz, t.term());
        }
        {
            //Term xyz = $("((x&&y) &&+2 z))");
            Term xyz = INSTANCE.$("(&&+- ,x,y,z)");
            Task t = n.answerBelief(xyz, 0);
            assertNotNull(t);
            assertEquals(1f, t.freq(), 0.05f);
            assertEquals(0.73f, t.conf(), 0.1f);
            assertEq("((y &&+2 z)&&x)", t.term());

        }
        {
            //Term xyz = $("((x&&y) &&+2 z))");
            Term xyz = INSTANCE.$("(&&+- ,x,--y,z)");
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
        n.believe(INSTANCE.$("x"), 0, 2, 1f, 0.9f);
        n.believe(INSTANCE.$("x"), 0, 2, 0.5f, 0.9f);
        n.time.dur(8);

        for (String s : new String[] { "(x &&+1 x)", "(x &&+- x)"}){
            Term xyz = INSTANCE.$(s);
            Task t = n.answerBelief(xyz, 0, 2);
            assertNotNull(t, new Supplier<String>() {
                @Override
                public String get() {
                    return s + " -> null";
                }
            });
            assertEq("x", t.term());
            assertEquals(0.75f, t.freq(), 0.05f);
            assertEquals(0.94f, t.conf(), 0.1f);
        }
    }

    static List<String> components(AbstractDynamicTruth model, Compound xyz, long s, long e) {
        List<String> components = new FasterList();
        model.evalComponents(xyz,s, e,
                new ObjectLongLongPredicate<Term>() {
                    @Override
                    public boolean accept(Term what, long whenStart, long whenEnd) {
                        components.add(what + " @ " + whenStart + ".." + whenEnd);
                        return true;
                    }
                });
        return components;
    }

    private static List<String> conjDynComponents(Compound xyz, long s, long e) {
        return components(ConjIntersection, xyz, s, e);
    }

    @Test void CoNegatedXternal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe(INSTANCE.$("x"), 0, 0);
        n.believe(INSTANCE.$("--x"), 2,2);
        n.time.dur(2);

        float conf = 0.59f;
        //try all combinations.  there is only one valid result
        for (int i = 0; i < 8; i++) {
            Task t = n.answerBelief(INSTANCE.$("(x &&+- --x)"), 0, 2);
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

    @Test void depVarContent() {
        assertFalse(isDynamicTable("(x && #1)"), "no way to decompose");
        assertTrue(isDynamicTable("(&&,x,y,#1)"), "decomposable two ways, paired with either x or y");
    }

    @Test void conjSeqWithDepVar() throws Narsese.NarseseException {

            NAR n = NARS.shell();
            n.believe(INSTANCE.$("(x && #1)"), 0);
            n.believe(INSTANCE.$("y"), 1);
            //n.believe($("(y && #1)"), 2);
            n.time.dur(8);

            Task t = n.answerBelief(INSTANCE.$$("((x&&#1) &&+1 y)"), 0);
            assertNotNull(t);

    }
    @Test void conjSeqWithDepVarSeq() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe(INSTANCE.$("(x &&+1 #1)"), 0);
        n.believe(INSTANCE.$("y"), 2);
        //n.believe($("(y && #1)"), 2);
        n.time.dur(8);

        Task T = null;
        //try because there are 2 solutions, one will be null
        for (int i = 0; i < 16; i++) {
            Task t = n.answerBelief(INSTANCE.$$("((x &&+1 #1) &&+1 y)"), 0);
            if (t!=null) {
                T = t;
                break;
            }
        }
        assertNotNull(T);
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
