package nars.nal.nal5;

import nars.*;
import nars.concept.Concept;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * NAL5 Boolean / Boolean Satisfiability / Boolean Conditionality
 */
public class BooleanTest {

    @Test
    public void testSAT2Individual00() throws Narsese.NarseseException {
        testSAT2Individual(0, 0);
    }

    @Test
    public void testSAT2Individual01() throws Narsese.NarseseException {
        testSAT2Individual(0, 1);
    }

    @Test
    public void testSAT2Individual10() throws Narsese.NarseseException {
        testSAT2Individual(1, 0);
    }

    @Test
    public void testSAT2Individual11() throws Narsese.NarseseException {
        testSAT2Individual(1, 1);
    }

    void testSAT2Individual(int i, int j) throws Narsese.NarseseException {

        final float confThresh = 0.7f;
        Param.DEBUG = true;

//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < 2; j++) {
        NAR d = NARS.tmp();
        d.freqResolution.set(0.05f);
        d.termVolumeMax.set(24);

        String[] outcomes = {
                "x:{0}", //"(x-->(0,0))",
                "x:{1}", //"(x-->(0,1))",
                "x:{2}", //"(x-->(1,0))",
                "x:{3}"}; //"(x-->(1,1))"};
        //String expected = "(x --> (" + i + "," + j + "))";

        d.believe("( (--(x-->i) && --(x-->j)) ==> " + outcomes[0] + ")");
        d.believe("( (--(x-->i) && (x-->j)) ==> " + outcomes[1] + ")");
        d.believe("( ((x-->i) && --(x-->j)) ==> " + outcomes[2] + ")");
        d.believe("( ((x-->i) && (x-->j)) ==> " + outcomes[3] + ")");

        Term I = $.$("(x-->i)").negIf(i == 0);
        Term J = $.$("(x-->j)").negIf(j == 0);

//                d.believe(I);
//                d.believe(J);
        d.believe($.conj(I, J));

//                for (String s : outcomes) {
//                    d.ask(s);
//                }

        d.run(1024);

        System.out.println(i + " " + j + " ");
        for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
            String s = outcomes[k];
            Concept dc = d.conceptualize(s);
            assertNotNull(dc);
            @Nullable Task t = d.belief(dc.term(), d.time());
            Truth b = t != null ? t.truth() : null;

            System.out.println("\t" + s + "\t" + b + "\t" + outcomes[k]);


            int ex = -1, ey = -1;
            switch (k) {
                case 0:
                    ex = 0;
                    ey = 0;
                    break;
                case 1:
                    ex = 0;
                    ey = 1;
                    break;
                case 2:
                    ex = 1;
                    ey = 0;
                    break;
                case 3:
                    ex = 1;
                    ey = 1;
                    break;
            }
            boolean thisone = ((ex == i) && (ey == j));
            if (thisone && b == null)
                fail("unrecognized true case");


            if (thisone && b.isNegative() && b.conf() > confThresh)
                fail("wrong true case:\n" + t.proof());

            if (!thisone && b != null && b.isPositive() && b.conf() > confThresh)
                fail("wrong false case:\n" + t.proof());

        }

//                System.out.println();

        //return;
//            }
//        }
    }

    @Test
    public void testEternalcept() throws Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = NARS.tmp().log();
        n.believe("((&&,(0,x),(1,x),(2,x),(3,x))==>a)");
        n.believe("((&&,(0,y),(1,y),(2,y),(3,y))==>b)");
        n.believe("((&&,(0,x),(1,x),(2,x),(3,y))==>c)");
        n.question("(a ==> c)");
        n.question("(b ==> c)");
        n.run(200);
    }

    @Test
    public void testConditionalImplication() {
        boolean[] booleans = {true, false};
        Term x = $.the("x");
        Term y = $.the("y");
        Term[] concepts = {x, y};

        for (boolean goalSubjPred : booleans) {


            for (boolean subjPolarity : booleans) {
                for (boolean predPolarity : booleans) {
                    for (boolean goalPolarity : booleans) {

                        Term goal = (goalSubjPred ? x : y).negIf(!goalPolarity);
                        Term condition = $.impl(x.negIf(!subjPolarity), y.negIf(!predPolarity));

                        NAR n = NARS.tmp();
                        n.goal(goal);
                        n.believe(condition);
                        n.run(128);

                        System.out.println(goal + "!   " + condition + ".");
                        for (Term t : concepts) {
                            if (!t.equals(goal.unneg()))
                                System.out.println("\t " + t + "! == " + n.goalTruth(t, ETERNAL));
                        }
                        System.out.println();

                    }
                }

            }

        }
    }

//    @Test
//    public void testSAT2Combined() throws Narsese.NarseseException {
//
//        final float confThresh = 0.7f;
//        Param.DEBUG = true;
//
//        NAR d = new NARS().get();
//        d.nal(6);
//        d.termVolumeMax.setValue(32);
//
//        String[] outcomes = {
//                "(x-->(0,0))",
//                "(x-->(0,1))",
//                "(x-->(1,0))",
//                "(x-->(1,1))"};
//
//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < 2; j++) {
//
//                //String expected = "(x --> (" + i + "," + j + "))";
//
//                d.believe("( (--(x-->i) && --(x-->j)) ==> " + outcomes[0] + ")");
//                d.believe("( (--(x-->i) && (x-->j)) ==> " + outcomes[1] + ")");
//                d.believe("( ((x-->i) && --(x-->j)) ==> " + outcomes[2] + ")");
//                d.believe("( ((x-->i) && (x-->j)) ==> " + outcomes[3] + ")");
//
//                Compound I = $.negIf($.$("(x-->i)"), i == 0);
//                Compound J = $.negIf($.$("(x-->j)"), j == 0);
//
////                d.believe(I);
////                d.believe(J);
//                d.believe($.conj(I, J));
//
////                for (String s : outcomes) {
////                    d.ask(s);
////                }
//
//
//                //return;
//            }
//        }
//
//        d.run(1024);
//
//        validate(confThresh, d, outcomes);
//
//        System.out.println();
//
//    }

//    public static void validate(float confThresh, NAR d, String[] outcomes) throws Narsese.NarseseException {
//        for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
//            String s = outcomes[k];
//            Concept dc = d.conceptualize(s);
//            assertNotNull(dc);
//            @Nullable Task t = d.belief(dc.term(), d.time());
//            Truth b = t != null ? t.truth() : null;
//
//            System.out.println("\t" + s + "\t" + b);
//
//
//            int ex = -1, ey = -1;
//            switch (k) {
//                case 0:
//                    ex = 0;
//                    ey = 0;
//                    break;
//                case 1:
//                    ex = 0;
//                    ey = 1;
//                    break;
//                case 2:
//                    ex = 1;
//                    ey = 0;
//                    break;
//                case 3:
//                    ex = 1;
//                    ey = 1;
//                    break;
//            }
//            boolean thisone = true; //((ex == i) && (ey == j));
//            if (thisone && b == null)
//                assertTrue("unrecognized true case", false);
//
//
//            if (thisone && b != null && b.isNegative() && b.conf() > 0)
//                assertTrue("wrong true case:\n" + t.proof(), false);
//
//            if (!thisone && b != null && b.isPositive() && b.conf() > confThresh)
//                assertTrue("wrong false case:\n" + t.proof(), false);
//
//        }
//    }


}
