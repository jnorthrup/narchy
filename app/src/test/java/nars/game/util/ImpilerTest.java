package nars.game.util;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.impiler.Impiler;
import nars.impiler.ImpilerDeduction;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImpilerTest {
    @Test
    public void testEternal1() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(b ==> c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, INSTANCE.$$("a"), true, 0).toString());
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, INSTANCE.$$("c"), false, 0).toString());

    }

    @Test
    public void testEternalInnerNegation_must_match_fwd1() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(      b ==> c)."); //likely
        n.input("(    --b ==> d)."); //unlikely
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, INSTANCE.$$("a"), true, 0).toString());
    }
    @Test
    public void testEternalInnerNegation_semi_match_fwd1() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(      b ==> c). %1.00;0.90%"); //likely
        n.input("(      b ==> d). %0.25;0.90%"); //unlikely
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%, $.34 ((a&&b)==>d). 0 %.25;.61%]", deduce(d, INSTANCE.$$("a"), true, 0).toString());
    }
    @Test
    public void testEternalInnerNegation_must_match_fwd_inverse() throws Narsese.NarseseException {
        //TODO
        NAR n = NARS.threadSafe();
        n.input("(a ==> --b).");
        n.input("(        b ==> c).");  //unlikely
        n.input("(      --b ==> d).");  //likely
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 (((--,b)&&a)==>d). 0 %1.0;.81%]",
            deduce(d, INSTANCE.$$("a"), true, 0).toString());
    }

    @Test
    public void testEternalInnerNegation_must_match_rev() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==>   b)."); //likely
        n.input("(x ==> --b)."); //unlikely
        n.input("(        b ==> c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, INSTANCE.$$("c"), false, 0).toString());
    }

    @Test
    public void testEternalOuterNegation_FwdStart() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(--a ==> b).");
        n.input("(b ==> c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 (((--,a)&&b)==>c). 0 %1.0;.81%]", deduce(d, INSTANCE.$$("a"), true, 0).toString());
    }
    @Test
    public void testEternalOuterNegation_FwdEnd() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(b ==> --c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %0.0;.81%]", deduce(d, INSTANCE.$$("a"), true, 0).toString());
    }

    @Test
    public void testEternal2() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();

        n.input("(a ==> b).");
        n.input("(--a ==> c). %0.9;0.5%");
        n.input("((c&&d) ==> e). %1.0;0.9%");

        n.input("(b ==> c). %0.9;0.5%");
        n.input("(c ==> d). %0.9;0.5%");
        n.input("(d ==> e). %0.9;0.5%");
        n.input("(e ==> f). %0.9;0.5%");


        Impiler.impile(n);

        Impiler.graphGML(n.concepts()::iterator, System.out);

        ImpilerDeduction d = new ImpilerDeduction(n);

        assertEquals(2, deduce(d, INSTANCE.$$("a"), true, 0).size());

		assertEquals(2, deduce(d, INSTANCE.$$("d"), false, 0).size());

    }

    private static List<Task> deduce(ImpilerDeduction d, Term x, boolean forward, long at) {
        System.out.println((forward ? "forward: " : "reverse: " ) + x);
        List<Task> t = d.get(x, at, forward);
        for (Task task : t) {
            System.out.println(task);
        }
        return t;
    }

    @Test
    public void testDeductionChainPositive() throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);

        int[] edges = {0};
        Impiler.ImplGrapher t = new Impiler.ImplGrapher() {
//            @Override
//            protected float leak(Task next) {
//                //System.out.println(this + " leak " + next);
//                return super.leak(next);
//            }
//
//            @Override
//            protected void edge( e, Task t, Concept sc, Concept pc) {
//                //System.out.println(this + " edge " + e);
//                super.edge(e, t, sc, pc);
//                edges[0]++;
//            }
//
        };


//        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(n) {
//
//        };

//        n.synch();

        n.log();
//        n.run(16);


        n.input("(a ==> b). ");
        n.run(1);
        n.input("(b ==> c). ");
        n.run(1);
        n.input("(c ==> d). ");
        n.run(1);
        n.input("(d ==> e). ");
        n.run(1);


        n.input("a@");
        n.run(1);

//        assertTrue(4 <= edges[0]);


        //d.graphGML(System.out)

    }

}