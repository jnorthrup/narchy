package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static jcog.Texts.n2;
import static nars.Op.BELIEF;
import static nars.Op.IMPL;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** exhaustive parametric implication deduction/induction belief/goal tests */
public class ImplicationTest {

    static final Term x = $.the("x");
    static final Term y = $.the("y");
    static final boolean[] B = new boolean[] { true, false };

    @Test public void testBelief() {
        //Z, X==>Y
        StringBuilder o = new StringBuilder();
        for (float condFreq : new float[] { 0, 1, 0.5f }) {
            for (boolean sp : B) {
                Term z = sp ? x : y;
                for (boolean xx : B) {
                    for (boolean yy : B) {
                        NAR n = NARS.tmp(6);

                        Term impl = IMPL.the(x.negIf(!xx), y.negIf(!yy));

                        n.believe(impl);
                        n.believe(z, condFreq, n.confDefault(BELIEF));
                        n.run(256);



                        Term nz = sp ? y : x;

//                        BeliefTable nzb = n.concept(nz).beliefs();
//                        int bs = nzb.size();
//                        if (bs == 2) {
//                            nzb.print();
//                            System.out.println();
//                        }
                        //assert(bs == 0 || bs == 1 || bs == 3); //either one answer, or something revised to 0.5 via 2

                        @Nullable Truth nzt = n.beliefTruth(nz, ETERNAL);

                        o.append(z + ". %" + n2(condFreq) + "% " + impl + ". " + nz + "=" + nzt + "\n");
                    }
                }
            }
        }

        String oo = o.toString();
        System.out.println(oo);

        assertContains(oo, "x. %0.0% ((--,x)==>y). y=%1.0;.81%");
        assertContains(oo, "y. %0.0% ((--,x)==>y). x=%1.0;.45%");
        assertContains(oo, "y. %0.0% (--,((--,x)==>y)). x=%0.0;.82%");
        assertContains(oo, "y. %0.0% (--,((--,x)==>y)). x=%0.0;.82%");
        //...

    }



    @Test public void testGoal() {
        //Z, X==>Y
        StringBuilder o = new StringBuilder();
        for (boolean sp : B) {
            Term z = sp ? x : y;
            for (boolean zz : B) {
                for (boolean xx : B) {
                    for (boolean yy : B) {
                        NAR n = NARS.tmp();

                        Term cond = z.negIf(!zz);
                        Term impl = IMPL.the(x.negIf(!xx), y.negIf(!yy));

                        n.believe(impl);
                        n.goal(cond);
                        n.run(64 );

                        Term nz = sp ? y : x;
                        @Nullable Truth nzt = n.goalTruth(nz, ETERNAL);
                        o.append(cond + "! " + impl + ". " + nz + "=" + nzt + "\n");
                    }
                }
            }
        }

        String oo = o.toString();

        System.out.println(oo);

        //strong
        assertContains(oo, "y! (x==>y). x=%1.0;.81%");
        assertContains(oo, "y! ((--,x)==>y). x=%0.0;.81%");
        assertContains(oo, "(--,y)! (--,(x==>y)). x=%1.0;.81%");
        assertContains(oo, "(--,y)! (--,((--,x)==>y)). x=%0.0;.81%");

        //weak
        assertContains(oo, "x! (x==>y). y=%1.0;.45%");
        assertContains(oo, "x! (--,(x==>y)). y=%0.0;.45%");
        //...


    }

    public void assertContains(String oo, String c) {
        assertTrue(oo.contains(c));
    }

}
