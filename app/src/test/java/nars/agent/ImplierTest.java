//package nars.agent;
//
//import nars.NAR;
//import nars.NARS;
//import nars.Narsese;
//import nars.agent.util.Implier;
//import nars.target.Term;
//import org.junit.jupiter.api.Test;
//
//import static nars.$.$;
//import static nars.Op.IMPL;
//
//public class ImplierTest {
//
//    @Test
//    public void testImplier1() throws Narsese.NarseseException {
//
//        NAR n = NARS.tmp(1 /* NAL1 to ensure that any implication derivations are from the Implier not the general NAL6 deriver */);
//
//        Term a = $("a");
//        Term b = $("b");
//        Term c = $("c");
//        Term d = $("d");
//        Term t = $("t");
//        Term u = $("u");
//        Term y = $("y");
//
//        Implier imp = new Implier(n, new float[] { 0f }, y);
//        n.run(1);
//
//        n.log();
//        n.believe(IMPL.the(a, y));
//        n.believe(IMPL.the(b.neg(), y));
//        n.believe(IMPL.the(c, y.neg()));
//        n.believe(IMPL.the(d.neg(), y.neg()));
//        n.believe(IMPL.the(t, -1, y));
//        n.believe(IMPL.the(t.neg(), -2, y));
//        n.believe(IMPL.the(u, +1, y.neg()));
//        n.believe(IMPL.the(u.neg(), +2, y.neg()));
//
//        n.believe(a);
//        n.believe(b);
//        n.believe(c);
//        n.believe(d);
//
//
//
//        n.run(15);
//
//
//
//    }
//}