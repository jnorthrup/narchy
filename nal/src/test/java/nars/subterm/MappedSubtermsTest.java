package nars.subterm;

import nars.Op;
import nars.term.Term;
import nars.term.util.TermTest;
import nars.term.var.CommonVariable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.$.$$$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MappedSubtermsTest {

    static final Term x = $$("x");
    static final Term y = $$("y");
    static final Term z = $$("z");

    @Test void test2ary() {
        assertEq($$("x"), $$("(y --> (z,/))"));
    }
    @Test
    void test3ary() {
        Subterms direct = assertEq(new Term[]{x, y, z}, new Term[]{x, y, z});
        assertTrue(!(direct instanceof RemappedSubterms));

        Subterms remapped = assertEq(new Term[]{y, x, z}, new Term[]{y, x, z}); //non-canonical, mapped order
        assertTrue(remapped instanceof RemappedSubterms);

        Subterms remapped2 = assertEq(new Term[]{x, x, z}, new Term[]{x, x, z}); //repeats
        assertTrue(!(remapped2 instanceof RemappedSubterms));

        Subterms remapped3 = assertEq(new Term[]{x, z, x}, new Term[]{x, z, x}); //repeats, unordered
        assertTrue(remapped3 instanceof RemappedSubterms);

    }
    @Test
    void test3aryNeg() {
        Subterms remapped3Neg = assertEq(
                new Term[]{x.neg(), z, x}, new Term[]{x.neg(), z, x}); //repeats, unordered
        assertTrue(remapped3Neg instanceof RemappedSubterms);
    }

    protected static Subterms assertEq(Term... x) {
        return assertEq(x, x);
    }

    /** subterms as array/vector */
    public static Subterms assertEq(Term[] aa, Term[] bb) {
        ArrayTermVector a = new ArrayTermVector(aa);
        Subterms b = SortedSubterms.the(bb, Op.terms::subterms);
        TermTest.assertEq(a, b);
        return b;
    }

    @Test void testRepeatedSubterms() {
        {
            Term s = $$("(abc,abc,abc,abc)");
            assertEquals(RemappedSubterms.RepeatedSubterms.class, s.subterms().getClass());
            assertEquals(4, s.subs());
            assertEquals(5, s.volume());
            assertEquals("(abc,abc,abc,abc)", s.toString());
        }
        {
            Subterms s = new RemappedSubterms.RepeatedSubterms($$$("#a"), 3);
            assertEquals(RemappedSubterms.RepeatedSubterms.class, s.getClass());
            assertEquals(3, s.subs());
            assertEquals(3, s.vars());
            assertEquals(3, s.varDep());
            assertTrue(s.hasVars());
            assertTrue(s.hasVarDep());
            assertEquals(1 + 3, s.volume());
            assertEquals(1, s.complexity());
            assertEquals("(#a,#a,#a)", s.toString());
        }
    }
    @Test void testBiSubtermWeird() {
        Term allegedTarget = $$$("( &&+- ,(--,##2#4),_1,##2#4,$1)");
        Subterms base = new BiSubterm(allegedTarget, $$$("$1"));
        assertEquals("(( &&+- ,(--,##2#4),_1,$1,##2#4),$1)", base.toString());

        Term target = $$$("( &&+- ,(--,##2#4),_1,##2#4,$1)");
        assertEquals(0, base.indexOf(target));

        /*
        ( &&+- ,(--,($1-->_1)),(--,##2#5),##2#5) (&&[TemporalCachedCompound] c5,v8,dt=2147483647,dtRange=0 110000010000111)
          (--,($1-->_1)) (--[Neg] c3,v4,dt=-2147483648,dtRange=0 100000000000111)
            ($1-->_1) (-->[SimpleCachedCompound] c2,v3,dt=-2147483648,dtRange=0 100000000000101)
              $1 ($[VarIndep] c0,v1,dt=-2147483648,dtRange=0 100000000000000)
              _1 (.[Anom] c1,v1,dt=-2147483648,dtRange=0 1)
          (--,##2#5) (--[Neg] c1,v2,dt=-2147483648,dtRange=0 10000000000010)
            ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)
          ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)

        ( &&+- ,(--,($1-->_1)),(--,##2#5),##2#5) (&&[TemporalCachedCompound] c5,v8,dt=2147483647,dtRange=0 110000010000111)
          (--,($1-->_1)) (--[Neg] c3,v4,dt=-2147483648,dtRange=0 100000000000111)
            ($1-->_1) (-->[SimpleCachedCompound] c2,v3,dt=-2147483648,dtRange=0 100000000000101)
              $1 ($[VarIndep] c0,v1,dt=-2147483648,dtRange=0 100000000000000)
              _1 (.[Anom] c1,v1,dt=-2147483648,dtRange=0 1)
          (--,##2#5) (--[Neg] c1,v2,dt=-2147483648,dtRange=0 10000000000010)
            ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)
          ##2#5 (#[CommonVariable] c0,v1,dt=-2147483648,dtRange=0 10000000000000)
     */
        assertTrue($$("##2#5") instanceof CommonVariable);
        assertTrue($$("##2#5").equals($$("##2#5")));
        assertTrue($$("##2#5").neg().equals($$("##2#5").neg()));
        assertTrue($$("--##2#5").unneg().equals($$("--##2#5").unneg()));
    }
}
