package nars.concept.dynamic;

import nars.*;
import nars.link.AtomicTaskLink;
import nars.time.When;
import nars.time.event.WhenTimeIs;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 10/27/16.
 */
class DynamicTruthBeliefTableTest {


    @Test
    void testDynamicBeliefTableSampling() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("x", 0f, 0.50f);
        n.believe("y", 0f, 0.50f);
        n.run(1);
        AtomicTaskLink tl = new AtomicTaskLink($$("(x && y)"), Op.EmptyProduct, BELIEF,  1f);
        Set<Task> tasks = new HashSet();
        When w = WhenTimeIs.eternal(n);
        for (int i = 0; i < 10; i++)
            tasks.add(tl.get(w));
        assertTrue(tasks.toString().contains("(x&&y). %0.0;.25%]"));
        tasks.forEach(System.out::println);
    }

    @Test
    void testDynamicBeliefTableSamplingTemporalFlexible() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.input("x. +1");
        n.input("y. +2");
        n.run(1);
        AtomicTaskLink tl = new AtomicTaskLink($$("(x && y)"), Op.EmptyProduct,  BELIEF, 1f);
        Set<Task> tasks = new HashSet();
        When w = new When(1, 2, 1, n);
        for (int i = 0; i < 100; i++)
            tasks.add(tl.get(w));
        assertTrue( tasks.toString().contains("(x &&+1 y). 1 %1.0;.81%]"), ()->tasks.toString());
        //assertEquals("[$.50 (x&|y). 1 %1.0;.74%]", tasks.toString());

    }

}

//    @Disabled
//    @Test
//    public void testDynamicIntRange() throws Narsese.NarseseException {
//        NAR n = NARS.shell();
//        n.believe("x:1", 1f, 0.9f);
//        n.believe("x:2", 0.5f, 0.9f);
//        n.believe("x:3", 0f, 0.9f);
//        n.run(1);
//
//        Concept x12 = n.conceptualize($.inh(Int.range(1, 2), $.the("x")));
//        Concept x23 = n.conceptualize($.inh(Int.range(2, 3), $.the("x")));
//        Concept x123 = n.conceptualize($.inh(Int.range(1, 3), $.the("x")));
//        assertEquals("%.50;.81%", n.beliefTruth(x12, ETERNAL).toString());
//        assertEquals("%0.0;.90%", n.beliefTruth(x23, ETERNAL).toString());
//        assertEquals("%0.0;.90%", n.beliefTruth(x123, ETERNAL).toString());
//    }
//
//    @Disabled @Test
//    public void testDynamicIntVectorRange() throws Narsese.NarseseException {
//        NAR n = NARS.shell();
//        n.believe("x(1,1)", 1f, 0.9f);
//        n.believe("x(1,2)", 0.5f, 0.9f);
//        n.believe("x(1,3)", 0f, 0.9f);
//        n.run(1);
//
//        Term t12 = $.inh($.p(Int.the(1), Int.range(1, 2)), $.the("x"));
//        assertEquals("x(1,1..2)", t12.toString());
//        Concept x12 = n.conceptualize(t12);
//        assertTrue(x12.beliefs() instanceof DynamicTruthBeliefTable);
//
//        Concept x23 = n.conceptualize($.inh($.p(Int.the(1), Int.range(2, 3)), $.the("x")));
//        Concept x123 = n.conceptualize($.inh($.p(Int.the(1), Int.range(1, 3)), $.the("x")));
//        assertEquals("%.50;.81%", n.beliefTruth(x12, ETERNAL).toString());
//        assertEquals("%0.0;.90%", n.beliefTruth(x23, ETERNAL).toString());
//        assertEquals("%0.0;.90%", n.beliefTruth(x123, ETERNAL).toString());
//    }
