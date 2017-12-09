package nars.term;

import nars.Narsese;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.container.ArrayTermVector;
import nars.term.container.Subterms;
import nars.term.container.TermVector1;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/1/16.
 */
public class SubtermsTest {

    /**
     * recursively
     */
    @NotNull
    static boolean commonSubtermOrContainment(@NotNull Term a, @NotNull Term b) {

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
                //neither are compounds
                return a.equals(b);
            }
        }

    }

    @Disabled
    @Test
    public void testCommonSubterms() throws Narsese.NarseseException {
        assertTrue(commonSubtermOrContainment($("x"), $("x")));
        assertFalse(commonSubtermOrContainment($("x"), $("y")));
        assertTrue(commonSubtermOrContainment($("(x,y,z)"), $("y")));
        assertFalse(commonSubtermOrContainment($("(x,y,z)"), $("w")));
        assertFalse(Subterms.commonSubterms($("(a,b,c)"), $("(x,y,z)"), false));
        assertTrue(Subterms.commonSubterms($("(x,y)"), $("(x,y,z)"), false));
    }

    @Disabled @Test
    public void testCommonSubtermsRecursion() throws Narsese.NarseseException {
        assertTrue(Subterms.commonSubterms($("(x,y)"), $("{a,x}"), false));
        assertFalse(Subterms.commonSubterms($("(x,y)"), $("{a,b}"), false));

        assertFalse(Subterms.commonSubterms($("(#x,y)"), $("{a,#x}"), true));
        assertTrue(Subterms.commonSubterms($("(#x,a)"), $("{a,$y}"), true));
    }

    @Test
    public void testUnionReusesInstance() throws Narsese.NarseseException {
        Compound container = $("{a,b}");
        Compound contained = $("{a}");
        assertSame(Terms.union(container.op(), container, contained), container);
        assertSame(Terms.union(contained.op(), contained, container), container);
        assertSame(Terms.union(container.op(), container, container), container);
    }

    @Test
    public void testDifferReusesInstance() throws Narsese.NarseseException {
        Compound x = $("{x}");
        Compound y = $("{y}");
        assertSame(Op.difference(x.op(), x, y), x);
    }
    @Test
    public void testIntersectReusesInstance() throws Narsese.NarseseException {
        Compound x = $("{x,y}");
        Compound y = $("{x,y}");
        assertSame(Terms.intersect(x.op(), x, y), x);
    }

    @Test
    public void testSomething() throws Narsese.NarseseException {
        Compound x = $("{e,f}");
        Compound y = $("{e,d}");

        System.out.println(Terms.intersect(x.op(), x, y));
        System.out.println(Op.difference(x.op(), x, y));
        System.out.println(Terms.union(x.op(), x, y));

    }

    @Test
    public void testEqualityOfVector1() {
        Term a = Atomic.the("a");
        Subterms x = new TermVector1(a);
        Subterms y = new TermVector1(a);
        assertEquals(x, y);

        Subterms z = new ArrayTermVector(a);
        assertEquals(z.hashCode(), x.hashCode());
        assertEquals(x, z);
        assertEquals(z, x);

    }
    
//    @Test public void testSubtermsExcept() {
//        //TODO
//    }

}