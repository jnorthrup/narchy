//package nars.derive.time;
//
//import nars.$;
//import nars.term.Term;
//import org.junit.jupiter.api.Test;
//
//import static nars.Op.CONJ;
//import static nars.Op.IMPL;
//import static nars.time.Tense.DTERNAL;
//import static nars.time.Tense.ETERNAL;
//import static nars.time.Tense.TIMELESS;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class DeriveTimeTest {
//
//    static final Term x = $.the("x");
//    static final Term y = $.the("y");
//    static final Term z = $.the("z");
//
//    @Test public void testSimple1() {
//        assertSolution(x, 1, 1, abs(x, 1));
//    }
//
//    @Test public void testSimple2() {
//        assertSolution(x, 1, 2, abs(x, 1), abs(x, 2));
//    }
//
//    @Test public void testSimpleEternal() {
//        assertSolution(x, 1, 1, abs(x, ETERNAL), abs(x, 1));
//    }
//
//    @Test public void testSimpleAcrossDuration() {
//        assertSolution(CONJ.the(x,2,x), 1, 1, abs(x, 1), abs(x, 3));
//    }
//
//    @Test public void testConj1() {
//        assertSolution(CONJ.the(x, 1, y), 1, 1, abs(x, 1), abs(y, 2));
//    }
//
//    @Test public void testConjSomeEternal() {
//        assertSolution(CONJ.the(x, DTERNAL, y), 1, 1, abs(x, 1), abs(y, ETERNAL));
//    }
//    @Test public void testConjAllEternal() {
//        assertSolution(CONJ.the(x, DTERNAL, y), ETERNAL, ETERNAL, abs(x, ETERNAL), abs(y, ETERNAL));
//    }
//    @Test public void testConflictingEternalAndOtherTemporal() {
//        //the eternals cancel-out
//        assertSolution(x, 0, 0, abs(x, 0), abs(y, ETERNAL), abs(y.neg(), ETERNAL));
//    }
//
//    @Test public void testConj2Eternal() {
//        assertSolution(CONJ.the(z, CONJ.the(x, 1, y)), 1, 1, abs(x, 1), abs(y, 2), abs(z, ETERNAL));
//    }
//
//    @Test public void testOneImplication() {
//        assertSolution(null, ETERNAL, abs(IMPL.the(x,y), 1), abs(z, 2));
//    }
//    @Test public void testAllImplicationsWithinDuration() {
//        assertSolution(IMPL.the(x,y), 1, 2, abs(IMPL.the(x,y), 1), abs(IMPL.the(x,y), 2));
//    }
//    @Test public void testAllImplicationsBeyondDuration() {
//        assertSolution(null, ETERNAL, abs(IMPL.the(x,y), 1), abs(IMPL.the(x,y), 3));
//    }
//
//    static TimeGraph.Absolute abs(Term term, long when) {
//        return new TimeGraph.Absolute(term, when);
//    }
//
//    static void assertSolution(Term yExpected, long occ, TimeGraph.Event... events) {
//        assertSolution(yExpected, occ, occ, events);
//    }
//
//    static void assertSolution(Term yExpected, long start, long end, TimeGraph.Event... events) {
//        long[] occ = new long[] { TIMELESS, TIMELESS };
//        Term y = DeriveTime.solveMerged(1, occ, events);
//
//        assertEquals(yExpected, y);
//        assertEquals(start, occ[0]);
//        assertEquals(end, occ[1]);
//    }
//}