package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.Op.IMPL;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImplicationTest {

    static final Term x = $.the("x");
    static final Term y = $.the("y");
    //static final Term z = $.the("z");
    static final boolean[] B = new boolean[] { true, false };

    @Test public void testBelief() {
        //Z, X==>Y
        StringBuilder o = new StringBuilder();
        for (boolean sp : B) {
            Term z = sp ? x : y;
            for (boolean zz : B) {
                for (boolean xx : B) {
                    for (boolean yy : B) {
                        NAR n = NARS.tmp(6);

                        Term cond = z.negIf(!zz);
                        Term impl = IMPL.the(x.negIf(!xx), y.negIf(!yy));

                        n.believe(impl);
                        n.believe(cond);
                        n.run(16);



                        Term nz = sp ? y : x;
                        @Nullable Truth nzt = n.beliefTruth(nz, ETERNAL);
                        o.append(cond + ". " + impl + ". " + nz + "=" + nzt + "\n");
                    }
                }
            }
        }
        assertEquals(
    "x. (x==>y). y=%1.0;.81%\n" +
            "x. (--,(x==>y)). y=%0.0;.81%\n" +
            "x. ((--,x)==>y). y=null\n" +
            "x. (--,((--,x)==>y)). y=null\n" +
            "(--,x). (x==>y). y=null\n" +
            "(--,x). (--,(x==>y)). y=null\n" +
            "(--,x). ((--,x)==>y). y=%1.0;.81%\n" +
            "(--,x). (--,((--,x)==>y)). y=%0.0;.81%\n" +
            "y. (x==>y). x=%1.0;.45%\n" +
            "y. (--,(x==>y)). x=null\n" +
            "y. ((--,x)==>y). x=%0.0;.45%\n" +
            "y. (--,((--,x)==>y)). x=null\n" +
            "(--,y). (x==>y). x=null\n" +
            "(--,y). (--,(x==>y)). x=%1.0;.45%\n" +
            "(--,y). ((--,x)==>y). x=null\n" +
            "(--,y). (--,((--,x)==>y)). x=%0.0;.45%\n", o.toString());

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
                        n.run(16 );

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
