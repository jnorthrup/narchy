package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;


public class FastCompoundTest {

    @Test public void testVar() throws Narsese.NarseseException {
        assertEquivalent("(?1-->x)");
        assertEquivalent("(?x-->x)");
        assertEquivalent("(x-->?1)");
        assertEquivalent("(x-->?x)");
    }

    @Test
    public void test1() throws Narsese.NarseseException {
        assertEquivalent("(((x)))");
        assertEquivalent("((x))");
    }
    @Test
    public void test2() throws Narsese.NarseseException {

        assertEquivalent("(P-->S)");
        assertEquivalent("(((P-->S)))");
        assertEquivalent("((P-->S))");
        assertEquivalent("(x,y)");
        assertEquivalent("(x,(P-->S))");
    }
    @Test
    public void test2b() throws Narsese.NarseseException {
        assertEquivalent("((P-->S),x)");

        assertEquivalent("(((P-->S)),x)");
        assertEquivalent("task(\"?\")");
        assertEquivalent("((P-->S),(S-->P),task(x))");
        assertEquivalent("(((Conversion-->Belief)),(P-->S))");
        assertEquivalent("(((Conversion-->Belief),(Belief-->Punctuation)),(P-->S))");
        assertEquivalent("((P-->S),((Conversion-->Belief),(Belief-->Punctuation)))");
    }

    @Test
    public void testComplex() throws Narsese.NarseseException {
        assertEquivalent("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");
    }

    @Test
    public void test3() throws Narsese.NarseseException {
        assertEquivalent("(((P-->S),(S-->P),task(\"?\")),((P-->S),((Conversion-->Belief),(Belief-->Punctuation))))");
    }

    static void assertEquivalent(String c) throws Narsese.NarseseException {
        assertEquivalent($(c));
    }

    static void assertEquivalent(Compound c) {
        FastCompound f = FastCompound.get(c);
        assertTrue(f!=c,
                ()->"identical, nothing is being tested");

        assertEquals(c.op(), f.op());
        assertEquals(c.subs(), f.subs());
        int s = f.subterms().subs();
        assertEquals(c.subterms().subs(), s);

        assertEquals(c.hashCode(), f.hashCode());

        for (int i = 0; i < s; i++) {
            Term ci = c.subterms().sub(i);
            Term fi = f.subterms().sub(i);
            assertEquals(ci, fi);
            assertEquals(fi, ci);
            assertEquals(fi.subterms(), ci.subterms());
            assertEquals(ci.subterms(), fi.subterms());
            assertEquals(fi.hashCode(), ci.hashCode());
            assertEquals(-fi.compareTo(ci),
                         ci.compareTo(fi) );
        }

        assertArrayEquals(c.subterms().arrayShared(), f.subterms().arrayShared());
        assertEquals(c.subterms().hashCodeSubterms(), f.subterms().hashCodeSubterms());
        assertEquals(c.subterms().hashCode(), f.subterms().hashCode());

        assertEquals(c.structure(), f.structure());
        assertEquals(c.complexity(), f.complexity());
        assertEquals(c.volume(), f.volume());
        assertEquals(c.toString(), f.toString());
        assertEquals(c, c);
        assertEquals(f, f);
        assertEquals(c, f);
        assertEquals(f, c);
        assertEquals(0, f.compareTo(c));
        assertEquals(0, c.compareTo(f));

//        f.print();
    }

}