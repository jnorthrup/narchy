package nars.agent.util;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.impiler.Impiler;
import nars.impiler.ImpilerDeduction;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImpilerTest {
    @Test
    public void testEternal1() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(b ==> c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, $$("a"), true, 0).toString());
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, $$("c"), false, 0).toString());

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
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, $$("a"), true, 0).toString());
    }

    @Test
    public void testEternalInnerNegation_must_match_fwd_inverse() throws Narsese.NarseseException {
        //test both matching and non-matching case, make sure only matching is invovled
        NAR n = NARS.threadSafe();
        n.input("(a ==> --b).");
        n.input("(        b ==> c).");  //unlikely
        n.input("(      --b ==> d).");  //likely
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 (((--,b)&&a)==>d). 0 %1.0;.81%]", deduce(d, $$("a"), true, 0).toString());
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
        assertEquals("[$.45 ((a&&b)==>c). 0 %1.0;.81%]", deduce(d, $$("c"), false, 0).toString());
    }

    @Test
    public void testEternalOuterNegation_FwdStart() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(--a ==> b).");
        n.input("(b ==> c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 (((--,a)&&b)==>c). 0 %1.0;.81%]", deduce(d, $$("a"), true, 0).toString());
    }
    @Test
    public void testEternalOuterNegation_FwdEnd() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.input("(a ==> b).");
        n.input("(b ==> --c).");
        Impiler.impile(n);
        ImpilerDeduction d = new ImpilerDeduction(n);
        assertEquals("[$.45 ((a&&b)==>c). 0 %0.0;.81%]", deduce(d, $$("a"), true, 0).toString());
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

        assertEquals(2, deduce(d, $$("a"), true, 0).size());

		assertEquals(2, deduce(d, $$("d"), false, 0).size());

    }

    private static List<Task> deduce(ImpilerDeduction d, Term x, boolean forward, long at) {
        System.out.println((forward ? "forward: " : "reverse: " ) + x);
        List<Task> t = d.get(x, at, forward);
        t.forEach(System.out::println);
        return t;
    }

    @Test
    public void testDeductionChainPositive() throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);

        final int[] edges = {0};
        Impiler.ImplGrapher t = new Impiler.ImplGrapher(n) {
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