package nars.term.util.conj;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.ETERNAL;

class ConjTreeTest {

    final static Term w = $$("w");
    final static Term x = $$("x");
    final static Term y = $$("y");
    final static Term z = $$("z");

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
}