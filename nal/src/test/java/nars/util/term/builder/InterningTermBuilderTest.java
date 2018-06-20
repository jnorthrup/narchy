package nars.util.term.builder;

import com.google.common.collect.Iterators;
import jcog.io.Huffman;
import jcog.pri.PriProxy;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.term.HijackTermCache;
import nars.util.term.InternedCompound;
import org.junit.jupiter.api.Test;

import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InterningTermBuilderTest {

    final static Term a = Atomic.the("a"), b = Atomic.the("b");

    @Test
    public void test1() {
        InterningTermBuilder t = new InterningTermBuilder();
        Term pab = t.compound(PROD, a, b);
        assertEquals( "(a,b)", pab.toString());

        HijackTermCache prodCache = t.termCache[PROD.id];

        PriProxy<InternedCompound, Term> pabEntry = Iterators.get(prodCache.iterator(), 0);
        assertEquals(pab, pabEntry.get());
        Term pabSame = t.compound(PROD, a, b);
        assertSame(pab, pabSame);

        Term paab = t.compound(PROD, a, t.compound(PROD, a, b));
        prodCache.print();

        assertSame(pab, paab.sub(1));

        Huffman h = prodCache.buildCodec();

    }
}