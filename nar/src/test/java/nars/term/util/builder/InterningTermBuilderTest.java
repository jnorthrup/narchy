package nars.term.util.builder;

import nars.Op;
import nars.io.IO;
import nars.subterm.RemappedSubterms;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.LightCompound;
import nars.term.compound.LightDTCompound;
import nars.term.util.cache.Intermed;
import org.junit.jupiter.api.Test;

import static jcog.data.byt.RecycledDynBytes.tmpKey;
import static nars.$.$$;
import static nars.Op.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

class InterningTermBuilderTest {

    final static Term a = Atomic.the("a"), b = Atomic.the("b");

    @Test
    public void test1() {
        InterningTermBuilder t = new InterningTermBuilder();
        Term pab = t.compound(PROD, a, b);
        assertEquals( "(a,b)", pab.toString());

//        Function<Intermed.InternedCompoundByComponents, Term> prodCache = (HijackMemoize<Intermed.InternedCompoundByComponents, Term>) t.terms[PROD.id];

//        PriProxy<Intermed.InternedCompoundByComponents, Term> pabEntry = Iterators.get(prodCache.iterator(), 0);
//        assertEquals(pab, pabEntry.get());
        Term pabSame = t.compound(PROD, a, b);
        assertSame(pab, pabSame);

        Term paab = t.compound(PROD, a, t.compound(PROD, a, b));
//        prodCache.print();

        assertSame(pab, paab.sub(1));

        //Huffman h = prodCache.buildCodec();

    }

    @Test void testMappedNegBiSubterms() {

        InterningTermBuilder t = new InterningTermBuilder();
        assertTrue(InterningTermBuilder.sortCanonically);
        Subterms s = t.subterms((Op)null, $$("x").neg(), $$("y"));
        assertTrue(s instanceof RemappedSubterms);
        assertEquals("((--,x),y)", s.toString());
    }

    @Test public void testImplicationComplexEndToEnd() {
        //InterningTermBuilder t = new InterningTermBuilder();
        if (Op.terms instanceof InterningTermBuilder) {

            InterningTermBuilder i = (InterningTermBuilder) Op.terms;
//        System.out.println("impl/conj:");
//        i.terms[Op.IMPL.id].print();
//        i.terms[CONJ.id].print();
            IMPL.the(a, CONJ.the(b.neg(), CONJ.the(a, 1, CONJ.the(b.neg(), b)).neg()));
//        System.out.println("impl/conj:");
//        i.terms[Op.IMPL.id].print();
//        i.terms[CONJ.id].print();
        }
    }

    @Test void testKeyConstructionEquivalence() {
        byte[] a = new Intermed.InternedCompoundByComponentsArray(CONJ, 1, this.a.neg(), this.b).key.arrayCopy();
        tmpKey().clear();
        byte[] b = new Intermed.InternedCompoundTransform(new LightDTCompound( new LightCompound(CONJ, this.a.neg(), this.b), 1)).key.arrayCopy();
        tmpKey().clear();
        assertArrayEquals(a, b);
        assertEq(IO.bytesToTerm(a),IO.bytesToTerm(b));
        assertEquals("((--,a) &&+1 b)", IO.bytesToTerm(a).toString());
    }
}