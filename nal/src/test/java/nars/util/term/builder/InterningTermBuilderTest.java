package nars.util.term.builder;

import com.google.common.collect.Iterators;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.term.InternedCompound;
import org.junit.jupiter.api.Test;

import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InterningTermBuilderTest {

    final static Term a = Atomic.the("a"), b = Atomic.the("b");

    @Test
    public void test1() {
        InterningTermBuilder t = new InterningTermBuilder();
        Term pab = t.compound(PROD, a, b);
        assertEquals( "(a,b)", pab.toString());
        InternedCompound pabEntry = (InternedCompound) Iterators.get(t.termCache[PROD.id].iterator(), 0);
        assertEquals(pab, pabEntry.get());
        assertEquals(null, pabEntry.rawSubs);
    }
}