package nars.op.language;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
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
        assertEquals("a a b", g.toString(Narsese.task("(a --> b).", n)));
        
        assertEquals("(a) and (bbb)", g.toString(Narsese.task("(&&, (a), (bbb)).", n)));
        
    }

    @Test
    public void testSimple2() throws Narsese.NarseseException {
        assertEquals("a same b", g.toString(Narsese.task("(a <-> b).", n)));
    }







}