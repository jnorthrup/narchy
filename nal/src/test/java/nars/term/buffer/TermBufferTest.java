package nars.term.buffer;

import jcog.data.byt.DynBytes;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.TermTransform;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.term.atom.IdempotentBool.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TermBufferTest {

    private static final Term A = INSTANCE.$$("a");
    private static final Term B = INSTANCE.$$("b");
    private static final Term C = INSTANCE.$$("c");

    private static class MyTermBuffer extends TermBuffer {
        @Override
        public Term term(int volMax) {

            Term t = super.term(volMax);
            /* if (Param.DEBUG) */ {
                int volExpected = volMax - volRemain;
                int volActual = t.volume();
                assert (t == Null || volActual == volExpected) : "incorrect volume calculation: actually " + volActual + " but measured " + volExpected;
            }

            return t;
        }
    }

    @Test
    void testSimple() {
        assertEquals("(a,b)", new MyTermBuffer()
                .appendCompound(Op.PROD, A, B).term().toString());
    }
    @Test
    void testNeg() {
        TermBuffer l0 = new MyTermBuffer().appendCompound(Op.PROD, A, B, B);
        TermBuffer l1 = new MyTermBuffer().appendCompound(Op.PROD, A, B, B.neg());

        DynBytes code = l1.code;
        DynBytes code1 = l0.code;
        assertEquals(code1.length()+1, code.length()); //only one additional byte for negation
        assertEquals(l0.sub.termCount(), l1.sub.termCount());
        assertEquals(l0.sub.termToId, l1.sub.termToId);

        assertEquals("(a,b,(--,b))", l1.term().toString());
        assertEquals("((--,a),(--,b))", new MyTermBuffer()
                .appendCompound(Op.PROD, A.neg(), B.neg()).term().toString());
    }
    @Test
    void testTemporal() {
        assertEquals("(a==>b)", new MyTermBuffer()
                .appendCompound(Op.IMPL, A, B).term().toString());

        assertEquals("(a ==>+1 b)", new MyTermBuffer()
                .appendCompound(Op.IMPL, 1, A, B).term().toString());
    }

    static final TermTransform nullTransform = new RecursiveTermTransform() {

    };
    static final TermTransform atomToCompoundTransform = new RecursiveTermTransform() {

        final Term cmp = INSTANCE.$$("(x,y)");

        @Override
        public Term applyAtomic(Atomic atomic) {
            return "_1".equals(atomic.toString()) ? cmp : atomic;
        }
    };

    @Test
    void testTransform1() {
        assertLazyTransforms("((_1) ==>+- (_1))");
    }

    @Test void Transform2() {
        String x = "((_1) ==>+- _1)";
        assertEquals("(((x,y)) ==>+- (x,y))",
                INSTANCE.$$(x).transform(atomToCompoundTransform).toString());

    }

    @Test
    void testCompoundInCompound() {
        assertEquals("(a,{b,c})", new MyTermBuffer()
                .compoundStart(Op.PROD).subsStart((byte)2).append(A)
                    .compoundStart(Op.SETe).subsStart((byte)2).subs(B, C)
                        .term().toString());
    }

    @Test void EmptyProd() {
        assertLazyTransforms("x(intValue,(),3)");
    }
    @Test void AtomFunc() {
        assertLazyTransforms("x(a)");
    }

    private static void assertLazyTransforms(String x) {
        assertEquals(x, INSTANCE.$$(x).transform(nullTransform).toString());
    }


}