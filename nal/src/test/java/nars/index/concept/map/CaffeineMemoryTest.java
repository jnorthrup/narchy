package nars.index.concept.map;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.index.concept.CaffeineMemory;
import nars.term.Term;
import org.junit.jupiter.api.Test;

class CaffeineMemoryTest {

    @Test
    void testDynamicWeight() throws Narsese.NarseseException {
        StringBuilder log = new StringBuilder();
        CaffeineMemory index;
        NAR n = new NARS().index(
            index = new CaffeineMemory(4000, (w) -> {
                int newWeight = Math.round(1000 * (w.beliefs().taskCount()));
                log.append("weigh ").append(w).append(' ').append(newWeight).append('\n');






                return newWeight;
            }) {
                @Override
                public Concept get(Term x, boolean createIfMissing) {
                    log.append("get ").append(x).append(createIfMissing ? " createIfMissing\n" : "\n");
                    return super.get(x, createIfMissing);
                }
            }).get();


        n.believe("(x-->y).");

        n.believe("(x-->y).");

        System.out.println(log);

    }
}