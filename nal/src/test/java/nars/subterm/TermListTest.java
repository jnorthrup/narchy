package nars.subterm;

import nars.$;
import nars.term.CompoundLight;
import nars.term.TermTest;
import nars.term.compound.CachedCompound;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

public class TermListTest {

    @ParameterizedTest
    @ValueSource(strings={
        "()",
        "(a)",
        "(a,b)",
        "(a,b,c)",
        "((a),b,c)",
        "((a),(b,c))",
        "((a),{$b,(#a)})",
        "((a),[?c])",
        "((a),{$b,(#a)},[?c],(%d|e),(f&&g),{1,b,()},(h ==>+1 j),--(k-->m),((l &&+1 k) ==>-3 n))",
    })
    public void test1(String i) {
        assertReallyEquals(i);
    }

    static void assertReallyEquals(String s) {
        Subterms immutable = $.$safe(s).subterms();
        TermList mutable = new TermList(immutable);
        assertNotSame(immutable, mutable);
        assertNotEquals(immutable.getClass(), mutable.getClass());
        assertEquals(immutable, mutable);
        assertEquals(mutable, immutable);
        assertEquals(mutable, mutable);
        assertEquals(immutable, immutable);

        if (immutable.subs() > 1) {
            assertNotEquals(mutable.reversed(), mutable);
            assertNotEquals(immutable.reversed(), mutable);
        }

        assertEquals(mutable.reversed(), immutable.reversed());

        assertEquals(immutable.toString(), mutable.toString());
        assertEquals(0, Subterms.compare(mutable, immutable));
        assertEquals(0, Subterms.compare(mutable, mutable));
        assertEquals(0, Subterms.compare(immutable, mutable));
        assertEquals(immutable.hashCode(), mutable.hashCode());
        assertEquals(immutable.hashCodeSubterms(), mutable.hashCodeSubterms());
        assertEquals(immutable.subs(), mutable.subs());
        assertEquals(immutable.volume(), mutable.volume());
        assertEquals(immutable.complexity(), mutable.complexity());
        assertEquals(immutable.structure(), mutable.structure());
        assertEquals(immutable, mutable.the());

        Subterms[] ab = {mutable, immutable};
        for (Subterms a : ab) {
            for (Subterms b : ab) {
                TermTest.assertReallyEquals(CachedCompound.the(PROD, a), new CompoundLight(PROD, b));
                TermTest.assertReallyEquals(CachedCompound.the(SETe, a), new CompoundLight(SETe, b));
                assertNotEquals(CachedCompound.the(PROD, a), new CompoundLight(SETi, b));

            }
        }
    }



}