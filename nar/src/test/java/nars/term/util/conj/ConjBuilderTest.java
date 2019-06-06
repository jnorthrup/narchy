package nars.term.util.conj;

import jcog.WTF;
import org.junit.jupiter.api.Test;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConjBuilderTest {


    @Test
    void testSimpleEternalsNeg() {
        ConjBuilder c = new ConjTree();
        c.add(ETERNAL, ConjTest2.x);
        c.add(ETERNAL, ConjTest2.y.neg());
        assertEquals("((--,y)&&x)", c.term().toString());
    }

    @Test
    void testSimpleEvents() {
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjLazy()}) {
            c.add(1, ConjTest2.x);
            c.add(2, ConjTest2.y);
            assertEquals("(x &&+1 y)", c.term().toString());
            assertEquals(1, c.shift());
            assertEquals(1, c.shiftOrZero());

//            if (c instanceof ConjBuilder)
//                assertEquals(2, ((Conj) c).event.size());
        }
    }

    @Test
    void testEventShiftEternal() {
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjLazy()}) {
            c.add(ETERNAL, ConjTest2.x);
            c.add(1, ConjTest2.y);
            assertEquals(1, c.shift());
        }
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjLazy()}) {
            c.add(ETERNAL, ConjTest2.x);
            c.add(ETERNAL, ConjTest2.y);
            assertEquals(ETERNAL, c.shift());
            assertEquals(0, c.shiftOrZero());
        }
    }

    @Test
    void testConjBuilder_DontAccept_TIMELESS() {
        for (ConjBuilder c : new ConjBuilder[]{new ConjTree(), new ConjLazy()}) {
            assertThrows(WTF.class, ()->c.add(TIMELESS, ConjTest2.x), ()->c.getClass().getSimpleName());
        }
    }
}
