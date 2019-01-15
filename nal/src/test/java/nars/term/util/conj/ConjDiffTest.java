package nars.term.util.conj;

import org.junit.jupiter.api.Test;

import static jcog.math.LongInterval.ETERNAL;
import static nars.$.$$;
import static nars.term.atom.Bool.False;
import static nars.term.util.TermTest.assertEq;

class ConjDiffTest {

    @Test
    void testConjDiff_Eliminate() {

        assertEq("c", ConjDiff.the(
                $$("(b &&+5 c)"), 5, $$("(a &&+5 b)"), 0).term());
        assertEq("c", ConjDiff.the(
                $$("(--b &&+5 c)"), 5, $$("(a &&+5 --b)"), 0).term());
    }

    @Test
    void testConjDiff_Eliminate_invert() {

        assertEq(False,
                "(--(x &&+1 y) ==>+1 (y &&+1 z))");
        assertEq("((--,(x &&+1 y)) ==>+2 z)",
                "(--(x &&+1 y) ==>+1 (--y &&+1 z))");

        assertEq("c", ConjDiff.the(
                $$("(--x &&+5 c)"), 5, $$("x"), ETERNAL, true).term()); //unchanged
        assertEq("((--,x) &&+5 c)", ConjDiff.the(
                $$("(--x &&+5 c)"), 5, $$("x"), 0, true).term()); //unchanged

    }



    @Test
    void testConjDiff_EternalComponents_Same_Masked() {
        //x && ... common to both
        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), 0, $$("(x&&y)"), 0).term());

        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), ETERNAL, $$("(x&&y)"), 0).term());
        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).term());

        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), 1, $$("(x&&y)"), 1).term());
        assertEq("(x&|z)", ConjDiff.the(
                $$("(x&&z)"), 1, $$("(x&&y)"), 0).term());
        assertEq("(x&|z)", ConjDiff.the(
                $$("(x&&z)"), 0, $$("(x&&y)"), 1).term());

        assertEq("(c&|x)", ConjDiff.the(
                $$("(x&&(b &&+5 c))"), 5, $$("(x&&(a &&+5 b))"), 0).term());
    }

    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Ete() {
        assertEq("(w&|z)", ConjDiff.the(
                $$("(w && z)"), 5, $$("(x && y)"), 0).term());
    }
    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Seq() {

        assertEq("(((a &&+5 b)&&x)=|>(y &&+5 (c&|y)))", "(((a &&+5 b)&&x) =|> ((b &&+5 c)&&y))");

        assertEq("(y &&+5 (c&|y))", ConjDiff.the(
                $$("(y && (b &&+5 c))"), 5, $$("(x && (a &&+5 b))"), 0).term());
    }

}