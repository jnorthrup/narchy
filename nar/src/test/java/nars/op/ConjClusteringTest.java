package nars.op;

import nars.*;
import nars.derive.Deriver;
import nars.derive.rule.PremiseRuleSet;
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
        int ccap = 3;
        ConjClustering c = new ConjClustering(n, BELIEF, BELIEF, 4, ccap, Task::isInput);
        new Deriver(new PremiseRuleSet(n).add(c));
        n.run(1); //HACK


        for (int i = 0; i < ccap; i++)
            n.believe($.the("x" + i), Tense.Present);
        n.run(64);

        BeliefTable b = n.concept($.$("(&&,x0,x1,x2)")).beliefs();
        assertEquals(1, b.taskCount());

        assert123(b);
    }

    @Test
    void testNeg() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        int ccap = 3;
        ConjClustering c = new ConjClustering(n, BELIEF, BELIEF, 4, ccap, Task::isInput);
        new Deriver(new PremiseRuleSet(n).add(c));
        n.log();
        n.run(1); //HACK

        for (int i = 0; i < ccap; i++)
            n.believe($.the("x" + i).neg(), Tense.Present);
        n.run(64);

        BeliefTable b = n.concept($.$("(&&,--x0,--x1,--x2)")).beliefs();
        assertEquals(1, b.taskCount());

        assert123(b);
    }

    private static void assert123(BeliefTable b) {
        Task the = b.taskStream().findFirst().get();
        float p = the.pri();
        assertTrue(p==p);
        assertEquals("[1, 2, 3]", Arrays.toString(the.stamp()));
        //assertTrue(p < n.priDefault(BELIEF)); //pri less than its components
    }

    @Test void DimensionalDistance1() {
        NAR n = NARS.shell();
        n.time.dur(4);

        int ccap = 8;
        ConjClustering c = new ConjClustering(n, BELIEF, 2, ccap, (t) -> t.isInput());
        new Deriver(new PremiseRuleSet(n).add(c));

        n.inputAt(1, "$1.0 x. |");
        n.inputAt(2, "$1.0 y. |");
        n.inputAt(1, "$0.1 z. |");
        n.inputAt(3, "$0.1 w. |");


        n.run(2);
        //TODO
//        assertEquals(1, n.concept($.$("")).beliefs().size());

    }
    
    
}