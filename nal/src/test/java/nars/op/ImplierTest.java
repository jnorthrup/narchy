package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.Op.IMPL;

public class ImplierTest {

    @Test
    public void testImplier1() throws Narsese.NarseseException {

        NAR n = NARS.tmp(1 /* NAL1 to ensure that any implication derivations are from the Implier not the general NAL6 deriver */);

        Term a = $("a");
        Term b = $("b");
        Term c = $("c");
        Term d = $("d");
        Term t = $("t");
        Term u = $("u");
        Term y = $("y");

        Implier imp = new Implier(n, new float[] { 0f }, y);
        n.run(1);

        n.log();
        n.believe(IMPL.the(a, y));
        n.believe(IMPL.the(b.neg(), y));
        n.believe(IMPL.the(c, y.neg()));
        n.believe(IMPL.the(d.neg(), y.neg()));
        n.believe(IMPL.the(t, -1, y));
        n.believe(IMPL.the(t.neg(), -2, y));
        n.believe(IMPL.the(u, +1, y.neg()));
        n.believe(IMPL.the(u.neg(), +2, y.neg()));

        n.believe(a);
        n.believe(b);
        n.believe(c);
        n.believe(d);
        //n.believe(t, ..);
        //n.believe(u, ..);

        n.run(15);

        System.out.println(imp.impl);

//        for (int i = 0; i < 2; i++) {
//
//            n.run(2);
//            System.out.println(imp.impl);
//            assertEquals(2, imp.impl.nodeCount());
//            assertEquals(1, imp.impl.edgeCount());
//        }

//        n.input("(z ==> x). :|:");
//        n.run(1);
//        System.out.println(imp.impl);
//        n.run(1);
//        System.out.println(imp.impl);
//        System.out.println(imp.goalTruth);

    }
}