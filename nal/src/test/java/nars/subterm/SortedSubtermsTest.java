package nars.subterm;

import nars.term.Term;
import nars.term.TermTest;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SortedSubtermsTest {

    static final Term x = $$("x");
    static final Term y = $$("y");
    static final Term z = $$("z");

    @Test void test2ary() {
        assertEq(new Term[]{ $$("x"), $$("(y --> (z,/))") });
    }
    @Test
    void test3ary() {
        Subterms direct = assertEq(new Term[]{x, y, z}, new Term[]{x, y, z});
        assertTrue(!(direct instanceof MappedSubterms));

        Subterms remapped = assertEq(new Term[]{y, x, z}, new Term[]{y, x, z}); //non-canonical, mapped order
        assertTrue(remapped instanceof MappedSubterms);

        Subterms remapped2 = assertEq(new Term[]{x, x, z}, new Term[]{x, x, z}); //repeats
        assertTrue(!(remapped2 instanceof MappedSubterms));

        Subterms remapped3 = assertEq(new Term[]{x, z, x}, new Term[]{x, z, x}); //repeats, unordered
        assertTrue(remapped3 instanceof MappedSubterms);

    }

    public static Subterms assertEq(Term... x) {
        return assertEq(x, x);
    }

    /** subterms as array/vector */
    public static Subterms assertEq(Term[] aa, Term[] bb) {
        ArrayTermVector a = new ArrayTermVector(aa);
        Subterms b = SortedSubterms.the(bb);
        TermTest.assertEq(a, b);
        return b;
    }
}
