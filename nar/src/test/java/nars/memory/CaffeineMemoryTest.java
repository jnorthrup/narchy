package nars.memory;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

class CaffeineMemoryTest {

    @Test
    void testDynamicWeight() throws Narsese.NarseseException {
        StringBuilder log = new StringBuilder();
        CaffeineMemory index;
        NAR n = new NARS().index(
            index = new CaffeineMemory(4000, new ToIntFunction<Concept>() {
                @Override
                public int applyAsInt(Concept w) {
                    int newWeight = Math.round(1000 * (w.beliefs().taskCount()));
                    log.append("weigh ").append(w).append(' ').append(newWeight).append('\n');


                    return newWeight;
                }
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