package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 6/8/15.
 */
class TemporalInductionTest {

    @Test
    void inductionDiffEventsAtom() {
        testInduction("before", "after", 10);
    }

    @Test
    void inductionDiffEventsCompound() {
        testInduction("x:before", "x:after", 10);
    }

    @Test
    void inductionDiffEventsCompoundNear() {
        testInduction("x:before", "x:after", 3);
    }

    @Test
    void inductionDiffEventsNegPos() {
        testInduction("(--,x:before)", "x:after", 4);
    }

    @Test
    void inductionSameEvents() {
        testInduction("x", "x", 3);
    }

    @Test
    void inductionSameEventsNeg() {
        testInduction("--x", "--x", 10);
    }

    @Test
    void inductionSameEventsInvertPosNeg() {
        testInduction("x", "--x", 10);
    }

    @Test
    void inductionSameEventsInvertNegPos() {
        testInduction("--x", "x", 10);
    }

    /** tests that conj and impl induction results dont have diminished
     * confidence as a result of temporal distance, as
     * would ordinarily happen due to using projected belief truth
     * rather than raw belief truth
     */
    private static void testInduction(String a, String b, int dt) {
        int cycles = dt * 20;
        boolean bNeg = b.startsWith("--");
        TestNAR t = new TestNAR(NARS.tmp())
            .logDebug()
                .input(a + ". |")
                .inputAt(dt, b + ". |")
                .mustBelieve(cycles, '(' + a + " &&+" + dt + ' ' + b + ')', 1.00f, 0.81f /*intersectionConf*/, 0)
                .mustBelieve(cycles, '(' + a + " ==>+" + dt + ' ' + (bNeg ? b.substring(2) /* unneg */ : b) + ')',
                        bNeg ? 0 : 1.00f,
                        0.45f /*abductionConf*/, 0)
        ;
//        if (!(a.contains("--") /*NEG*/ && a.equals(b)))
//            t.mustBelieve(cycles, '(' + b + " ==>-" + dt + ' ' + a + ')', 1.00f, 0.45f /*inductionConf*/, dt);

        t.run(cycles);
    }



    @Test
    void testTemporalRevision() throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.time.dur(1);


        

        n.input("a:b. :|: %1.0;0.9%");
        n.run(5);
        n.input("a:b. :|: %0.0;0.9%");
        n.run(5);
        n.input("a:b. :|: %0.5;0.9%");
        n.run(1);

        

        TaskConcept c = (TaskConcept) n.conceptualize("a:b");
        assertNotNull(c);
        

        BeliefTable b = c.beliefs();
        b.print();
        assertTrue(3 <= b.taskCount());


        Task x = n.belief(c.term(), 5);
        assertTrue(x.toStringWithoutBudget().startsWith("(b-->a). 5"));
        assertTrue(x.isNegative());


        Task y = n.belief(c.term(), 0);
        assertTrue(y.toStringWithoutBudget().startsWith("(b-->a). 0"));
        assertTrue(y.isPositive());

    }

    @Test
    void testTemporalRevisionOfTemporalRelation() throws Narsese.NarseseException {

        NAR n = NARS.tmp();

        

        n.input("(a ==>+0 b). %1.0;0.7%");
        n.input("(a ==>+5 b). %1.0;0.6%");
        n.run(1);

        
        //TODO
        
        
    }
//
//    @Test
//    void testQuestionProjection() throws Narsese.NarseseException {
//
//        NAR n = NARS.tmp();
//
//
//
//        n.input("a:b. :|:");
//
//        n.input("a:b? :/:");
//        n.run(5);
//        n.input("a:b? :/:");
//        n.run(30);
//        n.input("a:b? :/:");
//        n.run(250);
//        n.input("a:b? :/:");
//        n.run(1);
//
//
//
//
//
//    }

    @Test
    void testInductionStability() throws Narsese.NarseseException {
        
        NAR d = NARS.tmp();
        d.input("a:b. :|:");
        d.run(5);
        d.input("c:d. :|:");

        d.run(200);

        
        int before = d.memory.size();
        int numBeliefs = getBeliefCount(d);

        

        d.run(60);

        
        int after = d.memory.size();
        assertEquals(before, after);
        

    }


    private static int getBeliefCount(NAR n) {
        AtomicInteger a = new AtomicInteger(0);
        n.tasks(true, false, false, false).forEach(t -> {
            a.addAndGet(1);
        });
        return a.intValue();
    }

//    static class PriMeter extends DoubleMeter {
//
//        private final FloatSupplier getter;
//
//        PriMeter(NAR n, String id) {
//            super("pri(" + id + ")", true);
//            Term term = $.$$(id);
//            this.getter = ()->{
//                Concept cc = n.concept(term);
//                if (cc == null)
//                    return 0;
//                return n.concepts.pri(cc, 0);
//            };
//        }
//
//        @Override
//        public DoubleMeter reset() {
//            set(getter.asFloat());
//            return this;
//        }
//
//
//    }
//    /**
//     * higher-level rules learned from events, especially repeatd
//     * events, "should" ultimately accumulate a higher priority than
//     * the events themselves.
//     */
//    @Test
//    void testPriorityOfInductedRulesVsEventsThatItLearnedFrom() {
//        NAR n = NARS.tmp();
//
//        n.beliefPriDefault.set(0.1f);
//
//
//
//        TemporalMetrics m = new TemporalMetrics(1024);
//        n.onCycle(()->m.update(n.time()));
//
//        m.add(new PriMeter(n,"(0)"));
//        m.add(new PriMeter(n,"(1)"));
//        m.add(new PriMeter(n,"(2)"));
//        m.add(new PriMeter(n,"((0) && (1))"));
//        m.add(new PriMeter(n,"((0) ==> (1))"));
//        m.add(new PriMeter(n,"((1) && (2))"));
//        m.add(new PriMeter(n,"((1) ==> (2))"));
//
//
//
//        int loops = 32, eventsPerLoop = 3, delayBetweenEvents = 2;
//        for (int i = 0; i < loops; i++) {
//            for (int j = 0; j < eventsPerLoop; j++) {
//                n.believe($.p(j), Tense.Present);
//                n.run(delayBetweenEvents);
//            }
//        }
//
//
//        m.printCSV4(System.out);
//    }


}
