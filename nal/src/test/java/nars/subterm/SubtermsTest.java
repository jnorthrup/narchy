package nars.subterm;

import nars.Narsese;
import nars.Op;
import nars.op.SetFunc;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/1/16.
 */
class SubtermsTest {

    /**
     * recursively
     */
    @NotNull
    private static boolean commonSubtermOrContainment(@NotNull Term a, @NotNull Term b) {

        boolean aCompound = a instanceof Compound;
        boolean bCompound = b instanceof Compound;
        if (aCompound && bCompound) {
            return Subterms.commonSubterms((Compound) a, ((Compound) b), false);
        } else {
            if (aCompound && !bCompound) {
                return a.contains(b);
            } else if (bCompound && !aCompound) {
                return b.contains(a);
            } else {
                
                return a.equals(b);
            }
        }

    }

    @Disabled
    @Test
    void testCommonSubterms() throws Narsese.NarseseException {
        assertTrue(commonSubtermOrContainment($("x"), $("x")));
        assertFalse(commonSubtermOrContainment($("x"), $("y")));
        assertTrue(commonSubtermOrContainment($("(x,y,z)"), $("y")));
        assertFalse(commonSubtermOrContainment($("(x,y,z)"), $("w")));
        assertFalse(Subterms.commonSubterms($("(a,b,c)"), $("(x,y,z)"), false));
        assertTrue(Subterms.commonSubterms($("(x,y)"), $("(x,y,z)"), false));
    }

    @Disabled @Test
    void testCommonSubtermsRecursion() throws Narsese.NarseseException {
        assertTrue(Subterms.commonSubterms($("(x,y)"), $("{a,x}"), false));
        assertFalse(Subterms.commonSubterms($("(x,y)"), $("{a,b}"), false));

        assertFalse(Subterms.commonSubterms($("(#x,y)"), $("{a,#x}"), true));
        assertTrue(Subterms.commonSubterms($("(#x,a)"), $("{a,$y}"), true));
    }

    @Test
    void testUnionReusesInstance() throws Narsese.NarseseException {
        Compound container = $("{a,b}");
        Compound contained = $("{a}");
        assertSame(SetFunc.union(container.op(), container, contained), container);
        assertSame(SetFunc.union(contained.op(), contained, container), container);
        assertSame(SetFunc.union(container.op(), container, container), container);
    }

    @Test
    void testDifferReusesInstance() throws Narsese.NarseseException {
        Compound x = $("{x}");
        Compound y = $("{y}");
        assertSame(Op.differenceSet(x.op(), x, y), x);
    }
    @Test
    void testIntersectReusesInstance() throws Narsese.NarseseException {
        Compound x = $("{x,y}");
        Compound y = $("{x,y}");
        assertSame(SetFunc.intersect(x.op(), x, y), x);
    }

    @Test
    void testSomething() throws Narsese.NarseseException {
        Compound x = $("{e,f}");
        Compound y = $("{e,d}");

        System.out.println(SetFunc.intersect(x.op(), x, y));
        System.out.println(Op.differenceSet(x.op(), x, y));
        System.out.println(SetFunc.union(x.op(), x, y));

    }

    @Test
    void testEqualityOfUniSubtermsImpls() {
        Term a = Atomic.the("a");
        Subterms x = new UniSubterm(a);
        Subterms x0 = new UniSubterm(a);
        assertEquals(x, x0);

        Subterms y = new ArrayTermVector(a);
        assertEquals(y.hashCode(), x.hashCode());
        assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        assertEquals(x, y);
        assertEquals(y, x);

        Subterms z = new UniSubterm(a);
        assertEquals(y.hashCode(), z.hashCode());
        assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        assertEquals(y, z);
        assertEquals(x, z);
        assertEquals(z, y);
        assertEquals(z, x);

    }

    @Test
    void testEqualityOfBiSubtermsImpls() {
        Term a = Atomic.the("a");
        Term b = Atomic.the("b");
        Subterms x = new BiSubterm(a,b);
        Subterms x0 = new BiSubterm(a, b);
        assertEquals(x, x0);

        Subterms y = new ArrayTermVector(a, b);
        assertEquals(y.hashCode(), x.hashCode());
        assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        assertEquals(x, y);
        assertEquals(y, x);

        Subterms z =  new BiSubterm(a,b);
        assertEquals(y.hashCode(), z.hashCode());
        assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        assertEquals(y, z);
        assertEquals(x, z);
        assertEquals(z, y);
        assertEquals(z, x);

    }

    @Test
    void testEqualityOfBiSubtermReverseImpls() {
        Term a = Atomic.the("a");
        Term b = Atomic.the("b");
        Subterms ab = new BiSubterm(a,b);
        Subterms x = new BiSubterm.ReversibleBiSubterm(a,b).reverse();
        Subterms x0 = new BiSubterm.ReversibleBiSubterm(a,b).reverse();
        assertEquals(x, x0);
        assertNotEquals(ab, x);

        Subterms y = new ArrayTermVector(b, a);
        assertEquals(y.hashCode(), x.hashCode());
        assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        assertEquals(x, y);
        assertEquals(y, x);

        Subterms z =  new BiSubterm.ReversibleBiSubterm(a,b).reverse();
        assertEquals(y.hashCode(), z.hashCode());
        assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        assertEquals(y, z);
        assertEquals(x, z);
        assertEquals(z, y);
        assertEquals(z, x);

    }




}