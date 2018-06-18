package nars.task;

import nars.*;
import nars.concept.Concept;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/3/15.
 */
class TaskTest {

    @Test
    void testTenseEternality() throws Narsese.NarseseException {
        NAR n = new NARS().get();

        String s = "<a --> b>.";

        assertEquals(Narsese.the().task(s, n).start(), ETERNAL);

        assertTrue(Narsese.the().task(s, n).isEternal(), "default is eternal");

        assertEquals(Narsese.the().task(s, n).start(), ETERNAL, "tense=eternal is eternal");

        

    }










































































    @Test
    void inputTwoUniqueTasksDef() throws Narsese.NarseseException {
        inputTwoUniqueTasks(new NARS().get());
    }
    /*@Test public void inputTwoUniqueTasksSolid() {
        inputTwoUniqueTasks(new Solid(4, 1, 1, 1, 1, 1));
    }*/
    /*@Test public void inputTwoUniqueTasksEq() {
        inputTwoUniqueTasks(new Equalized(4, 1, 1));
    }
    @Test public void inputTwoUniqueTasksNewDef() {
        inputTwoUniqueTasks(new Default());
    }*/

    private void inputTwoUniqueTasks(@NotNull NAR n) throws Narsese.NarseseException {

        

        Task x = n.inputTask("<a --> b>.");
        assertArrayEquals(new long[]{1}, x.stamp());
        n.run();

        Task y = n.inputTask("<b --> c>.");
        assertArrayEquals(new long[]{2}, y.stamp());
        n.run();

        n.reset();

        n.input("<e --> f>.  <g --> h>. "); 

        n.run(10);

        Task q = n.inputTask("<c --> d>.");
        assertArrayEquals(new long[]{5}, q.stamp());

    }


    @Test
    void testDoublePremiseMultiEvidence() throws Narsese.NarseseException {

        
        
        NAR d = new NARS().get();
        
        d.input("<a --> b>.", "<b --> c>.");

        long[] ev = {1, 2};
        d.eventTask.on(t -> {

            if (t instanceof DerivedTask && ((DerivedTask)t).getParentBelief()!=null && !t.isCyclic())
                assertArrayEquals(ev, t.stamp(), "all double-premise derived terms have this evidence: "
                        + t + ": " + Arrays.toString(ev) + "!=" + Arrays.toString(t.stamp()));

            System.out.println(t.proof());

        });

        d.run(256);


    }

    @Test
    void testValid() throws Narsese.NarseseException {
        NAR tt = NARS.shell();
        Task t = $.task("((&&,#1,(#1 &&+0 #3),(#2 &&+0 #3),(#2 &&+0 (toothbrush-->here))) ==>+0 lighter(I,toothbrush))", BELIEF, 1f, 0.9f).apply(tt);
        assertNotNull(t);
        Concept c = t.concept(tt,true);
        assertNotNull(c);
    }



}
