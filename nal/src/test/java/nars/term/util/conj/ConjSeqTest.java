package nars.term.util.conj;

import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConjSeqTest {

    @Test void test1() {
        ConjList l = new ConjList();
        l.add(1L, $$("x"));
        l.add(2L, $$("y"));
        l.add(2L, $$("z"));
        l.add(4L, $$("x"));
        assertEquals("((x &&+1 (y&&z)) &&+2 x)", l.term().toString());
        ConjSeq.ConjSequence s = ConjSeq.the(l);

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

}