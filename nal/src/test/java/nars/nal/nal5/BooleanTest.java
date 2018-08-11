package nars.nal.nal5;

import nars.*;
import nars.concept.Concept;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.Op.CONJ;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * NAL5 Boolean / Boolean Satisfiability / Boolean Conditionality
 */
class BooleanTest {

    @Test
    void testSAT2Individual00() throws Narsese.NarseseException {
        testSAT2Individual(0, 0);
    }

    @Test
    void testSAT2Individual01() throws Narsese.NarseseException {
        testSAT2Individual(0, 1);
    }

    @Test
    void testSAT2Individual10() throws Narsese.NarseseException {
        testSAT2Individual(1, 0);
    }

    @Test
    void testSAT2Individual11() throws Narsese.NarseseException {
        testSAT2Individual(1, 1);
    }

    private void testSAT2Individual(int i, int j) throws Narsese.NarseseException {

        final float confThresh = 0.7f;




        NAR d = NARS.tmp();
        d.freqResolution.set(0.02f);
        d.termVolumeMax.set(16);




        String[] outcomes = {
                "a", 
                "b", 
                "c", 
                "d"}; 
        

        d.believe("( (--i && --j) ==> " + outcomes[0] + ")");
        d.believe("( (--i && j) ==> " + outcomes[1] + ")");
        d.believe("( (i && --j) ==> " + outcomes[2] + ")");
        d.believe("( (i && j) ==> " + outcomes[3] + ")");

        Term I = $.$("i").negIf(i == 0);
        Term J = $.$("j").negIf(j == 0);



        d.believe(CONJ.the(I, J));





        d.run(768);

        System.out.println(i + " " + j + " ");
        for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
            String s = outcomes[k];
            Concept dc = d.conceptualize(s);
            assertNotNull(dc);
            @Nullable Task t = d.belief(dc, d.time());
            Truth b = t != null ? t.truth() : null;

            System.out.println("\t" + dc.term() + "\t" + s + "\t" + b + "\t" + outcomes[k]);


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
            boolean positive = ((ex == i) && (ey == j));
            if (positive != (b != null)) {
                if (positive)
                    fail("unrecognized true case");
                else if (b.conf() > confThresh)
                    fail("invalid false impl subj deriving a pred");
            }


            if (positive && b.isNegative() && b.conf() > confThresh)
                fail("wrong true case:\n" + t.proof());

            if (!positive && b != null && b.isPositive() && b.conf() > confThresh)
                fail("wrong false case:\n" + t.proof());

        }



        


    }

    @Test
    void testEternalcept() throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.believe("((&&,(0,x),(1,x),(2,x),(3,x))==>a)");
        n.believe("((&&,(0,y),(1,y),(2,y),(3,y))==>b)");
        n.believe("((&&,(0,x),(1,x),(2,x),(3,y))==>c)");
        n.question("(a ==> c)");
        n.question("(b ==> c)");
        n.run(200);
    }

    @Test
    void testConditionalImplication() {
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
                        n.want(goal);
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
































































































}
