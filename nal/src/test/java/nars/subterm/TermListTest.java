package nars.subterm;

import nars.$;
import nars.Op;
import nars.term.TermTest;
import nars.term.compound.LightCompound;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

class TermListTest {

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
    void test1(String i) {
        assertReallyEquals(i);
    }

    private static void assertReallyEquals(String s) {
        Subterms immutable = $.$$(s).subterms();
        TermList mutable = new TermList(immutable);

        SubtermsTest.assertEquals(mutable, immutable);

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
        assertEquals(immutable.volume(), mutable.volume(), ()->immutable + " " + immutable.volume() + " " + mutable + " " + mutable.volume());
        assertEquals(immutable.complexity(), mutable.complexity());
        assertEquals(immutable.structure(), mutable.structure());

        Subterms[] ab = {mutable, immutable};
        for (Subterms a : ab) {
            for (Subterms b : ab) {
                TermTest.assertReallyEquals(Op.terms.theCompound(PROD, a), new LightCompound(PROD, b));
                TermTest.assertReallyEquals(Op.terms.theCompound(SETe, a), new LightCompound(SETe, b));
                assertNotEquals(Op.terms.theCompound(PROD, a), new LightCompound(SETi, b));

            }
        }
    }



}