package nars.term.util.conj;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;
import java.util.function.Supplier;

import static nars.$.*;
import static nars.term.atom.IdempotentBool.False;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

class ConjDiffTest {

    @Test
    void testConjDiff_EliminateSeq() {

        assertEq("c", Conj.diff(
                INSTANCE.$$("(b &&+5 c)"), 5, INSTANCE.$$("(a &&+5 b)"), 0).term());
        assertEq("c", Conj.diff(
                INSTANCE.$$("(--b &&+5 c)"), 5, INSTANCE.$$("(a &&+5 --b)"), 0).term());
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
                INSTANCE.$$("(x&&z)"), 0, INSTANCE.$$("(x&&y)"), 0).term());

        assertEq("z", Conj.diff(
                INSTANCE.$$("(x&&z)"), 0, INSTANCE.$$("(x&&y)"), ETERNAL).term());
//        assertEq("z", ConjDiff.the(
//                $$("(x&&z)"), 0, $$("(x&&y)"), ETERNAL).target());

        assertEq("z", Conj.diff(
                INSTANCE.$$("(x&&z)"), 1, INSTANCE.$$("(x&&y)"), 1).term());
        assertEq("(x&&z)", Conj.diff(
                INSTANCE.$$("(x&&z)"), 1, INSTANCE.$$("(x&&y)"), 0).term());
        assertEq("(x&&z)", Conj.diff(
                INSTANCE.$$("(x&&z)"), 0, INSTANCE.$$("(x&&y)"), 1).term());

        assertEq("(c&&x)", Conj.diff(
                INSTANCE.$$("(x&&(b &&+5 c))"), 5, INSTANCE.$$("(x&&(a &&+5 b))"), 0).term());
    }

    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Ete() {
        assertEq("(w&&z)", Conj.diff(
                INSTANCE.$$("(w && z)"), 5, INSTANCE.$$("(x && y)"), 0).term());
    }
    @Test
    void testConjDiff_EternalComponents_Diff_Masked_Seq() {

        assertEquals(5, INSTANCE.$$("((a &&+5 b)&&x)").eventRange());

        assertEq("(((a &&+5 b)&&x)==>(y &&+5 (c&&y)))", "(((a &&+5 b)&&x)==>((b &&+5 c)&&y))");

        assertEq("(y &&+5 (c&&y))", Conj.diff(
                INSTANCE.$$("(y && (b &&+5 c))"), 5, INSTANCE.$$("(x && (a &&+5 b))"), 0).term());
    }

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Seq() {
        Term x = INSTANCE.$$("x"), y = INSTANCE.$$("y");
        Term xy = INSTANCE.$$("((x &&+4120 y) &&+1232 --y)");

        String both = "[(x &&+5352 (--,y)), (x &&+4120 y)]";
        assertConjDiffPN(xy, y, "[x]"); //2 compound results
        assertConjDiffPN(xy, y.neg(), "[x]"); //2 compound results
        assertConjDiffPN(xy, x, "[(y &&+1232 (--,y))]"); //1 compound result
    }

    private final Term xy = INSTANCE.$$("((x &&+4120 (y&&z)) &&+1232 --y)");

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm() {
        assertConjDiffPN(xy, INSTANCE.$$("(y&&z)"), "[(x &&+5352 (--,y))]");
        //assertConjDiffPN(xy, $$("(y&&z)").neg(), "[(x &&+5352 (--,y))]");
    }

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Seq_with_inner_Comm_unify() {
        assertConjDiffPN(xy, INSTANCE.$$("(z &&+1232 --y)"), "[(x &&+4120 y)]");
    }

    @Test void ConjWithoutPN_EliminateOnlyOneAtAtime_Comm2() {
        Term x = INSTANCE.$$("x"), y = INSTANCE.$$("y");
        Term xy = INSTANCE.$$("(x && y)");
        assertConjDiffPN(xy, y, "[x]");
        assertConjDiffPN(xy, x, "[y]");
    }

    static void assertEventOf(Term xy, Term x) {
        assertTrue( Conj.eventOf(xy, x), new Supplier<String>() {
            @Override
            public String get() {
                return "eventOf(" + xy + "," + x + ")";
            }
        }); //test detection
    }
    static void assertNotEventOf(Term xy, Term x) {
        assertFalse( Conj.eventOf(xy, x), new Supplier<String>() {
            @Override
            public String get() {
                return "!eventOf(" + xy + "," + x + ")";
            }
        }); //test detection
    }

    @Test void eventOf() {
        assertEventOf(INSTANCE.$$("(x && y)"), INSTANCE.$$("x"));
        assertEventOf(INSTANCE.$$("(x && y)"), INSTANCE.$$("y"));
        assertNotEventOf(INSTANCE.$$("(x && y)"), INSTANCE.$$("(x&&y)")); //equal

        assertNotEventOf(INSTANCE.$$("(x &&+- y)"), INSTANCE.$$("(x&&y)")); //component-wise, this is contained

        assertEventOf(INSTANCE.$$("(x &&+- y)"), INSTANCE.$$("x"));
        assertEventOf(INSTANCE.$$("(x &&+- y)"), INSTANCE.$$("y"));

        assertEventOf(INSTANCE.$$("(x &&+1 y)"), INSTANCE.$$("x"));
        assertEventOf(INSTANCE.$$("(x &&+1 y)"), INSTANCE.$$("y"));

        assertEventOf(INSTANCE.$$("(&&,x,y,z)"), INSTANCE.$$("x"));
        assertEventOf(INSTANCE.$$("(&&,x,y,z)"), INSTANCE.$$("(x && y)"));
        assertEventOf(INSTANCE.$$("(&&,x,y,z)"), INSTANCE.$$("(x && z)"));

        assertEventOf(INSTANCE.$$("((&&,x,y) &&+1 w)"), INSTANCE.$$("w"));
        assertEventOf(INSTANCE.$$("((&&,x,y) &&+1 w)"), INSTANCE.$$("(x && y)"));
    }
    @Test void eventOf_SubSeq() {
        assertEventOf(xy, INSTANCE.$$("(z &&+1232 --y)"));
        assertNotEventOf(xy, INSTANCE.$$("(w &&+1232 --y)")); //wrong start term
        assertNotEventOf(xy, INSTANCE.$$("(z &&+1232 w)")); //wrong end term
        assertNotEventOf(xy, INSTANCE.$$("(z &&+1231 --y)")); //different sequencing

    }
    @Test void diffCommComm() {
        assertEq("(b&&c)", Conj.diffAll(INSTANCE.$$("(&&,a,b,c)"), INSTANCE.$$("(&&,a,d,e)")));
        assertEq("a", Conj.diffAll(INSTANCE.$$("(&&,a,b,c)"), INSTANCE.$$("(&&,b,c,e)")));
        assertEq("b", Conj.diffAll(INSTANCE.$$("(&&,a,b,c)"), INSTANCE.$$("(&&,a,c,e)")));

        assertEq("b", Conj.diffAll(INSTANCE.$$("(&&,a,b)"), INSTANCE.$$("(&&,a,c)")));
        assertEq("a", Conj.diffAll(INSTANCE.$$("(&&,a,b)"), INSTANCE.$$("(&&,b,c)")));

    }

    private static void assertConjDiffPN(Term xy, Term r, String s) {

        assertEventOf(xy, r);

        Supplier<TreeSet> collectionFactory = TreeSet::new;
        var results = collectionFactory.get();
        for (int i = 0; i < 16; i++) {
            Term term = Conj.diffAllPN(xy, r);
            results.add(term);
        }
        assertEquals(s, results.toString());
    }

}