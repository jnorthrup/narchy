package nars.term.compound;

import nars.$;
import nars.Narsese;
import nars.index.term.TermKey;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermVector1;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static nars.Op.NEG;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/16/16.
 */
public class UnitCompound1Test {

    @Test
    public void testUnitCompound1() {
        Atomic x = Atomic.the("x");
        UnitCompound1 u = new UnitCompound1(PROD, x);
        Compound g = new CachedCompound(PROD, new TermVector1(x));
        assertEquals(g.hashCode(), u.hashCode());
        assertEquals(u, g);
        assertEquals(g, u);
        assertEquals(0, u.compareTo(g));
        assertEquals(0, g.compareTo(u));
        assertEquals(g.toString(), u.toString());
        assertTrue(Arrays.equals(TermKey.term(g).array(), TermKey.term(u).array()));
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

    @Test
    public void testUnitCompoundNeg() {
        Atomic x = Atomic.the("x");

        Term u = x.neg();
//        System.out.println(u);
//        System.out.println(u.sub(0));
        assertEquals(UnitCompound1.class, u.getClass());

        CachedCompound g = new CachedCompound(NEG, new TermVector1(x));
        assertNotSame(u, g);
        assertEquals(u, g);
        assertEquals(g, u);
        assertEquals(u, u);
        assertEquals(g, g);
        assertEquals(u.subs(), g.subs());
        assertEquals(u.dt(), g.dt());
        assertEquals(u.subterms(), g.subterms());
        assertEquals(g.subterms(), u.subterms()); //reverse
        assertEquals(u.hashCode(), g.hashCode());
        assertEquals(((Compound)u).hashCodeSubterms(), g.hashCodeSubterms());
        assertEquals(u.toString(), g.toString());
        assertEquals(0, u.compareTo(g));
        assertEquals(0, g.compareTo(u));
        assertEquals(g.structure(), u.structure());
        assertEquals(g.volume(), u.volume());
    }

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