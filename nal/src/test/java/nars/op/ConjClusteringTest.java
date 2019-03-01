package nars.op;

import nars.*;
import nars.op.stm.ConjClustering;
import nars.table.BeliefTable;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConjClusteringTest {

    @Test
    void testSameTruthSameTime() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        int ccap = 4;
        ConjClustering c = new ConjClustering(n, BELIEF, Task::isInput, 4, ccap);


        for (int i = 0; i < ccap; i++)
            n.believe($.the("x" + i), Tense.Present);
        n.run(1);

        BeliefTable b = n.concept($.$("(&&,x0,x1,x2,x3)")).beliefs();
        assertEquals(1, b.size());

        assert1234(n, b);
    }

    @Test
    void testNeg() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        int ccap = 4;
        ConjClustering c = new ConjClustering(n, BELIEF, Task::isInput, 4, ccap);

        for (int i = 0; i < ccap; i++)
            n.believe($.the("x" + i).neg(), Tense.Present);
        n.run(1);
        BeliefTable b = n.concept($.$("(&&,(--,x0),(--,x1),(--,x2),(--,x3))")).beliefs();
        assertEquals(1, b.size());

        assert1234(n, b);
    }

    private static void assert1234(NAR n, BeliefTable b) {
        Task the = b.streamTasks().findFirst().get();
        float p = the.pri();
        assertTrue(p==p);
        assertEquals("[1, 2, 3, 4]", Arrays.toString(the.stamp()));
        //assertTrue(p < n.priDefault(BELIEF)); //pri less than its components
    }

    @Test void testDimensionalDistance1() {
        NAR n = NARS.shell();
        n.time.dur(4);

        int ccap = 8;
        ConjClustering c = new ConjClustering(n, BELIEF, (t) -> t.isInput(), 2, ccap);

        n.inputAt(1, "$1.0 x. |");
        n.inputAt(2, "$1.0 y. |");
        n.inputAt(1, "$0.1 z. |");
        n.inputAt(3, "$0.1 w. |");


        n.run(4);
        //TODO
//        assertEquals(1, n.concept($.$("")).beliefs().size());

    }
    
    
}