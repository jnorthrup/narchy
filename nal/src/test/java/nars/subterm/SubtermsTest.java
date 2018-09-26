package nars.subterm;

import com.google.common.collect.Iterators;
import nars.$;
import nars.IO;
import nars.Narsese;
import nars.Op;
import nars.op.SetFunc;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.SetSectDiff;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.Op.PROD;
import static nars.Op.SETe;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/1/16.
 */
public class SubtermsTest {

    @Test
    void testUnionReusesInstance() throws Narsese.NarseseException {
        Compound container = $.$("{a,b}");
        Compound contained = $.$("{a}");
        assertSame(SetFunc.union(container.op(), container, contained), container);
        assertSame(SetFunc.union(contained.op(), contained, container), container);
        assertSame(SetFunc.union(container.op(), container, container), container);
    }

    @Test
    void testDifferReusesInstance() throws Narsese.NarseseException {
        Compound x = $.$("{x}");
        Compound y = $.$("{y}");
        assertSame(SetSectDiff.differenceSet(x.op(), x, y), x);
    }
    @Test
    void testIntersectReusesInstance() throws Narsese.NarseseException {
        Compound x = $.$("{x,y}");
        Compound y = $.$("{x,y}");
        assertSame(SetFunc.intersect(x.op(), x, y), x);
    }

    @Test
    void testSomething() throws Narsese.NarseseException {
        Compound x = $.$("{e,f}");
        Compound y = $.$("{e,d}");

        System.out.println(SetFunc.intersect(x.op(), x, y));
        System.out.println(SetSectDiff.differenceSet(x.op(), x, y));
        System.out.println(SetFunc.union(x.op(), x, y));

    }

    @Test
    void testEqualityOfUniSubtermsImpls() {
        Term a = Atomic.the("a");
        Subterms x = new UniSubterm(a);
        Subterms x0 = new UniSubterm(a);
        Assertions.assertEquals(x, x0);

        Subterms y = new ArrayTermVector(a);
        Assertions.assertEquals(y.hashCode(), x.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        Assertions.assertEquals(x, y);
        Assertions.assertEquals(y, x);

        Subterms z = new UniSubterm(a);
        Assertions.assertEquals(y.hashCode(), z.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        Assertions.assertEquals(y, z);
        Assertions.assertEquals(x, z);
        Assertions.assertEquals(z, y);
        Assertions.assertEquals(z, x);

    }

    @Test
    void testEqualityOfBiSubtermsImpls() {
        Term a = Atomic.the("a");
        Term b = Atomic.the("b");
        Subterms x = new BiSubterm(a,b);
        Subterms x0 = new BiSubterm(a, b);
        Assertions.assertEquals(x, x0);

        Subterms y = new ArrayTermVector(a, b);

        assertEquals(x, y);

        Assertions.assertEquals(y.hashCode(), x.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        Assertions.assertEquals(x, y);
        Assertions.assertEquals(y, x);

        Subterms z =  new BiSubterm(a,b);
        Assertions.assertEquals(y.hashCode(), z.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        Assertions.assertEquals(y, z);
        Assertions.assertEquals(x, z);
        Assertions.assertEquals(z, y);
        Assertions.assertEquals(z, x);

    }

    @Test
    void testEqualityOfBiSubtermReverseImpls() {
        Term a = Atomic.the("a");
        Term b = Atomic.the("b");
        Subterms ab = new BiSubterm(a,b);
        Subterms x = new BiSubterm.ReversibleBiSubterm(a,b).reverse();
        Subterms x0 = new BiSubterm.ReversibleBiSubterm(a,b).reverse();
        Assertions.assertEquals(x, x0);
        assertNotEquals(ab, x);

        Subterms y = new ArrayTermVector(b, a);
        Assertions.assertEquals(x, y);

        Assertions.assertEquals(y.hashCode(), x.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), x.hashCodeSubterms());
        Assertions.assertEquals(x, y);
        Assertions.assertEquals(y, x);

        Subterms z =  new BiSubterm.ReversibleBiSubterm(a,b).reverse();
        Assertions.assertEquals(y.hashCode(), z.hashCode());
        Assertions.assertEquals(y.hashCodeSubterms(), z.hashCodeSubterms());
        Assertions.assertEquals(y, z);
        Assertions.assertEquals(x, z);
        Assertions.assertEquals(z, y);
        Assertions.assertEquals(z, x);

    }


//    /**
//     * recursively
//     */
//    @NotNull
//    private static boolean commonSubtermOrContainment(@NotNull Term a, @NotNull Term b) {
//
//        boolean aCompound = a instanceof Compound;
//        boolean bCompound = b instanceof Compound;
//        if (aCompound && bCompound) {
//            return Subterms.commonSubterms((Compound) a, ((Compound) b), false);
//        } else {
//            if (aCompound && !bCompound) {
//                return a.contains(b);
//            } else if (bCompound && !aCompound) {
//                return b.contains(a);
//            } else {
//
//                return a.equals(b);
//            }
//        }
//
//    }

    public static void assertEquals(Subterms a, Subterms b) {
        Assertions.assertEquals(a, b);
        Assertions.assertEquals(b, a);
        Assertions.assertEquals(a.toString(), b.toString());
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertEquals(a.hashCodeSubterms(), b.hashCodeSubterms());
        assertTrue(Iterators.elementsEqual(a.iterator(), b.iterator()));
        Assertions.assertEquals(Op.terms.theCompound(PROD, a), Op.terms.theCompound(PROD, b));
        Assertions.assertEquals(Op.terms.theCompound(PROD, b), Op.terms.theCompound(PROD, a));


        org.junit.jupiter.api.Assertions.assertEquals(0, Subterms.compare(a, b));
        org.junit.jupiter.api.Assertions.assertEquals(0, Subterms.compare(a, b));

        {
            byte[] bytesExpected = IO.termToBytes($.pFast(a));
            byte[] bytesActual = IO.termToBytes($.pFast(b));
            assertArrayEquals(bytesExpected, bytesActual);
        }
        {
            if (a.subs() > 0) {
                byte[] bytesExpected = IO.termToBytes($.sFast(a));
                byte[] bytesActual = IO.termToBytes($.sFast(b));
                assertArrayEquals(bytesExpected, bytesActual);
            }
        }
        {
            byte[] bytesExpected = IO.termToBytes(PROD.the(a));
            byte[] bytesActual = IO.termToBytes(PROD.the(b));
            assertArrayEquals(bytesExpected, bytesActual);
        }

        {
            byte[] bytesExpected = IO.termToBytes(PROD.the(a));
            byte[] bytesActual = IO.termToBytes(PROD.the(b));
            assertArrayEquals(bytesExpected, bytesActual);
        }

        {
            if (a.subs() > 0) {
                byte[] bytesExpected = IO.termToBytes(SETe.the(a));
                byte[] bytesActual = IO.termToBytes(SETe.the(b));
                assertArrayEquals(bytesExpected, bytesActual);
            }
        }

    }

//    @Disabled
//    @Test
//    void testCommonSubterms() throws Narsese.NarseseException {
//        assertTrue(commonSubtermOrContainment($("x"), $("x")));
//        assertFalse(commonSubtermOrContainment($("x"), $("y")));
//        assertTrue(commonSubtermOrContainment($("(x,y,z)"), $("y")));
//        assertFalse(commonSubtermOrContainment($("(x,y,z)"), $("w")));
//        assertFalse(Subterms.commonSubterms($("(a,b,c)"), $("(x,y,z)"), false));
//        assertTrue(Subterms.commonSubterms($("(x,y)"), $("(x,y,z)"), false));
//    }
//
//    @Disabled @Test
//    void testCommonSubtermsRecursion() throws Narsese.NarseseException {
//        assertTrue(Subterms.commonSubterms($("(x,y)"), $("{a,x}"), false));
//        assertFalse(Subterms.commonSubterms($("(x,y)"), $("{a,b}"), false));
//
//        assertFalse(Subterms.commonSubterms($("(#x,y)"), $("{a,#x}"), true));
//        assertTrue(Subterms.commonSubterms($("(#x,a)"), $("{a,$y}"), true));
//    }




}