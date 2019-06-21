package nars.agent.util;

import jcog.data.graph.GraphIO;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

class ImpilerTest {

    @Test
    public void test1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        //n.log();

        Impiler.init(n);

        n.input("(a ==> b).");
        n.input("(--a ==> c). %0.9;0.5%");
        n.input("((c&&d) ==> e). %1.0;0.9%");
        n.input("a.");

        n.run(10);
        n.input("(b ==> c). %0.9;0.5%");
        n.input("(c ==> d). %0.9;0.5%");
        n.input("(d ==> e). %0.9;0.5%");
        n.input("(e ==> f). %0.9;0.5%");
        n.run(10);

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


        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(n) {

        };

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

        GraphIO.writeGML(d.graph(), System.out);

    }

    @Test
    public void testDeductionChainPositiveNegative() throws Narsese.NarseseException {

    }
}