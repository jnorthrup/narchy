package nars.task.util;

import jcog.util.ArrayUtils;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnswerTest {

    @Test
    void testMatchPartialXternal() throws Narsese.NarseseException {
        beliefQuery("((x &&+1 y) =|> z)",
                new String[]{"((x &&+- y) =|> z)", "((x &&+- y) ==> z)", "((x &&+- y) ==>+- z)", "((x && y) =|> z)"});
    }
    @Test
    void testMatchPartialXternalDifferentVolume() throws Narsese.NarseseException {
        beliefQuery("((x &&+1 (x &&+1 y)) =|> z)",
                new String[]{"((x &&+- y) =|> z)"});
    }

    static void beliefQuery(String belief, String[] queries) throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe(belief);
        queries = ArrayUtils.add(queries, belief);
        for (String q : queries) {
            @Nullable Task a = n.answer($$(q), BELIEF, ETERNAL);
            assertNotNull(a, ()->q + " did not match " + belief);
            assertEquals($$(belief), a.term());
        }
    }
}