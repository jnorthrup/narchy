package nars.term.compound;

import nars.$;
import nars.IO;
import nars.Narsese;
import nars.Op;
import nars.index.term.TermKey;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.sub.TermVector1;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/16/16.
 */
public class UnitCompoundTest {

    @Test
    public void testUnitCompound_viaProd() {
        Atomic x = Atomic.the("x");
        assertEqual(PROD, x, new CachedUnitCompound(PROD, x));
    }

    @Test
    public void testCachedUnitCompound1() {
        Atomic x = Atomic.the("x");
        assertEqual(PROD, x, new CachedUnitCompound(PROD, x));
    }

    static void assertEqual(Op o, Atomic x, Compound u) {
        Compound g = new CachedCompound(o, new TermVector1(x));
        assertEquals(g.hashCode(), u.hashCode());
        assertEquals(g.hashCodeSubterms(), u.hashCodeSubterms());
        assertEquals(u, g);
        assertEquals(g, u);
        assertEquals(0, u.compareTo(g));
        assertEquals(0, g.compareTo(u));
        assertEquals(g.toString(), u.toString());
        assertTrue(Arrays.equals(TermKey.term(g).array(), TermKey.term(u).array()));
        assertTrue(Arrays.equals(IO.termToBytes(g), IO.termToBytes(u)));
    }

    @Test
    public void testUnitCompound2() {
        Atomic x = Atomic.the("x");
        Term c = $.p(x);
        System.out.println(c);
        System.out.println(c.sub(0));

        Compound d = $.inh(x, Atomic.the("y"));
        System.out.println(d);
    }

    @Test
    public void testUnitCompound3() {
        Atomic x = Atomic.the("x");
        Atomic y = Atomic.the("y");
        Term c = $.func(x, y);
        System.out.println(c);
        assertEquals("(y)", c.sub(0).toString());
        assertEquals("x", c.sub(1).toString());
    }

//    @Test
//    public void testUnitCompoundNeg() {
//        Atomic x = Atomic.the("x");
//
//        Term u = x.neg();
//
//        CachedCompound g = new CachedUnitCompound(NEG, new TermVector1(x));
//        assertNotSame(u, g);
//        assertEquals(u, g);
//        assertEquals(g, u);
//        assertEquals(u, u);
//        assertEquals(g, g);
//        assertEquals(u.subs(), g.subs());
//        assertEquals(u.dt(), g.dt());
//        assertEquals(u.subterms(), g.subterms());
//        assertEquals(g.subterms(), u.subterms()); //reverse
//        assertEquals(u.hashCode(), g.hashCode());
//        assertEquals(((Compound)u).hashCodeSubterms(), g.hashCodeSubterms());
//        assertEquals(u.toString(), g.toString());
//        assertEquals(0, u.compareTo(g));
//        assertEquals(0, g.compareTo(u));
//        assertEquals(g.structure(), u.structure());
//        assertEquals(g.volume(), u.volume());
//    }

    @Test
    public void testRecursiveContains() throws Narsese.NarseseException {
        Term s = $.$("(--,(x))");
        Term p = $.$("((--,(x)) &&+0 (--,(y)))");
        assertTrue(p.contains(s));
        assertTrue(p.containsRecursively(s));
    }

    @Test
    public void testImpossibleSubterm() throws Narsese.NarseseException {
        assertFalse($.$("(--,(x))").impossibleSubTerm($.$("(x)")));
    }
}