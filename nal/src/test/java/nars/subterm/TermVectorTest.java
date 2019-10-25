package nars.subterm;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.util.TermTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/12/15.
 */
class TermVectorTest {

    @Test
    void testSubtermsEquality() throws Narsese.NarseseException {

        Term a = $.INSTANCE.$("(a-->b)");
        Compound b = $.INSTANCE.impl(Atomic.the("a"), Atomic.the("b"));
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());

        TermTest.assertEq(a.subterms(), b.subterms());


        assertNotEquals(0, a.compareTo(b));
        assertNotEquals(0, b.compareTo(a));

        /*assertTrue("after equality test, subterms vector determined shareable",
                a.subterms() == b.subterms());*/


    }

    @Test
    void testSortedTermContainer() throws Narsese.NarseseException {
        Term aa = $.INSTANCE.$("a");
        Term bb = $.INSTANCE.$("b");
        Subterms a = Op.terms.subterms(aa, bb);
        assertTrue(a.isSorted());
        Subterms b = Op.terms.subterms(bb, aa);
        assertFalse(b.isSorted());
        Subterms s = Op.terms.subterms(Terms.commute(b.arrayShared()));
        assertTrue(s.isSorted());
        assertEquals(a, s);
        assertNotEquals(b, s);
    }


}
