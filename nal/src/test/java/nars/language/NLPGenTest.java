package nars.language;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.util.NLPGen;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by me on 7/9/16.
 */
@Disabled
public class NLPGenTest {

    final NLPGen g = new NLPGen();

    NAR n = new NARS().get();

    @Test
    public void testSimple1() throws Narsese.NarseseException {
        assertEquals("a a b", g.toString(Narsese.the().task("(a --> b).", n)));
        //assertEquals("a notA b", g.toString(n.task("(--,(a --> b)).")));
        assertEquals("(a) and (bbb)", g.toString(Narsese.the().task("(&&, (a), (bbb)).", n)));
        //assertEquals("(a) or (bbb)", g.toString(n.task("(||, (a), (bbb)).")));
    }

    @Test
    public void testSimple2() throws Narsese.NarseseException {
        assertEquals("a same b", g.toString(Narsese.the().task("(a <-> b).", n)));
    }

//    @Test
//    public void testSimple3() {
//        assertEquals("a isn't b.", g.toString(n.task("(--,(a --> b)).")));
//        assertEquals("(a is b) and (c is d).", g.toString(n.task("((a --> b) && (c --> d)).")));
//    }

}