package nars.derive;

import nars.Narsese;
import nars.derive.premise.PatternIndex;
import nars.term.Compound;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PremisePatternCompoundTest {

    private final PatternIndex i = new PatternIndex();

    @Test
    void testPatternCompoundWithXTERNAL() throws Narsese.NarseseException {
        Compound p = (Compound) i.get($("((x) ==>+- (y))"), true).term();
        assertEquals(XTERNAL, p.dt());

    }

    @Test public void testPatternTermConjHasXTERNAL() {
        Term p = PatternIndex.patternify($$("(x && y)"));
        assertEquals("(x &&+- y)", p.toString());

        Term r = PatternIndex.patternify($$("(x || y)"));
        assertEquals(
                //"(--,((--,x) &&+- (--,y)))",
                "(||+- ,x,y)",
                //"(x ||+- y)", //TODO
                r.toString());
    }


}