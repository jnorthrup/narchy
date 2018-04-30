package nars.op;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.op.stm.ConjClustering;
import nars.util.time.Tense;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConjClusteringTest {

    @Test
    public void testSameTruthSameTime() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        int ccap = 4;
        ConjClustering c = new ConjClustering(n, BELIEF, (t) -> t.isInput(), 4, ccap);

        for (int i = 0; i < ccap; i++)
            n.believe($.the("x" + i), Tense.Present);
        n.run(1);
        assertEquals(1, n.concept($.$("(&&,x0,x1,x2,x3)")).beliefs().size());
    }
    @Test
    public void testNeg() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        int ccap = 4;
        ConjClustering c = new ConjClustering(n, BELIEF, (t) -> t.isInput(), 4, ccap);

        for (int i = 0; i < ccap; i++)
            n.believe($.the("x" + i).neg(), Tense.Present);
        n.run(1);
        assertEquals(1, n.concept($.$("(&&,(--,x0),(--,x1),(--,x2),(--,x3))")).beliefs().size());
    }

    //TODO differing truth
    //etc
}