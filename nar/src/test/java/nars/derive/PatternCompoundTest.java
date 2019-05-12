package nars.derive;

import nars.Narsese;
import nars.derive.premise.PatternTermBuilder;
import nars.term.Compound;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternCompoundTest {


    @Test
    void testPatternCompoundWithXTERNAL() throws Narsese.NarseseException {
        Compound p = (Compound) PatternTermBuilder.patternify($("((x) ==>+- (y))")).term();
        assertEquals(XTERNAL, p.dt());

    }

    @Test public void testPatternTermConjHasXTERNAL() {
        Term p = PatternTermBuilder.patternify($$("(x && y)"));
        assertEquals("(x &&+- y)", p.toString());

        Term r = PatternTermBuilder.patternify($$("(x || y)"));
        assertEquals(
                //"(--,((--,x) &&+- (--,y)))",
                "(x ||+- y)",
                //"(x ||+- y)", //TODO
                r.toString());
    }


}