package nars.term.util.conj;

import nars.Op;
import nars.term.Term;
import nars.term.compound.Sequence;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.term.atom.IdempotentBool.False;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
class SequenceTest {

    public static final Term A = INSTANCE.$$("a");
    public static final Term Z = INSTANCE.$$("z");
    public static final Term X = INSTANCE.$$("x");
    public static final Term Y = INSTANCE.$$("y");

    @Test void IntervalOp() {
        assertFalse(Op.INTERVAL.taskable);
        assertFalse(Op.INTERVAL.conceptualizable);
        assertFalse(Op.INTERVAL.eventable);
        assertTrue(Op.INTERVAL.atomic);
    }

    @Test void one() {
        ConjList l = new ConjList();
        l.add(1L, INSTANCE.$$("x"));
        l.add(2L, INSTANCE.$$("y"));
        l.add(2L, INSTANCE.$$("z"));
        l.add(4L, INSTANCE.$$("x"));
        //assertEquals("((x &&+1 (y&&z)) &&+2 x)", l.term().toString());
        Sequence s = ConjSeq.sequenceFlat(l);

        assertEquals(3, s.eventRange());

        {
            StringBuilder ee = new StringBuilder();
            s.eventsAND(new LongObjectPredicate<Term>() {
                @Override
                public boolean accept(long when, Term what) {
                    ee.append(when).append(what).append(' ');
                    return true;
                }
            }, 0, true, true);
            assertEquals("0x 1y 1z 3x ", ee.toString());
        }
        {
            StringBuilder ee = new StringBuilder();
            s.eventsAND(new LongObjectPredicate<Term>() {
                @Override
                public boolean accept(long when, Term what) {
                    ee.append(when).append(what).append(' ');
                    return true;
                }
            }, 0, false, true);
            assertEquals("0x 1(y&&z) 3x ", ee.toString());
        }

        assertEquals("x", s.eventFirst().toString());
        assertEquals("x", s.eventLast().toString());
        assertEquals("", s.eventSet().toString());
        assertEquals("(&/,x,+1,(y&&z),+2,x)", s.toString());

    }
    @Test void Transform() {
        ConjList l = new ConjList();
        l.add(1L, X);
        l.add(2L, Y);
        l.add(2L, Z);
        l.add(4L, X);
//        assertEquals("((x &&+1 (y&&z)) &&+2 x)", l.term().toString());
        Sequence x = ConjSeq.sequenceFlat(l);
        {
            Term contradicted = CONJ.the(X.neg(),x);
            assertEquals(False, contradicted);
        }
        {
            Term y = x.replace(Z, X);
            assertNotEquals(x, y);
            assertTrue(y instanceof Sequence);
        }

        {
            assertTrue(x.anon() instanceof Sequence);
        }

        {
            Term wrapped = CONJ.the(A, x);
            assertTrue(wrapped.op() == CONJ, new Supplier<String>() {
                @Override
                public String get() {
                    return x + " -> " + wrapped;
                }
            });
        }


//        assertEquals("", y.toString());



    }

}