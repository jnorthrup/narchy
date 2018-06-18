package nars.task;

import com.google.common.collect.Lists;
import nars.*;
import nars.term.Term;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.polation.FocusingLinearTruthPolation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static nars.Op.BELIEF;
import static nars.task.RevisionTest.newNAR;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 5/8/16.
 */
class RevectionTest {

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

















    static void print(@NotNull List<Task> l, int start, int end) {
        
        System.out.println("INPUT");
        for (Task t : l) {
            System.out.println(t);
        }

        System.out.println();

        System.out.println("TRUTHPOLATION");
        for (long d = start; d < end; d++) {
            Truth a1 = new FocusingLinearTruthPolation(d, d, 1).add(l).truth();
            System.out.println(d + ": " + a1);
        }
    }


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
        assertEquals(5, b.wave().start());
        assertEquals(11, b.wave().end());

        b.believe(0.5f, 1.0f, 0.99f, 12); 
        

        n.run(3);
        b.print();
        
        assertEquals(4, b.size(true));

        b.print();

        assertEquals(5, b.wave().start());
        assertEquals(12, b.wave().end());



    }

    @Test
    void testSequenceIntermpolation1() throws Narsese.NarseseException {

        
        
        
        
        
        
        
        
        
        
        
        


        n.dtMergeOrChoose.set(false);

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
        n.concept(a).beliefs().setCapacity(1, 1);
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


}