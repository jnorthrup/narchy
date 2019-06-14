package nars.term.util.conj;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static nars.$.$$;
import static nars.term.atom.Bool.False;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

class ConjDiffTest {

    @Test
    void testConjDiff_EliminateSeq() {

        assertEq("c", Conj.diff(
                $$("(b &&+5 c)"), 5, $$("(a &&+5 b)"), 0).term());
        assertEq("c", Conj.diff(
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
        assertEq("z", Conj.diff(
                $$("(x&&z)"), 0, $$("(x&&y)"), 0).term());

        assertEq("z", Conj.diff(
                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).term());
//        assertEq("z", ConjDiff.the(
//                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).target());

        assertEq("z", Conj.diff(
                $$("(x&&z)"), 1, $$("(x&&y)"), 1).term());
        assertEq("(x&&z)", Conj.diff(
                $$("(x&&z)"), 1, $$("(x&&y)"), 0).term());
        assertEq("(x&&z)", Conj.diff(
                $$("(x&&z)"), 0, $$("(x&&y)"), 1).term());

        assertEq("(c&&x)", Conj.diff(
                $$("(x&&(b &&+5 c))"), 5, $$("(x&&(a &&+5 b))"), 0).term());
    }

    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Ete() {
        assertEq("(w&&z)", Conj.diff(
                $$("(w && z)"), 5, $$("(x && y)"), 0).term());
    }
    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Seq() {

        assertEquals(5, $$("((a &&+5 b)&&x)").eventRange());

        assertEq("(((a &&+5 b)&&x)==>(y &&+5 (c&&y)))", "(((a &&+5 b)&&x)==>((b &&+5 c)&&y))");

        assertEq("(y &&+5 (c&&y))", Conj.diff(
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
        assertConjDiffPN(xy, $$("(y&&z)"), "[(x &&+5352 (--,y))]");
        //assertConjDiffPN(xy, $$("(y&&z)").neg(), "[(x &&+5352 (--,y))]");
    }

    @Test void testConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm_unify() {
        assertConjDiffPN(xy, $$("(z &&+1232 --y)"), "[(x &&+4120 y)]");
    }

    @Test void testConjWithoutPN_EliminateOnlyOneAtAtime_Comm2() {
        Term x = $$("x"), y = $$("y");
        Term xy = $$("(x && y)");
        assertConjDiffPN(xy, y, "[x]");
        assertConjDiffPN(xy, x, "[y]");
    }

    static void assertEventOf(Term xy, Term x) {
        assertTrue( Conj.eventOf(xy, x), ()->"eventOf(" + xy + ","+ x +")" ); //test detection
    }
    static void assertNotEventOf(Term xy, Term x) {
        assertFalse( Conj.eventOf(xy, x), ()->"!eventOf(" + xy + ","+ x +")" ); //test detection
    }

    @Test void eventOf() {
        assertEventOf($$("(x && y)"), $$("x"));
        assertEventOf($$("(x && y)"), $$("y"));
        assertNotEventOf($$("(x && y)"), $$("(x&&y)")); //equal

        assertNotEventOf($$("(x &&+- y)"), $$("(x&&y)")); //component-wise, this is contained

        assertEventOf($$("(x &&+- y)"), $$("x"));
        assertEventOf($$("(x &&+- y)"), $$("y"));

        assertEventOf($$("(x &&+1 y)"), $$("x"));
        assertEventOf($$("(x &&+1 y)"), $$("y"));

        assertEventOf($$("(&&,x,y,z)"), $$("x"));
        assertEventOf($$("(&&,x,y,z)"), $$("(x && y)"));
        assertEventOf($$("(&&,x,y,z)"), $$("(x && z)"));

        assertEventOf($$("((&&,x,y) &&+1 w)"), $$("w"));
        assertEventOf($$("((&&,x,y) &&+1 w)"), $$("(x && y)"));
    }
    @Test void eventOf_SubSeq() {
        assertEventOf(xy, $$("(z &&+1232 --y)"));
        assertNotEventOf(xy, $$("(w &&+1232 --y)")); //wrong start term
        assertNotEventOf(xy, $$("(z &&+1232 w)")); //wrong end term
        assertNotEventOf(xy, $$("(z &&+1231 --y)")); //different sequencing

    }
    @Test void diffCommComm() {
        assertEq("(b&&c)", Conj.diffAll($$("(&&,a,b,c)"), $$("(&&,a,d,e)")));
        assertEq("a", Conj.diffAll($$("(&&,a,b,c)"), $$("(&&,b,c,e)")));
        assertEq("b", Conj.diffAll($$("(&&,a,b,c)"), $$("(&&,a,c,e)")));

        assertEq("b", Conj.diffAll($$("(&&,a,b)"), $$("(&&,a,c)")));
        assertEq("a", Conj.diffAll($$("(&&,a,b)"), $$("(&&,b,c)")));

    }

    static private void assertConjDiffPN(Term xy, Term r, String s) {

        assertEventOf(xy, r);

        Set<Term> results = new TreeSet();
        for (int i = 0; i < 16; i++)
            results.add(Conj.diffAll(xy, r));
        assertEquals(s, results.toString());
    }

}