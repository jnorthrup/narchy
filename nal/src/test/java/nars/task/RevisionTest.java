package nars.task;

import jcog.bag.Bag;
import nars.*;
import nars.concept.TaskConcept;
import nars.concept.state.DefaultConceptState;
import nars.term.Compound;
import nars.term.Term;
import nars.test.analyze.BeliefAnalysis;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static nars.Op.ATOM;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/18/16.
 */
public class RevisionTest {

    static NAR newNAR(int fixedNumBeliefs) {
        //this.activeTasks = activeTasks;
        NAR d = NARS.tmp();

        /*
        @Override
        public BeliefTable.RankBuilder getConceptRanking() {
            if (rb == null)
                return super.getConceptRanking();
            else
                return rb;
        }
        */

        //}
        DefaultConceptState cb = (DefaultConceptState) d.terms.conceptBuilder.awake();
        cb.beliefsMaxEte = (fixedNumBeliefs);
        cb.beliefsMaxTemp = (fixedNumBeliefs);
        cb.beliefsMinTemp = (fixedNumBeliefs);
        cb.goalsMaxEte = (fixedNumBeliefs);
        cb.goalsMaxTemp = (fixedNumBeliefs);
        cb.goalsMinTemp = (fixedNumBeliefs);
        return d;
    }

    @Test
    public void testBeliefRevision1() {
        testRevision(1, true); //short term immediate test for correct revisionb ehavior
    }

    @Test
    public void testGoalRevision1() {
        testRevision(32, false); //longer term test
    }

    @Test
    public void testBeliefRevision32() {
        testRevision(32, true); //longer term test
    }

    @Test
    public void testGoalRevision32() {
        testRevision(32, false); //longer term test
    }


    public final static Term x = $.the("x");

    void testRevision(int delay1, boolean beliefOrGoal) {
        Param.DEBUG = true;

        NAR n = newNAR(6);

        //arbitrary time delays in which to observe that certain behavior does not happen

        BeliefAnalysis b = new BeliefAnalysis(n, x)
            .input(beliefOrGoal, 1f, 0.9f).run(1);

        assertEquals(1, b.size(beliefOrGoal));

        b.input(beliefOrGoal, 0.0f, 0.9f).run(1);

        b.run(delay1);

        //b.print(beliefOrGoal);

        b.table(beliefOrGoal).print();
        assertEquals( 3, b.size(beliefOrGoal));

        n.run(delay1);

        assertEquals(3, b.size(beliefOrGoal), "no additional revisions");


    }

    @Test
    public void testTruthOscillation() {

        NAR n = newNAR(8);


        Param.DEBUG = true;
        n.log();

        int offCycles = 2;

        BeliefAnalysis b = new BeliefAnalysis(n, x);

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        b.believe(1.0f, 0.9f, Tense.Present);
        b.run(1);
        //b.printEnergy();

        b.run(1);
        //b.printEnergy();

        b.believe(0.0f, 0.9f, Tense.Present);
        b.run(1);
        //b.printEnergy();

        b.run(1);
        //b.printEnergy();

        b.print();
        assertEquals(2, b.size(true));

        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
                .believe(0.0f, 0.9f, Tense.Present);

        for (int i = 0; i < 16; i++) {
            //b.printEnergy();
            //b.print();
            n.run(1);
            //TODO test that they are sorted ?
        }


    }


    @Test
    public void testTruthOscillation2() {

        Param.DEBUG = true;

        int maxBeliefs = 16;
        NAR n = newNAR(maxBeliefs);



        BeliefAnalysis b = new BeliefAnalysis(n, x);

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        int period = 8;
        int loops = 4;
        for (int i = 0; i < loops; i++) {
            b.believe(1.0f, 0.9f, Tense.Present);


            b.run(period);
            //b.printEnergy();

            b.believe(0.0f, 0.9f, Tense.Present);

            b.run(period);
            //b.printEnergy();
            b.print();
        }

        b.run(period);

        b.print();

        //TODO test the belief table for something like the following:
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


//        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
//                .believe(0.0f, 0.9f, Tense.Present);

    /*for (int i = 0; i < 16; i++) {
        b.printEnergy();
        b.print();
        n.frame(1);
    }*/


    }

    @Test public void testRevision3Eternals() throws Narsese.NarseseException {
        NAR n = newNAR(6);
        //n.log();
        n.input("(a). %1.0;0.5%",
                "(a). %0.0;0.5%",
                "(a). %0.1;0.5%"
                );
        n.run(1);
        Task t = n.conceptualize("(a)").beliefs().match(ETERNAL, null, n);
        assertEquals(0.37f, t.freq(), 0.02f);
        assertEquals(0.75f, t.conf(), 0.02f);
    }

    @Test public void testRevision2EternalImpl() throws Narsese.NarseseException {
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


    @Test public void testRevision2TemporalImpl() throws Narsese.NarseseException {
        NAR n = newNAR(3)
                .input("(x ==> y). :|: %1.0;0.9%",
                       "(x ==> y). :|: %0.0;0.9%" );

        n.run(1);
        Truth t = n.beliefTruth("(x ==> y)", 0);
        assertEquals(0.5f, t.freq(), 0.01f);
        assertEquals(0.947f, t.conf(), 0.01f);
    }

    /** test that budget is conserved during a revision between
     * the input tasks and the result */
    @Test public void testRevisionBudgeting() {
        NAR n = newNAR(6);

        BeliefAnalysis b = new BeliefAnalysis(n, x);

        assertEquals(0, b.priSum(), 0.01f);

        b.believe(1.0f, 0.5f).run(1);

        Bag<Task,?> tasklinks = b.concept().tasklinks();

        assertEquals(0.5f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.01f);

        printTaskLinks(b);        System.out.println("--------");

        float linksBeforeRevisionLink = tasklinks.priSum();

        b.believe(0.0f, 0.5f).run(1);
        assertEquals(2, tasklinks.size());
        printTaskLinks(b);        System.out.println("--------");

        b.run(1); //allow enough time for tasklinks bag to commit
        tasklinks.commit();

        printTaskLinks(b);        System.out.println("--------");

        System.out.println("Beliefs: "); b.print();
        System.out.println("\tSum Priority: " + b.priSum());




        float beliefAfter2;
        assertEquals(1.0f, beliefAfter2 = b.priSum(), 0.1f /* large delta to allow for forgetting */);

        //assertEquals(linksBeforeRevisionLink, tasklinks.priSum(), 0.01f);

        assertEquals(0.71f, b.beliefs().match(ETERNAL, null, n).truth().conf(), 0.06f); //the revised task on top

        b.print();

        //revised:
        assertEquals(3, b.size(true));

        //3 if a non-revised eternal task (which creates a revision) revised eternal task is also input/processed. 2 if it is not
        //assertEquals(3, tasklinks.size());

        assertEquals(beliefAfter2, b.priSum(), 0.01f); //CONSERVED BELIEF BUDGET

        //tasklinks.commit();
        //tasklinks.print();

        //without tasklink balancing: 1.24 - 0.97
        //with balancing: 1.10 - 0.97

        //assertEquals( linksBeforeRevisionLink, tasklinks.priSum(), 0.1f); //CONSERVED LINK BUDGET

    }

    static void printTaskLinks(BeliefAnalysis b) {
        System.out.println("Tasklinks @ " + b.time());
        b.tasklinks().print();
    }

    @Test public void testIntermpolation0() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+3 (b &&+3 c))");
        Compound b = $.$("(a &&+3 (b &&+1 c))");
        assertEquals(
                "[((a &&+3 b) &&+1 c), ((a &&+3 b) &&+3 c)]",
                permuteChooses(a, b).toString());
    }

    @Test public void testIntermpolation0b() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+3 (b &&+3 c))");
        Compound b = $.$("(a &&+1 (b &&+1 c))");
        assertEquals("[((a &&+1 b) &&+1 c), ((a &&+1 b) &&+3 c), ((a &&+3 b) &&+1 c), ((a &&+3 b) &&+3 c)]",
                permuteChooses(a, b).toString());
    }

    @Test public void testIntermpolation0invalid() throws Narsese.NarseseException {
        Compound a = $.$("(a &&+3 (b &&+3 c))");
        Compound b = $.$("(a &&+1 b)");
        try {
            Set<Term> p = permuteChooses(a, b);
            fail("");
        } catch (Error  e) {
            assertTrue(true);
        }
    }


    @Test public void testIntermpolation2() throws Narsese.NarseseException {
        Compound f = $.$("(a &&+1 b)");
        Compound g = $.$("(a &&-1 b)");
        assertEquals("[(b &&+1 a), (a &&+1 b)]", permuteChooses(f, g).toString());

        Compound h = $.$("(a &&+1 b)");
        Compound i = $.$("(a &| b)");
        assertEquals("[(a&|b), (a &&+1 b)]", permuteChooses(h, i).toString());
    }

    @Test public void testIntermpolationInner() throws Narsese.NarseseException {
        Compound f = $.$("(x --> (a &&+1 b))");
        Compound g = $.$("(x --> (a &| b))");
        assertEquals("[(x-->(a&|b)), (x-->(a &&+1 b))]",
                permuteChooses(f, g).toString());
    }

    static Set<Term> permuteChooses(Term a, Term b) {

        NAR s = NARS.shell();
        s.dtMergeOrChoose.set(false);

        assertEquals(a.conceptual(), b.conceptual());

        @NotNull Set<Term> ut = ((Compound) a).recurseTermsToSet(ATOM);

        Set<Term> ss = new TreeSet();

        int n = 8 * (a.volume() + b.volume());
        for (int i = 0; i < n; i++) {
            Term ab = Revision.intermpolate(a, b, 0.5f, s);

            //assertTrue(ab.conceptual().equals(a.conceptual()));


//            assertTrue(ab + " not valid interpolation of: " + a + " x " + b,
//                    validIntermpolation(ut, ab));
            ss.add(ab);
        }

        System.out.println(ss);

        return ss;
    }

//    private boolean validIntermpolation(@NotNull Set<Term> ut, Term ab) {
//        return ut.equals( ((Compound)ab).recurseTermsToSet(ATOM) );
//    }

}
