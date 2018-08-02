package nars.agent.util;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

class ImpilerTest {

    @Test
    public void test1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.log();

        Impiler.ImpilerTracker t = new Impiler.ImpilerTracker(32, 2, n);
        Impiler.ImpilerSolver s = new Impiler.ImpilerSolver(32, 2, n);
        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(32, 2, n);

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

        Impiler.ImpilerTracker t = new Impiler.ImpilerTracker(8, 2, n);
        //Impiler.ImpilerSolver s = new Impiler.ImpilerSolver(8, 1, n);
        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction(8, 2, n);

        n.synch();

        n.log();
        n.run(4);


        n.input("(a ==> b). "); n.run(1);
        n.input("(b ==> c). "); n.run(1);
        n.input("(c ==> d). "); n.run(1);
        n.input("(d ==> e). "); n.run(1);
//        n.input("(d ==> e). ");
//        n.input("(e ==> f). ");

        n.input("a@"); n.run(1);
        n.input("a@"); n.run(1);

        n.run(10);

    }

}