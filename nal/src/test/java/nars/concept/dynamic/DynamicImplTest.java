package nars.concept.dynamic;

import nars.*;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicImplTest {

    @Test
    void testDynamicImplSubj() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("(x ==> a)", 1f, 0.9f);
        n.believe("(y ==> a)", 1f, 0.9f);
        //            n.believe("a:y", 1f, 0.9f);
        //            n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("(z ==> a)", 0f, 0.9f);
        n.believe("(--z ==> a)", 0.75f, 0.9f);
        //            n.believe("a:(--,z)", 1f, 0.9f);
        //            n.believe("x:b", 1f, 0.9f);
        //            n.believe("y:b", 1f, 0.9f);
        //            n.believe("z:b", 0f, 0.9f);
        n.run(2);
        for (long now: new long[]{0, n.time() /* 2 */, ETERNAL}) {
            Term pp = $$("((x && y) ==> a)");
            Term pn = $$("((x && z) ==> a)");

            assertTrue(n.conceptualize(pp).beliefs() instanceof DynamicTruthBeliefTable);
            assertEquals($.t(1f, 0.81f), n.beliefTruth(pp, now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(pn, now));

            Term Npp = $$("(--(x && y) ==> a)");
            assertEquals(null, n.beliefTruth(Npp, now));

            Term pnn = $$("((x && --z) ==> a)");
            assertEquals($.t(0.75f, 0.81f), n.beliefTruth(pnn, now));


            //                assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
        }

    }

    @Test
    void testDynamicImplSubjTemporalExact() throws Narsese.NarseseException {
        for (int inner : new int[] { 1, 0, 2 /*, -2, -1*/ } ) {
            for (int outer: new int[]{ 1, 0, 2 /*,  -2, -1*/ }) {
                NAR n = NARS.shell();
                int xdt = inner >= 0 ? outer + inner : outer - inner;
                String x = "(x ==>" + dtStr(xdt) + " a)";
                String y = "(y ==>" + dtStr(xdt-inner) + " a)";
                String xy = "((x &&" + dtStr(xdt - outer) + " y) ==>" + dtStr(outer) + " a)";

                System.out.println("inner="  + inner + " outer=" + outer + "\t"
                        + x + " " + y + " " + xy);

                Term pt_p = $$(xy);
                n.believe(x, 1f, 0.9f);
                n.believe(y, 1f, 0.9f);

                Task pt_p_Task = n.match(pt_p, BELIEF, 0);
                assertEquals(pt_p.toString(), pt_p_Task.term().toString());
            }
        }

        //Term p_tp = $$("((x && y) ==>+1 a)");
        //Term pttp = $$("((x &&+1 y) ==>+1 a)");
    }

    private String dtStr(int dt) {
        return (dt >= 0 ? "+" : "") + (dt);
    }

}
