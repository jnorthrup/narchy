package nars.agent.util;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

class ImpilerTest {

    @Test
    public void test1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.input("(a ==> b).");
        n.input("(--a ==> c). %0.9;0.5%");
        n.input("((c&&d) ==> e). %1.0;0.9%");

        Impiler.Impiled i = Impiler.of(true, n.concepts, n);
        i.print();
    }

}