package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImplierTest {

    @Test
    public void testImplier1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        Term x = $("x");
        Term y = $("y");
        Implier imp = new Implier(n, new float[] { 0f }, y);

        n.log();
        n.input("(x ==> y). :|:");

        System.out.println(imp.impl);

        for (int i = 0; i < 2; i++) {

            n.run(2);
            System.out.println(imp.impl);
            assertEquals(2, imp.impl.nodeCount());
            assertEquals(1, imp.impl.edgeCount());
        }

        n.input("(z ==> x). :|:");
        n.run(1);
        System.out.println(imp.impl);
        n.run(1);
        System.out.println(imp.impl);
        System.out.println(imp.goalTruth);

    }
}