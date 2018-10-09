package nars.task;

import com.google.common.collect.Lists;
import jcog.pri.bag.Bag;
import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.link.TaskLink;
import nars.table.eternal.EternalTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.polation.FocusingLinearTruthPolation;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;
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
                new FocusingLinearTruthPolation(0, 0, n.dur())
                        .add(Lists.newArrayList(a.apply(n), b.apply(n)))
                        .truth());

    }




    @Test
    void testRevisionInequivalenceDueToTemporalSeparation() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, -4).evidence(1);
        TaskBuilder b = t(0f, 0.5f, 4).evidence(2);

        int dur = 9;
        Truth pt = new FocusingLinearTruthPolation(0, 0, dur).add(Lists.newArrayList(a.apply(n), b.apply(n))).truth();
        @Nullable Truth rt = Revision.revise(a, b);

        assertEquals(pt.freq(), rt.freq(), 0.01f);
        assertTrue(pt.conf() < rt.conf());

    }


    @Test
    void testRevisionEquivalence2Instant() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, 0);
        TaskBuilder b = t(0f, 0.5f, 0);
        assertEquals( Revision.revise(a, b), new FocusingLinearTruthPolation(0, 0, 1).add(Lists.newArrayList(a.apply(n), b.apply(n))).truth());
    }

    @Test
    void testPolation1() throws Narsese.NarseseException {

        int dur = 1;

        Task a = t(1f, 0.9f, 3).apply(n);
        Task b = t(0f, 0.9f, 6).apply(n);
        for (int i = 0; i < 10; i++) {
            System.out.println(i + " " +
                    new FocusingLinearTruthPolation(i, i, dur).add(Lists.newArrayList(a, b)).truth());
        }

        System.out.println();

        Truth ab2 = new FocusingLinearTruthPolation(3, 3, dur).add(Lists.newArrayList(a, b)).truth();
        assertTrue( ab2.conf() >= 0.5f );

        Truth abneg1 = new FocusingLinearTruthPolation(3, 3, dur).add(Lists.newArrayList(a, b)).truth();
        assertTrue( abneg1.freq() > 0.6f );
        assertTrue( abneg1.conf() >= 0.5f );

        Truth ab5 = new FocusingLinearTruthPolation(6, 6, dur).add(Lists.newArrayList(a, b)).truth();
        assertTrue( ab5.freq() < 0.35f );
        assertTrue( ab5.conf() >= 0.5f );
    }

    @Test
    void testRevisionEquivalence4() throws Narsese.NarseseException {
        Task a = t(0f, 0.1f, 3).evidence(1).apply(n);
        Task b = t(0f, 0.1f, 4).evidence(2).apply(n);
        Task c = t(1f, 0.1f, 5).evidence(3).apply(n);
        Task d = t(0f, 0.1f, 6).evidence(4).apply(n);
        Task e = t(0f, 0.1f, 7).evidence(5).apply(n);

        for (int i = 0; i < 15; i++) {
            System.out.println(i + " " + new FocusingLinearTruthPolation(i, i, 1).add(Lists.newArrayList(a, b, c, d, e)).truth());
        }

    }

    private static TaskBuilder t(float freq, float conf, long occ) throws Narsese.NarseseException {
        return new TaskBuilder("a:b", BELIEF, $.t(freq, conf)).time(0, occ);
    }
    static TaskBuilder t(float freq, float conf, long start, long end) throws Narsese.NarseseException {
        return new TaskBuilder("a:b", BELIEF, $.t(freq, conf)).time(0, start, end);
    }

    @Test void testAdjacentTasks() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        {
            Task x = Revision.merge(n, t(1, 0.9f, 0, 3).apply(n),
                    t(1, 0.9f, 3, 5).apply(n));
            assertEquals("(b-->a). 0⋈5 %1.0;.93%", x.toStringWithoutBudget());
        }
        {
            Task x = Revision.merge(n, t(1, 0.9f, 0, 2).apply(n),
                    t(1, 0.9f, 4, 5).apply(n));
            assertEquals("(b-->a). 0⋈5 %1.0;.86%", x.toStringWithoutBudget());
        }
        {
            Task x = Revision.merge(n, t(1, 0.9f, 0, 2).apply(n),
                    t(1, 0.9f, 100, 102).apply(n));
            assertEquals("(b-->a). 0⋈102 %1.0;.30%", x.toStringWithoutBudget());
        }


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
//            Truth a1 = new FocusingLinearTruthPolation(d, d, 1).add(l).truth();
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

            System.out.println(now + " " +  tt);

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
        int maxBeliefs = repeats*4;

        NAR n = newNAR(maxBeliefs);



        long at = 5;

        float outConf = w2c( c2w(inConf)*repeats);

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
        assertTrue( repeats <= b.size(true));

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

        Term a = $.$("(((--,(dx-->noid)) &&+4 ((--,(by-->noid))&|(happy-->noid))) &&+11 (bx-->noid))");
        Term b = $.$("(((bx-->noid) &&+7 (--,(dx-->noid))) &&+4 ((--,(by-->noid))&|(happy-->noid)))");
        Term ar = a.root();
        Term br = b.root();
        assertEquals(ar, br);
        assertEquals(a.concept(), b.concept());

        TreeSet<Term> outcomes = new TreeSet();

        int misses = 0;
        for (int i = 0; i < 10; i++) {
            Term c = Revision.intermpolate(a, b, 0.5f, n);
            if (c!=null) {
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

        n.log();
        StringBuilder out = new StringBuilder();
        n.onTask(t -> {
            out.append(t).append('\n');
        });

        Task at = n.believe(a, Tense.Present, 1f);
        n.believe(b, Tense.Present);
        n.concept(a).beliefs().tableFirst(EternalTable.class).setCapacity(1);
        n.concept(a).beliefs().tableFirst(TemporalBeliefTable.class).setCapacity(1);
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

    private static void printTaskLinks(BeliefAnalysis b) {
        System.out.println("Tasklinks @ " + b.time());
        b.tasklinks().print();
    }

    protected static void permuteChoose(Compound a, Compound b, int dur, String expected) {
        assertEquals(expected, permuteIntermpolations(a, b, dur).toString());
    }

    protected static void permuteChoose(Compound a, Compound b, String expected) {
        permuteChoose(a, b, 1, expected);
    }

    protected static Set<Term> permuteIntermpolations(Term a, Term b) {
        return permuteIntermpolations(a, b, 1);
    }

    private static Set<Term> permuteIntermpolations(Term a, Term b, int dur) {

        NAR s = NARS.shell();
        s.time.dur(dur);

        assertEquals(a.concept(), b.concept(), "concepts differ: " + a + " " + b);



        Set<Term> ss = new TreeSet();

        int n = 10 * (a.volume() + b.volume());
        for (int i = 0; i < n; i++) {
            Term ab = Revision.intermpolate(a, b, s.random().nextFloat(), s);






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
        assertEquals( 3, b.size(beliefOrGoal));

        n.run(delay1);

        assertEquals(3, b.size(beliefOrGoal), "no additional revisions");


    }

    @Test
    void testTruthOscillation() {

        NAR n = NARS.shell();

        n.log();

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
        Task t = n.conceptualize("(a)").beliefs().match(ETERNAL, null, n);
        assertEquals(0.37f, t.freq(), 0.02f);
        assertEquals(0.75f, t.conf(), 0.02f);
    }

    @Test
    void testRevision2EternalImpl() throws Narsese.NarseseException {
        NAR n = newNAR(3)
            .input("(x ==> y). %1.0;0.9%",
                   "(x ==> y). %0.0;0.9%" );

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
                       "(x ==> y). :|: %0.0;0.9%" );

        n.run(1);

        Concept c = n.concept($.$("(x ==> y)"));
        assertEquals(2, c.beliefs().size());

        Task tt = n.belief($.$("(x ==> y)"), 0);
        assertNotNull(tt);
        Truth t = tt.truth();
        assertEquals(0.5f, t.freq(), 0.01f);
        assertEquals(0.947f, t.conf(), 0.01f);
    }

    /** test that budget is conserved during a revision between
     * the input tasks and the result */
    @Test
    void testRevisionBudgeting() {
        NAR n = newNAR(6);

        BeliefAnalysis b = new BeliefAnalysis(n, x);

        assertEquals(0, b.priSum(), 0.01f);

        b.believe(1.0f, 0.5f).run(1);

        Bag<?,TaskLink> tasklinks = b.concept().tasklinks();

        assertEquals(0.5f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.01f);

        printTaskLinks(b);        System.out.println("--------");

        float linksBeforeRevisionLink = tasklinks.priSum();

        b.believe(0.0f, 0.5f).run(1);

        printTaskLinks(b);        System.out.println("--------");

        b.run(1);
        tasklinks.commit();

        printTaskLinks(b);        System.out.println("--------");

        System.out.println("Beliefs: "); b.print();
        System.out.println("\tSum Priority: " + b.priSum());




        float beliefAfter2;
        assertEquals(1.0f, beliefAfter2 = b.priSum(), 0.1f /* large delta to allow for forgetting */);



        assertEquals(0.71f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.06f);

        b.print();


        assertEquals(3, b.size(true));




        assertEquals(beliefAfter2, b.priSum(), 0.01f);









    }

    @Test public void testMergeTruthDilution() {
        //presence or increase of empty space in the union of between merged tasks reduces truth proportionally


        Task a1 = n.believe(x, 1, 1f);
        Task a2 = n.believe(x, 1, 1f);
        Task b = n.believe(x, 2, 1f);
        Task c = n.believe(x, 4, 1f);
        Task d = n.believe(x, 8, 1f);
        Task aa = Revision.merge(n, a1, a2);
        Task ab = Revision.merge(n, a1 ,b);
        Task ac = Revision.merge(n, a1 ,c);
        Task ad = Revision.merge(n, a1 ,d);
        System.out.println(a1.evi());
        p(aa);
        p(ab);
        p(ac);
        p(ad);
        assertTrue(aa.conf() > a1.conf());
        assertTrue(ab.conf() <= a1.conf());
        assertTrue(ac.conf() < ab.conf());
        assertTrue(ad.conf() < ac.conf());
    }

    private static void p(Task aa) {
        System.out.println(aa.toString(true));
        System.out.println("\tevi=" + aa.evi());
    }


}
