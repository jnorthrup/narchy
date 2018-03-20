package nars.index.term.map;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

class CaffeineIndexTest {

    @Test
    public void testDynamicWeight() throws Narsese.NarseseException {
        StringBuilder log = new StringBuilder();
        CaffeineIndex index;
        NAR n = new NARS().index(
            index = new CaffeineIndex(4000, (w) -> {
                log.append("weigh ").append(w).append('\n');
                return Math.round(1000 * w.tasklinks().priSum());
            })).get();


        n.believe("(x-->y).");

        n.believe("(x-->y).");

        System.out.println(log);

    }
}