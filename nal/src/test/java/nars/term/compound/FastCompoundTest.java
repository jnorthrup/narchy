package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.TermTest;
import org.junit.jupiter.api.Test;

import static nars.$.*;


class FastCompoundTest {

    @Test
    void testVar() throws Narsese.NarseseException {
        assertEquivalent("(?1-->x)");
        assertEquivalent("(?x-->x)");
        assertEquivalent("(x-->?1)");
        assertEquivalent("(x-->?x)");
    }

    @Test
    void test1() throws Narsese.NarseseException {
        assertEquivalent("(((x)))");
        assertEquivalent("((x))");
    }
    @Test
    void test2() throws Narsese.NarseseException {

        assertEquivalent("(P-->S)");
        assertEquivalent("(((P-->S)))");
        assertEquivalent("((P-->S))");
        assertEquivalent("(x,y)");
        assertEquivalent("(x,(P-->S))");
    }
    @Test
    void test2b() throws Narsese.NarseseException {
        assertEquivalent("((P-->S),x)");

        assertEquivalent("(((P-->S)),x)");
        assertEquivalent("task(\"?\")");
        assertEquivalent("((P-->S),(S-->P),task(x))");
        assertEquivalent("(((Conversion-->Belief)),(P-->S))");
        assertEquivalent("(((Conversion-->Belief),(Belief-->Punctuation)),(P-->S))");
        assertEquivalent("((P-->S),((Conversion-->Belief),(Belief-->Punctuation)))");
    }

    @Test
    void testComplex() throws Narsese.NarseseException {
        assertEquivalent("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");
    }

    @Test
    void test3() throws Narsese.NarseseException {
        assertEquivalent("(((P-->S),(S-->P),task(\"?\")),((P-->S),((Conversion-->Belief),(Belief-->Punctuation))))");
    }

    private static void assertEquivalent(String c) throws Narsese.NarseseException {
        assertEquivalent(INSTANCE.$(c));
    }

    private static void assertEquivalent(Compound c) {
        FastCompound f = FastCompound.get(c);
        TermTest.assertEq((Term) c, f);


    }

}