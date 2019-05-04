package nars.term.util.conj;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static jcog.math.LongInterval.ETERNAL;
import static nars.$.$$;
import static nars.term.atom.Bool.False;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConjDiffTest {

    @Test
    void testConjDiff_EliminateSeq() {

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

//        assertEq("c", ConjDiff.the(
//                $$("(--x &&+5 c)"), 5, $$("x"), ETERNAL, true).target()); //unchanged
//        assertEq("((--,x) &&+5 c)", ConjDiff.the(
//                $$("(--x &&+5 c)"), 5, $$("x"), 0, true).target()); //unchanged

    }



    @Test
    void testConjDiff_EternalComponents_Same_Masked() {
        //x && ... common to both
        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), 0, $$("(x&&y)"), 0).term());

        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), ETERNAL, $$("(x&&y)"), 0).term());
//        assertEq("z", ConjDiff.the(
//                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).target());

        assertEq("z", ConjDiff.the(
                $$("(x&&z)"), 1, $$("(x&&y)"), 1).term());
        assertEq("(x&&z)", ConjDiff.the(
                $$("(x&&z)"), 1, $$("(x&&y)"), 0).term());
        assertEq("(x&&z)", ConjDiff.the(
                $$("(x&&z)"), 0, $$("(x&&y)"), 1).term());

        assertEq("(c&&x)", ConjDiff.the(
                $$("(x&&(b &&+5 c))"), 5, $$("(x&&(a &&+5 b))"), 0).term());
    }

    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Ete() {
        assertEq("(w&&z)", ConjDiff.the(
                $$("(w && z)"), 5, $$("(x && y)"), 0).term());
    }
    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Seq() {

        assertEq("(((a &&+5 b)&&x)=|>(y &&+5 (c&&y)))", "(((a &&+5 b)&&x) =|> ((b &&+5 c)&&y))");

        assertEq("(y &&+5 (c&&y))", ConjDiff.the(
                $$("(y && (b &&+5 c))"), 5, $$("(x && (a &&+5 b))"), 0).term());
    }

    @Test void testConjWithoutPN_EliminateOnlyOneAtAtime_Seq() {
        Term x = $$("x"), y = $$("y");
        Term xy = $$("((x &&+4120 y) &&+1232 --y)");

        String both = "[(x &&+5352 (--,y)), (x &&+4120 y)]";
        assertConjDiffPN(xy, y, both); //2 compound results
        assertConjDiffPN(xy, y.neg(), both); //2 compound results
        assertConjDiffPN(xy, x, "[(y &&+1232 (--,y))]"); //1 compound result
    }

    private final Term xy = $$("((x &&+4120 (y&&z)) &&+1232 --y)");

    @Test void testConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm() {
        assertConjDiffPN(xy, $$("(y&|z)"), "[(x &&+5352 (--,y))]");
        assertConjDiffPN(xy, $$("(y&|z)").neg(), "[(x &&+5352 (--,y))]");
    }

    @Test void testConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm_unify() {
        assertConjDiffPN(xy, $$("(y&&z)"), "[]"); //TODO unify
    }

    @Test void testConjWithoutPN_EliminateOnlyOneAtAtime_Comm2() {
        Term x = $$("x"), y = $$("y");
        Term xy = $$("(x && y)");
        assertConjDiffPN(xy, y, "[x]");
        assertConjDiffPN(xy, y.neg(), "[x]");
        assertConjDiffPN(xy, x, "[y]");
    }

    static private void assertConjDiffPN(Term xy, Term r, String s) {
        Set<Term> results = new TreeSet();
        for (int i = 0; i < 16; i++)
            results.add(ConjDiff.diffOne(xy, r, true));
        assertEquals(s, results.toString());
    }

}