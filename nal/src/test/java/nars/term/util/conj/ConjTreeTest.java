package nars.term.util.conj;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.atom.Bool.False;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;

class ConjTreeTest {

    static final Term w = $$("w");
    static final Term x = $$("x");
    static final Term y = $$("y");
    static final Term z = $$("z");

    @Test
    void testSimple() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, y);
        assertEq("(x&&y)", t.term());
    }

    @Test
    void testSimpleWithNeg() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x.neg());
        t.add(ETERNAL, y);
        assertEq("((--,x)&&y)", t.term());
    }

    @Test
    void testSimpleSeq() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(1, y);
        t.add(2, z);
        assertEq("((y &&+1 z)&&x)", t.term());
    }

    @Test
    void testSimpleSeq2() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, w.neg());
        t.add(1, y);
        t.add(2, z);
        assertEq("(&&,(y &&+1 z),(--,w),x)", t.term());
    }

    @Test
    void testContradict1() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, x.neg());
        assertEq(False, t.term());
    }

    @Test
    void testDisjReductionOutward() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, $$("(x||y)"));
        assertEq(x, t.term());
    }
    @Test
    void testDisjReductionOutwardSeq() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(ETERNAL, $$("--(--y &&+1 --x)"));
        assertEq(x, t.term());
    }

    @Test
    void testDisjReductionOutwardCancel() {

        /* (not(not(y) and x) and x) */
        assertEq("(x&&y)", "(--(--y && x) && x)");

        assertEq("(x&&y)", "(--(--y &&+1 x) && x)");

        {
            ConjTree t = new ConjTree();
            t.add(ETERNAL, x);
            t.add(ETERNAL, $$("--(--y &&+1 x)"));
            assertEq("(x&&y)", t.term());
        }
    }

    @Test
    void testContradictionInward() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(1, x.neg());
        assertEq(False, t.term());
    }
    @Test
    void testContradictionInward2() {
        ConjTree t = new ConjTree();
        t.add(ETERNAL, x);
        t.add(1, y);
        t.add(2, x.neg());
        assertEq(False, t.term());
    }

}