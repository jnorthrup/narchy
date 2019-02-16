package nars.term.compound;

import jcog.data.byt.DynBytes;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.transform.TermTransform;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LazyCompoundTest {

    private static final Term A = $$("a"), B = $$("b"), C = $$("c");

    @Test
    void testSimple() {
        assertEquals("(a,b)", new LazyCompound()
                .compound(Op.PROD, A, B).get().toString());
    }
    @Test
    void testNeg() {
        LazyCompound l0 = new LazyCompound().compound(Op.PROD, A, B, B);
        LazyCompound l1 = new LazyCompound().compound(Op.PROD, A, B, B.neg());

        DynBytes code = l1.code;
        DynBytes code1 = l0.code;
        assertEquals(code1.length()+1, code.length()); //only one additional byte for negation
        assertEquals(l0.sub.termCount(), l1.sub.termCount());
        assertEquals(l0.sub.termToId, l1.sub.termToId);

        assertEquals("(a,b,(--,b))", l1.get().toString());
        assertEquals("((--,a),(--,b))", new LazyCompound()
                .compound(Op.PROD, A.neg(), B.neg()).get().toString());
    }
    @Test
    void testTemporal() {
        assertEquals("(a==>b)", new LazyCompound()
                .compound(Op.IMPL, A, B).get().toString());

        assertEquals("(a ==>+1 b)", new LazyCompound()
                .compound(Op.IMPL, 1, A, B).get().toString());
    }

    static final TermTransform nullTransform = new TermTransform() {

    };
    static final TermTransform atomToCompoundTransform = new TermTransform() {

        final Term cmp = $$("(x,y)");

        @Override
        public Term transformAtomic(Atomic atomic) {
            return atomic.toString().equals("_1") ? cmp : atomic;
        }
    };

    @Test
    void testTransform1() {
        String x = "((_1) ==>+- (_1))";
        assertEquals(x, nullTransform.transformCompoundLazily($$(x)).toString());
    }

    @Test void testTransform2() {
        String x = "((_1) ==>+- _1)";
        assertEquals("(((x,y)) ==>+- (x,y))",
                atomToCompoundTransform.transformCompoundLazily($$(x)).toString());
    }

    @Test
    void testCompoundInCompound() {
        assertEquals("(a,{b,c})", new LazyCompound()
                .compoundStart(Op.PROD).subsStart((byte)2).append(A)
                    .compoundStart(Op.SETe).subsStart((byte)2).subs(B, C)
                        .get().toString());
    }

    @Test void testEmptyProd() {
        String x = "x(intValue,(),3)";
        assertEquals(x, nullTransform.transformCompoundLazily($$(x)).toString());

    }
}