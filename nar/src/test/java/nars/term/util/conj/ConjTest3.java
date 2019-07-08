package nars.term.util.conj;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import nars.term.TermTestMisc;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.term.TermTestMisc.assertValid;
import static nars.term.util.TermTest.assertInvalidTerms;
import static org.junit.jupiter.api.Assertions.*;

/**
 * tests specific to conjunction (and disjunction) compounds
 * TODO use assertEq() where possible for target equality test (not junit assertEquals). it applies more rigorous testing
 */
public class ConjTest3 {
    private final NAR n = NARS.shell();

    static final Term x = $.the("x");
    static final Term y = $.the("y");

    @Test
    void testCoNegatedSubtermTask() throws Narsese.NarseseException {


        assertNotNull(Narsese.task("(x &&+1 (--,x)).", n));



    }
    @Test
    void testConegatedTask() {
        assertInvalidTask("(x && (--,x)).");
        assertInvalidTask("(x &| (--,x)).");
    }
    //
    private void assertInvalidTask(String ss) {
        try {
            Narsese.task(ss, n);
            fail("");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    void testFilterCommutedWithCoNegatedSubterms() throws Narsese.NarseseException {


        TermTestMisc.assertValidTermValidConceptInvalidTaskContent(("((--,x) && x)."));
        TermTestMisc.assertValidTermValidConceptInvalidTaskContent("((--,x) &| x).");
        assertValid($("((--,x) &&+1 x)"));
        assertValid($("(x &&+1 x)"));

        assertEquals($("x"), $("(x &| x)"));
        assertEquals($("x"), $("(x && x)"));
        assertNotEquals($("x"), $("(x &&+1 x)"));

        assertInvalidTerms("((--,x) || x)");
        assertInvalidTerms("(&|,(--,(score-->tetris)),(--,(height-->tetris)),(--,(density-->tetris)),(score-->tetris),(height-->tetris))");

    }
    @Disabled
    @Test
    void testRepeatConjunctionTaskSimplification() throws Narsese.NarseseException {

        assertEquals(
                "$.50 (x). 0⋈10 %1.0;.90%",
                Narsese.task("((x) &&+10 (x)). :|:", NARS.shell()).toString());
    }


}

