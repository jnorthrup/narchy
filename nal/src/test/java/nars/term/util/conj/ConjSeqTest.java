package nars.term.util.conj;

import nars.term.Term;
import nars.term.compound.Sequence;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;

class ConjSeqTest {

    public static final Term Z = $$("z");
    public static final Term X = $$("x");
    public static final Term Y = $$("y");

    @Test void test1() {
        ConjList l = new ConjList();
        l.add(1L, $$("x"));
        l.add(2L, $$("y"));
        l.add(2L, $$("z"));
        l.add(4L, $$("x"));
        assertEquals("((x &&+1 (y&&z)) &&+2 x)", l.term().toString());
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
        assertEquals("((x &&+1 (y&&z)) &&+2 x)", l.term().toString());
        Sequence x = ConjSeq.sequenceFlat(l);
        Term y = x.replace(Z,X);
        assertNotEquals(x, y);
        assertTrue(y instanceof Sequence);
        assertEquals("", y.toString());


    }

}