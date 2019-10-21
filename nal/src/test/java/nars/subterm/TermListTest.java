package nars.subterm;

import nars.Narsese;
import nars.Op;
import nars.term.Term;
import nars.term.compound.LightCompound;
import nars.term.util.TermTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Supplier;

import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.fail;

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
        assertEq(i);
    }

    private static void assertEq(String s) {
        Subterms immutable = null;
        try {
            immutable = Narsese.term(s, false).subterms();
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
            fail(new Supplier<String>() {
                @Override
                public String get() {
                    return s + " -> " + e;
                }
            });
        }
        TermList mutable = new TermList(immutable);

        TermTest.assertEq(mutable, immutable);

        Subterms[] ab = {mutable, immutable};
        for (Subterms a : ab) {
            for (Subterms b : ab) {
                TermTest.assertEq((Term) Op.terms.newCompound(PROD, a), new LightCompound(PROD, b));
//                TermTest.assertEq(
//                        (Term) Op.terms.newCompound(PROD, a), new LighterCompound(PROD, b.arrayShared()));

                //                if (a.subs() > 0) {
//                    TermTest.assertEq((Term) Op.terms.newCompound(SETe, as), new LightCompound(SETe, bs));
//                    assertNotEquals(Op.terms.newCompound(PROD, a), new LightCompound(SETi, bs));
//                }

            }
        }
    }



}