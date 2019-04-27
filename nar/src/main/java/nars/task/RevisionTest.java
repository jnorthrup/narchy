package nars.task;

import com.google.common.collect.Lists;
import jcog.pri.ScalarValue;
import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.polation.LinearTruthProjection;
import nars.truth.polation.TruthIntegration;
import nars.truth.polation.TruthProjection;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2w;
import static nars.truth.func.TruthFunctions.w2c;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/18/16.
 */
public class RevisionTest {

    public final static Term x = $.the("x");
    private final NAR n = NARS.shell();

    @Test
    void testRevisionEquivalence() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, 0);
        a.evidence(0);
        TaskBuilder b = t(1f, 0.5f, 0);
        b.evidence(1);


        assertEquals(Revision.revise(a, b),
                new LinearTruthProjection(0, 0, n.dur())
                        .add(Lists.newArrayList(a.apply(n), b.apply(n)))
                        .truth());
    }

    @Test void testCoincidentTasks() throws Narsese.NarseseException {
        Task t01 = merge(t(1, 0.9f, 0, 0).apply(n), t(1, 0.9f, 0, 0).apply(n), n);
        assertNotNull(t01);
        assertEquals("(b-->a). 0 %1.0;.95%", t01.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t01.stamp()));
    }

    @Test void testPartiallyCoincidentTasks() throws Narsese.NarseseException {
        Task t01 = merge(t(1, 0.9f, 0, 0).apply(n), t(1, 0.9f, 0, 1).apply(n), n);
        assertNotNull(t01);
        assertEquals("(b-->a). 0⋈1 %1.0;.93%", t01.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t01.stamp()));
    }

    @Test void testAdjacentTasks() throws Narsese.NarseseException {
        Task t01 = merge(t(1, 0.9f, 0, 0).apply(n), t(1, 0.9f, 1, 1).apply(n), n);
        assertNotNull(t01);
        assertEquals("(b-->a). 0⋈1 %1.0;.90%", t01.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t01.stamp()));
    }

    /** test solutions are optimal in cases where one or more tasks have
     * overlapping evidence.  including cases where the top ranked merge
     * result is best excluded.
     */
    @Test void testOverlapConflict() throws Narsese.NarseseException {
        Pair<Task, TruthProjection> rr = Revision.merge(n, true,
                new Task[]{
                        t(0, 0.71f, 0, 0).evidence(1, 2).apply(n),
                        t(1, 0.7f, 0, 0).evidence(1).apply(n),
                        t(1, 0.7f, 0, 0).evidence(2).apply(n)});
        assertNotNull(rr);
        Task t = rr.getOne();

        assertNotNull(t);
        assertEquals("(b-->a). 0 %1.0;.82%", t.toStringWithoutBudget());
        assertEquals("[1, 2]", Arrays.toString(t.stamp()));

    }

    @Test void testNonAdjacentTasks() throws Narsese.NarseseException {
//        if (Param.REVISION_ALLOW_DILUTE_UNION) { //HACK requires truth dilution to be enabled, which ideally will be controlled on a per-revision basis. not statically
            NAL<NAL<NAR>> n = NARS.shell();

            Task t01 = t(1, 0.9f, 0, 1).apply(n);
            Task t02 = t(1, 0.9f, 0, 2).apply(n);
            Task t03 = t(1, 0.9f, 0, 3).apply(n);
            Task t35 = t(1, 0.9f, 3, 5).apply(n);
            Task t45 = t(1, 0.9f, 4, 5).apply(n);
            Task t100_102 = t(1, 0.9f, 100, 102).apply(n);

            //evidence density
            Task a = merge(t01, t45, n);
            Task b = merge(t02, t45, n);
            Task c = merge(t03, t45, n);
            assertNotNull(a);
            assertNotNull(b);
            assertTrue(a.evi() < b.evi());
            assertNotNull(c);
            assertTrue(b.evi() < c.evi());

            assertEquals("(b-->a). 0⋈102 %1.0;.41%", merge(t02, t100_102, n).toStringWithoutBudget());

            assertTrue(merge(t03, t35, n).toStringWithoutBudget().startsWith("(b-->a). 0⋈5 %1.0;.9"));
            assertTrue(merge(t02, t35, n).toStringWithoutBudget().startsWith("(b-->a). 0⋈5 %1.0;.9"));
//        }

    }

    private Task merge(Task t01, Task t45, NAL<NAL<NAR>> n) {
        return Revision.merge(n, false, new Task[] { t01, t45 }).getOne();
    }


    @Test
    void testRevisionInequivalenceDueToTemporalSeparation() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, -4).evidence(1);
        TaskBuilder b = t(0f, 0.5f, 4).evidence(2);

        int dur = 9;
        Truth pt = new LinearTruthProjection(0, 0, dur).add(Lists.newArrayList(a.apply(n), b.apply(n))).truth();
        @Nullable Truth rt = Revision.revise(a, b);

        assertEquals(pt.freq(), rt.freq(), 0.01f);
        assertTrue(pt.conf() < rt.conf());

    }


    @Test
    void testRevisionEquivalence2Instant() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, 0);
        TaskBuilder b = t(0f, 0.5f, 0);
        assertEquals(Revision.revise(a, b), new LinearTruthProjection(0, 0, 1).add(Lists.newArrayList(a.apply(n), b.apply(n))).truth());
    }

    @Test
    void testPolation1() throws Narsese.NarseseException {

        int dur = 1;

        Task a = t(1f, 0.9f, 3).apply(n);
        Task b = t(0f, 0.9f, 6).apply(n);
        for (int i = 0; i < 10; i++) {
            System.out.println(i + " " +
                    new LinearTruthProjection(i, i, dur).add(Lists.newArrayList(a, b)).truth());
        }

        System.out.println();

        Truth ab2 = new LinearTruthProjection(3, 3, dur).add(Lists.newArrayList(a, b)).truth();
        assertTrue(ab2.conf() >= 0.5f);

        Truth abneg1 = new LinearTruthProjection(3, 3, dur).add(Lists.newArrayList(a, b)).truth();
        assertTrue(abneg1.freq() > 0.51f);
        assertTrue(abneg1.conf() >= 0.5f);

        Truth ab5 = new LinearTruthProjection(6, 6, dur).add(List.of(a, b)).truth();
        assertTrue(ab5.freq() < 0.35f);
        assertTrue(ab5.conf() >= 0.5f);
    }

    @Test
    void testRevisionEquivalence4() throws Narsese.NarseseException {
        Task a = t(0f, 0.1f, 3).evidence(1).apply(n);
        Task b = t(0f, 0.1f, 4).evidence(2).apply(n);
        Task c = t(1f, 0.1f, 5).evidence(3).apply(n);
        Task d = t(0f, 0.1f, 6).evidence(4).apply(n);
        Task e = t(0f, 0.1f, 7).evidence(5).apply(n);

        for (int i = 0; i < 15; i++) {
            System.out.println(i + " " + new LinearTruthProjection(i, i, 1).add(Lists.newArrayList(a, b, c, d, e)).truth());
        }

    }

    private static TaskBuilder t(float freq, float conf, long occ) throws Narsese.NarseseException {
        return new TaskBuilder("a:b", BELIEF, $.t(freq, conf)).time( 0, occ, occ);
    }

    static TaskBuilder t(float freq, float conf, long start, long end) throws Narsese.NarseseException {
        return new TaskBuilder("a:b", BELIEF, $.t(freq, conf)).time(0, start, end);
    }


//
//    static void print(@NotNull List<Task> l, int start, int end) {
//
//        System.out.println("INPUT");
//        for (Task t : l) {
//            System.out.println(t);
//        }
//
//        System.out.println();
//
//        System.out.println("TRUTHPOLATION");
//        for (long d = start; d < end; d++) {
//            Truth a1 = new FocusingLinearTruthPolation(d, d, 1).addAt(l).truth();
//            System.out.println(d + ": " + a1);
//        }
//    }


    @Test
    void testTemporalProjectionInterpolation() throws Narsese.NarseseException {


        int maxBeliefs = 12;
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");
        b.believe(0.5f, 1.0f, 0.85f, 5);
        b.believe(0.5f, 0.0f, 0.85f, 10);
        b.believe(0.5f, 1.0f, 0.85f, 15);
        b.run(1);

        assertTrue(3 <= b.size(true));

        int period = 1;
        int loops = 20;

        Set<Task> tops = new HashSet();
        for (int i = 0; i < loops; i++) {


            b.run(period);

            long now = b.nar.time();

            Task tt = n.belief(b.concept().term(), now);
            tops.add(tt);

            System.out.println(now + " " + tt);

        }

        assertTrue(3 <= tops.size(), "all beliefs covered");

        b.print();

    }

    @Test
    void testTemporalProjectionConfidenceAccumulation2_1() {
        testConfidenceAccumulation(2, 1f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation2_5() {
        testConfidenceAccumulation(2, 1f, 0.5f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation2_9() {

        testConfidenceAccumulation(2, 1f, 0.9f);
        testConfidenceAccumulation(2, 0.5f, 0.9f);
        testConfidenceAccumulation(2, 0f, 0.9f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_1_pos() {
        testConfidenceAccumulation(3, 1f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_1_neg() {
        testConfidenceAccumulation(3, 0f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_1_mid() {
        testConfidenceAccumulation(3, 0.5f, 0.1f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_5() {
        testConfidenceAccumulation(3, 1f, 0.5f);
    }

    @Test
    void testTemporalProjectionConfidenceAccumulation3_9() {
        testConfidenceAccumulation(3, 1f, 0.9f);
    }


    private void testConfidenceAccumulation(int repeats, float freq, float inConf) {
        int maxBeliefs = repeats * 4;

        NAR n = newNAR(maxBeliefs);


        long at = 5;

        float outConf = w2c(c2w(inConf) * repeats);

        BeliefAnalysis b = null;
        try {
            b = new BeliefAnalysis(n, "<a-->b>");
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        for (int i = 0; i < repeats; i++) {
            b.believe(0.5f, freq, inConf, at);
        }

        b.run(1);

        b.print();
        assertTrue(repeats <= b.size(true));

        @Nullable Truth result = n.beliefTruth(b, at);
        assertEquals(freq, result.freq(), 0.25f);
        assertEquals(outConf, result.conf(), 0.25f);
    }


    @Test
    void testTemporalRevection() throws Narsese.NarseseException {


        int maxBeliefs = 4;
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");


        b.believe(0.5f, 0.0f, 0.85f, 5);
        n.run();
        b.believe(0.5f, 0.95f, 0.85f, 10);
        n.run();
        b.believe(0.5f, 1.0f, 0.85f, 11);
        n.run();

        b.print();
        assertTrue(3 <= b.size(true));


        b.believe(0.5f, 1.0f, 0.99f, 12);


        n.run(3);
        b.print();

        assertEquals(4, b.size(true));

        b.print();

//        assertEquals(5, b.wave().start());
//        assertEquals(12, b.wave().end());
//
//        assertEquals(5, b.wave().start());
//        assertEquals(11, b.wave().end());

    }

    @Test
    void testSequenceIntermpolation1() throws Narsese.NarseseException {

        Compound a = $.$("(((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))) &&+11 (bx-->noid))");
        Compound b = $.$("(((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&|(happy-->noid)))");
        Term ar = a.root();
        Term br = b.root();
        assertEquals(ar, br);
        assertEquals(a.concept(), b.concept());

        TreeSet<Term> outcomes = new TreeSet();

        int misses = 0;
        for (int i = 0; i < 10; i++) {
            Term c = Intermpolate.intermpolate(a, b, 0.5f, n);
            if (c != null) {
                outcomes.add(c);
            } else
                misses++;
        }

        outcomes.forEach(System.out::println);
        assertTrue(!outcomes.isEmpty());
    }

    @Test
    void testSequenceIntermpolationInBeliefTable() throws Narsese.NarseseException {


        Term a = $.$("(((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))) &&+11 (bx-->noid))");
        Term b = $.$("(((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&|(happy-->noid)))");
        assertEquals(a.root(), b.root());
        assertEquals(a.concept(), b.concept());


        StringBuilder out = new StringBuilder();
        n.onTask(t -> {
            out.append(t).append('\n');
        });

        Task at = n.believe(a, Tense.Present, 1f);
        n.believe(b, Tense.Present);
        ((BeliefTables) n.concept(a).beliefs()).tableFirst(EternalTable.class).setTaskCapacity(1);
        ((BeliefTables) n.concept(a).beliefs()).tableFirst(TemporalBeliefTable.class).setTaskCapacity(1);
        n.input(at);


        n.run(1);

        /*
        $.50 (((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))) &&+11 (bx-->noid)). 0⋈15 %1.0;.90% {0: 1}
        $.50 (((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&|(happy-->noid))). 0⋈11 %1.0;.90% {0: 2}
          >-- should not be activated: $.50 (((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))) &&+11 (bx-->noid)). 0⋈15 %1.0;.90% {0: 1}
        $.50 (((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))) &&+7 ((--,(by-->noid))&|(happy-->noid))). 0⋈15 %1.0;.95% {0: 1;2}
        $.26 ((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))). 0⋈4 %1.0;.81% {1: 1;;}
        $.31 ((--,(dx-->noid)) &&+15 (bx-->noid)). 0⋈15 %1.0;.81% {1: 1;;}
         */

    }

    static NAR newNAR(int fixedNumBeliefs) {
        //TODO
//
//        ConceptAllocator cb = new ConceptAllocator(fixedNumBeliefs, fixedNumBeliefs, 1);
//        cb.beliefsMaxEte = (fixedNumBeliefs);
//        cb.beliefsMaxTemp = (fixedNumBeliefs);
//        cb.beliefsMinTemp = (fixedNumBeliefs);
//        cb.goalsMaxEte = (fixedNumBeliefs);
//        cb.goalsMaxTemp = (fixedNumBeliefs);
//        cb.goalsMinTemp = (fixedNumBeliefs);

        return new NARS()/*.concepts(new DefaultConceptBuilder(cb))*/.get();

    }

    protected static void permuteChoose(Compound a, Compound b, String expected) {
        assertEquals(expected, permuteIntermpolations(a, b).toString());
    }

    protected static Set<Term> permuteIntermpolations(Compound a, Compound b) {

        {
            float ab = Intermpolate.dtDiff(a, b);
            assertTrue(Float.isFinite(ab));
            assertEquals(ab, Intermpolate.dtDiff(b, a), ScalarValue.EPSILON); //commutative
        }

        NAL<NAL<NAR>> s = NARS.shell();

        Term concept = a.concept();
        assertEquals(concept, b.concept(), "concepts differ: " + a + ' ' + b);


        Set<Term> ss = new TreeSet();

        int n = 10 * (a.volume() + b.volume());
        for (int i = 0; i < n; i++) {
            Term ab = Intermpolate.intermpolate(a, b, s.random().nextFloat(), s);
            ss.add(ab);
        }

        System.out.println(ss);

        return ss;
    }

    @Test
    void testBeliefRevision1() {
        testRevision(1, true);
    }

    @Test
    void testGoalRevision1() {
        testRevision(32, false);
    }

    @Test
    void testBeliefRevision32() {
        testRevision(32, true);
    }

    @Test
    void testGoalRevision32() {
        testRevision(32, false);
    }

    private void testRevision(int delay1, boolean beliefOrGoal) {


        NAR n = newNAR(6);


        BeliefAnalysis b = new BeliefAnalysis(n, x)
                .input(beliefOrGoal, 1f, 0.9f).run(1);

        assertEquals(1, b.size(beliefOrGoal));

        b.input(beliefOrGoal, 0.0f, 0.9f).run(1);

        b.run(delay1);


        b.table(beliefOrGoal).print();
        assertEquals(3, b.size(beliefOrGoal));

        n.run(delay1);

        assertEquals(3, b.size(beliefOrGoal), "no additional revisions");


    }

    @Test
    void testTruthOscillation() {

        NAR n = NARS.shell();


        int offCycles = 2;

        BeliefAnalysis b = new BeliefAnalysis(n, x);


        b.believe(1.0f, 0.9f, Tense.Present);
        b.run(1);


        b.run(1);


        b.believe(0.0f, 0.9f, Tense.Present);
        b.run(1);


        b.run(1);


        b.print();
        assertEquals(2, b.size(true));

        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
                .believe(0.0f, 0.9f, Tense.Present);

        for (int i = 0; i < 16; i++) {


            n.run(1);

        }


    }

    @Test
    void testTruthOscillation2() {


        int maxBeliefs = 16;
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, x);


        int period = 8;
        int loops = 4;
        for (int i = 0; i < loops; i++) {
            b.believe(1.0f, 0.9f, Tense.Present);


            b.run(period);


            b.believe(0.0f, 0.9f, Tense.Present);

            b.run(period);

            b.print();
        }

        b.run(period);

        b.print();


    /*
    Beliefs[@72] 16/16
    <a --> b>. %0.27;0.98% [1, 2, 3, 4, 6] [Revision]
    <a --> b>. %0.38;0.98% [1, 2, 3, 4, 6, 7] [Revision]
    <a --> b>. %0.38;0.98% [1, 2, 3, 4, 5, 6] [Revision]
    <a --> b>. %0.23;0.98% [1, 2, 3, 4, 6, 8] [Revision]
    <a --> b>. %0.35;0.97% [1, 2, 3, 4] [Revision]
    <a --> b>. %0.52;0.95% [1, 2, 3] [Revision]
    <a --> b>. 56+0 %0.00;0.90% [8] [Input]
    <a --> b>. 48+0 %1.00;0.90% [7] [Input]
    <a --> b>. 40+0 %0.00;0.90% [6] [Input]
    <a --> b>. 32+0 %1.00;0.90% [5] [Input]
    <a --> b>. 24+0 %0.00;0.90% [4] [Input]
    <a --> b>. 16+0 %1.00;0.90% [3] [Input]
    <a --> b>. 8+0 %0.00;0.90% [2] [Input]
    <a --> b>. 0+0 %1.00;0.90% [1] [Input]
    <a --> b>. %0.09;0.91% [1, 2] [Revision]
    <a --> b>. 28-20 %0.00;0.18% [1, 2, 3] [((%1, <%1 </> %2>, shift_occurrence_forward(%2, "=/>")), (%2, (<Analogy --> Truth>, <Strong --> Desire>, <ForAllSame --> Order>)))]
     */





    /*for (int i = 0; i < 16; i++) {
        b.printEnergy();
        b.print();
        n.frame(1);
    }*/


    }

    @Test
    void testRevision3Eternals() throws Narsese.NarseseException {
        NAR n = newNAR(6);

        n.input("(a). %1.0;0.5%",
                "(a). %0.0;0.5%",
                "(a). %0.1;0.5%"
        );
        n.run(1);
        Task t = n.conceptualize("(a)").beliefs().match(ETERNAL, ETERNAL, null, 0, n);
        assertEquals(0.37f, t.freq(), 0.02f);
        assertEquals(0.75f, t.conf(), 0.02f);
    }

    @Test
    void testRevision2EternalImpl() throws Narsese.NarseseException {
        NAR n = newNAR(3)
                .input("(x ==> y). %1.0;0.9%",
                        "(x ==> y). %0.0;0.9%");

        n.run(1);

        TaskConcept c = (TaskConcept) n.conceptualize("(x ==> y)");
        c.print();
        Task t = n.answer(c.term(), BELIEF, ETERNAL);
        assertEquals(0.5f, t.freq(), 0.01f);
        assertEquals(0.947f, t.conf(), 0.01f);
    }

    @Test
    void testRevision2TemporalImpl() throws Narsese.NarseseException {
        NAR n = newNAR(3)
                .input("(x ==> y). :|: %1.0;0.9%",
                        "(x ==> y). :|: %0.0;0.9%");

        n.run(1);

        Concept c = n.concept($.$("(x ==> y)"));
        assertEquals(2, c.beliefs().taskCount());

        Task tt = n.belief($.$("(x ==> y)"), 0);
        assertNotNull(tt);
        Truth t = tt.truth();
        assertEquals(0.5f, t.freq(), 0.01f);
        assertEquals(0.947f, t.conf(), 0.01f);
    }

//    /**
//     * test that budget is conserved during a revision between
//     * the input tasks and the result
//     */
//    @Test
//    void testRevisionBudgeting() {
//        NAR n = newNAR(6);
//
//        BeliefAnalysis b = new BeliefAnalysis(n, x);
//
//        assertEquals(0, b.priSum(), 0.01f);
//
//        b.believe(1.0f, 0.5f).run(1);
//
//        Bag<?, TaskLink> tasklinks = b.concept().tasklinks();
//
//        assertEquals(0.5f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.01f);
//
//        System.out.println("--------");
//
//        float linksBeforeRevisionLink = tasklinks.priSum();
//
//        b.believe(0.0f, 0.5f).run(1);
//
//        System.out.println("--------");
//
//        b.run(1);
//        tasklinks.commit();
//
//        System.out.println("--------");
//
//        System.out.println("Beliefs: ");
//        b.print();
//        System.out.println("\tSum Priority: " + b.priSum());
//
//
//        float beliefAfter2;
//        assertEquals(1.0f, beliefAfter2 = b.priSum(), 0.1f /* large delta to allow for forgetting */);
//
//
//        assertEquals(0.71f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.06f);
//
//        b.print();
//
//
//        assertEquals(3, b.size(true));
//
//
//        assertEquals(beliefAfter2, b.priSum(), 0.01f);
//
//
//    }

    @Test
    public void testMergeTruthDilution() {
        //presence or increase of empty space in the union of between merged tasks reduces truth proportionally


        Task a = n.believe(x, 1, 1f);

        assertTrue(TruthIntegration.eviAvg(a, 1, 1, 0) > TruthIntegration.eviAvg(a, 1, 2, 0));
        assertEquals(0, TruthIntegration.eviAvg(a, 2, 2, 0));

        Task a2 = n.believe(x, 1, 1f);
        Task b = n.believe(x, 2, 1f);
        Task c = n.believe(x, 3, 1f);
//        Task d = n.believe(x, 8, 1f);
        Task aa = merge(a, a2, n);
        p(aa);
        assertTrue(aa.conf() > a.conf());
        Task ab = merge(a, b, n);
        p(ab);
        assertTrue(ab.conf() == a.conf());
//        if (Param.REVISION_ALLOW_DILUTE_UNION) {
            Task ac = merge(a, c, n);
            p(ac);
            assertTrue(ac.conf() < ab.conf(), () -> ac + " must have less conf than " + ab);
//        }
//        Task ad = Revision.merge(a, d, n);
//        p(ad);
//        assertTrue(ad.conf() < ac.conf());
    }

    private static void p(Task aa) {
        System.out.println(aa.toString(true));
        System.out.println("\tevi=" + aa.evi());
    }


}
