package nars.term.util.conj;

import nars.Op;
import nars.term.Term;
import nars.term.compound.Sequence;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.CONJ;
import static nars.term.atom.Bool.False;
import static org.junit.jupiter.api.Assertions.*;

class SequenceTest {

    public static final Term A = $$("a");
    public static final Term Z = $$("z");
    public static final Term X = $$("x");
    public static final Term Y = $$("y");

    @Test void testIntervalOp() {
        assertFalse(Op.INTERVAL.taskable);
        assertFalse(Op.INTERVAL.conceptualizable);
        assertFalse(Op.INTERVAL.eventable);
        assertTrue(Op.INTERVAL.atomic);
    }

    @Test void test1() {
        ConjList l = new ConjList();
        l.add(1L, $$("x"));
        l.add(2L, $$("y"));
        l.add(2L, $$("z"));
        l.add(4L, $$("x"));
        //assertEquals("((x &&+1 (y&&z)) &&+2 x)", l.term().toString());
        Sequence s = ConjSeq.sequenceFlat(l);

        assertEquals(3, s.eventRange());

        {
            StringBuilder ee = new StringBuilder();
            s.eventsAND((when, what) -> {
                ee.append(when).append(what).append(' ');
                return true;
            }, 0, true, true);
            assertEquals("0x 1y 1z 3x ", ee.toString());
        }
        {
            StringBuilder ee = new StringBuilder();
            s.eventsAND((when, what) -> {
                ee.append(when).append(what).append(' ');
                return true;
            }, 0, false, true);
            assertEquals("0x 1(y&&z) 3x ", ee.toString());
        }

        assertEquals("x", s.eventFirst().toString());
        assertEquals("x", s.eventLast().toString());
        assertEquals("", s.eventSet().toString());
        assertEquals("(&/,x,+1,(y&&z),+2,x)", s.toString());

    }
    @Test void testTransform() {
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
            assertTrue(wrapped.op() == CONJ, () -> x + " -> " + wrapped);
        }


//        assertEquals("", y.toString());



    }

}