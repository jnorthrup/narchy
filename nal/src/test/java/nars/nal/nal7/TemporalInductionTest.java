package nars.nal.nal7;

import jcog.math.FloatSupplier;
import jcog.meter.TemporalMetrics;
import jcog.meter.event.DoubleMeter;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.Activate;
import nars.exe.UniExec;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.test.TestNAR;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 6/8/15.
 */
public class TemporalInductionTest {

    @Test
    public void inductionDiffEventsAtom() {
        testInduction("before", "after", 10);
    }

    @Test
    public void inductionDiffEventsCompound() {
        testInduction("x:before", "x:after", 10);
    }

    @Test
    public void inductionDiffEventsCompoundNear() {
        testInduction("x:before", "x:after", 3);
    }

    @Test
    public void inductionDiffEventsNegPos() {
        testInduction("--x:before", "x:after", 4);
    }

    @Test
    public void inductionSameEvents() {
        testInduction("x", "x", 3);
    }

    @Test
    public void inductionSameEventsNeg() {
        testInduction("--x", "--x", 10);
    }

    @Test
    public void inductionSameEventsInvertPosNeg() {
        testInduction("x", "--x", 10);
    }

    @Test
    public void inductionSameEventsInvertNegPos() {
        testInduction("--x", "x", 10);
    }

    /** tests that conj and impl induction results dont have diminished
     * confidence as a result of temporal distance, as
     * would ordinarily happen due to using projected belief truth
     * rather than raw belief truth
     */
    static void testInduction(String a, String b, int dt) {
        int cycles = dt * 24;
        TestNAR t = new TestNAR(NARS.tmp())
                //.log()
//                .confTolerance(0.99f)
                .input(a + ". :|:")
                .inputAt(dt, b + ". :|:")
                .mustBelieve(cycles, "(" + a + " &&+" + dt + " " + b + ")", 1.00f, 0.81f /*intersectionConf*/, 0)
                .mustBelieve(cycles, "(" + a + " ==>+" + dt + " " + b + ")", 1.00f, 0.45f /*abductionConf*/, 0);
        if (!(a.contains("--") /*NEG*/ && a.equals(b)))
            t.mustBelieve(cycles, "(" + b + " ==>-" + dt + " " + a + ")", 1.00f, 0.45f /*inductionConf*/, dt);

        t.run(cycles);
    }



    @Test
    public void testTemporalRevision() throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.time.dur(1);


        //TextOutput.out(n);

        n.input("a:b. :|: %1.0;0.9%");
        n.run(5);
        n.input("a:b. :|: %0.0;0.9%");
        n.run(5);
        n.input("a:b. :|: %0.5;0.9%");
        n.run(1);

        //n.forEachConcept(Concept::print);

        TaskConcept c = (TaskConcept) n.conceptualize("a:b");
        assertNotNull(c);
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top(n.time()).toStringWithoutBudget());

        BeliefTable b = c.beliefs();
        b.print();
        assertTrue(3 <= b.size());

        //when originality is considered:
        //assertEquals("(b-->a). 5+0 %0.0;.90%", c.beliefs().top(n.time()).toStringWithoutBudget());

        //most current relevant overall:
        assertEquals(
                "(b-->a). 5 %0.0;.90%"
                //"(b-->a). 5 %.19;.92%"
                , n.belief(c.term(), 5).toStringWithoutBudget());


        //least relevant
        assertEquals(
                //"(b-->a). 0 %1.0;.90%"
                //"(b-->a). 0 %.83;.92%"
                "(b-->a). 0 %.86;.91%"
                , n.belief(c.term(), 0).toStringWithoutBudget());

    }

    @Test
    public void testTemporalRevisionOfTemporalRelation() throws Narsese.NarseseException {

        NAR n = NARS.tmp();

        //TextOutput.out(n);

        n.input("(a ==>+0 b). %1.0;0.7%");
        n.input("(a ==>+5 b). %1.0;0.6%");
        n.run(1);

        //n.forEachActiveConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }

    @Test
    public void testQuestionProjection() throws Narsese.NarseseException {

        NAR n = NARS.tmp();



        n.input("a:b. :|:");
        //n.frame();
        n.input("a:b? :/:");
        n.run(5);
        n.input("a:b? :/:");
        n.run(30);
        n.input("a:b? :/:");
        n.run(250);
        n.input("a:b? :/:");
        n.run(1);

        //n.forEachConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }

    @Test
    public void testInductionStability() throws Narsese.NarseseException {
        //two entirely disjoint events, and all inductable beliefs from them, should produce a finite system that doesn't explode
        NAR d = NARS.tmp();
        d.input("a:b. :|:");
        d.run(5);
        d.input("c:d. :|:");

        d.run(200);

        //everything should be inducted by now:
        int before = d.concepts.size();
        int numBeliefs = getBeliefCount(d);

        //System.out.println(numConcepts + " " + numBeliefs);

        d.run(60);

        //# unique concepts unchanged:
        int after = d.concepts.size();
        assertEquals(before, after);
        //assertEquals(numBeliefs, getBeliefCount(d));

    }


    private static int getBeliefCount(NAR n) {
        AtomicInteger a = new AtomicInteger(0);
        n.tasks(true, false, false, false).forEach(t -> {
            a.addAndGet(1);
        });
        return a.intValue();
    }

    static class PriMeter extends DoubleMeter {

        private final FloatSupplier getter;

        public PriMeter(NAR n, String id) {
            super("pri(" + id + ")", true);
            Term term = $.$$(id);
            this.getter = ()->{
                Concept cc = n.concept(term);
                if (cc == null)
                    return 0;
                Activate c = ((UniExec)(n.exe)).active.get(cc);
                if (c == null)
                    return 0;
                else return c.priElseZero();
            };
        }

        @Override
        public DoubleMeter reset() {
            set(getter.asFloat());
            return this;
        }


    }
    /**
     * higher-level rules learned from events, especially repeatd
     * events, "should" ultimately accumulate a higher priority than
     * the events themselves.
     */
    @Test public void testPriorityOfInductedRulesVsEventsThatItLearnedFrom() throws FileNotFoundException {
        NAR n = NARS.tmp();

        n.beliefPriDefault.set(0.1f);
        //n.deep.set(1f);


        TemporalMetrics m = new TemporalMetrics(1024);
        n.onCycle(()->m.update(n.time()));

        m.add(new PriMeter(n,"(0)"));
        m.add(new PriMeter(n,"(1)"));
        m.add(new PriMeter(n,"(2)"));
        m.add(new PriMeter(n,"((0) && (1))"));
        m.add(new PriMeter(n,"((0) ==> (1))"));
        m.add(new PriMeter(n,"((1) && (2))"));
        m.add(new PriMeter(n,"((1) ==> (2))"));


        //expose to repeat sequence
        int loops = 32, eventsPerLoop = 3, delayBetweenEvents = 2;
        for (int i = 0; i < loops; i++) {
            for (int j = 0; j < eventsPerLoop; j++) {
                n.believe($.p(j), Tense.Present);
                n.run(delayBetweenEvents);
            }
        }

//        m.printCSV4("/tmp/x.csv");
        m.printCSV4(System.out);
    }

}
