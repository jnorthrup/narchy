package nars.term.compound;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.io.IO;
import nars.subterm.UniSubterm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.map.TermRadixTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/16/16.
 */
class UnitCompoundTest {

    @Test
    void testUnitCompound_viaProd() {
        Atomic x = Atomic.the("x");
        assertEqual(PROD, x, new CachedUnitCompound(PROD, x));
    }

    @Test
    void testCachedUnitCompound1() {
        Atomic x = Atomic.the("x");
        assertEqual(PROD, x, new CachedUnitCompound(PROD, x));
    }

    private static void assertEqual(Op o, Atomic x, Compound u) {
        Compound g = Op.terms.newCompound(o, new UniSubterm(x));
        assertEquals(g.hashCode(), u.hashCode(), ()->"inconsistent hash:\n" + g + "\n" + u);
        assertEquals(g.hashCodeSubterms(), u.hashCodeSubterms(), ()->"inconsistent subhash:\n" + g + "\n" + u);
        assertEquals(u, g);
        assertEquals(g, u);
        assertEquals(0, u.compareTo(g));
        assertEquals(0, g.compareTo(u));
        assertEquals(g.toString(), u.toString());
        assertTrue(Arrays.equals(TermRadixTree.termByVolume(g).arrayCompactDirect(), TermRadixTree.termByVolume(u).arrayCompactDirect()));
        assertTrue(Arrays.equals(IO.termToBytes(g), IO.termToBytes(u)));
    }

    @Test
    void testUnitCompound2() {
        Atomic x = Atomic.the("x");
        Term c = $.p(x);
        System.out.println(c);
        System.out.println(c.sub(0));

        Compound d = $.inh(x, Atomic.the("y"));
        System.out.println(d);
    }

    @Test
    void testUnitCompound3() {
        Atomic x = Atomic.the("x");
        Atomic y = Atomic.the("y");
        Term c = $.func(x, y);
        System.out.println(c);
        assertEquals("(y)", c.sub(0).toString());
        assertEquals("x", c.sub(1).toString());
    }


























    @Test
    void testRecursiveContains() throws Narsese.NarseseException {
        Term s = $.$("(--,(x))");
        Term p = $.$("((--,(x)) &&+0 (--,(y)))");
        assertTrue(p.contains(s));
        assertTrue(p.containsRecursively(s));
    }

    @Test
    void testImpossibleSubterm() throws Narsese.NarseseException {
        assertFalse($.$("(--,(x))").impossibleSubTerm($.$("(x)")));
    }
}