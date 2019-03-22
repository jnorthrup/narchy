package nars.nar;

import com.google.common.primitives.Longs;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Termed;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 8/7/15.
 */
class NARTest {


    @Test
    @Disabled
    void testMemoryTransplant() throws Narsese.NarseseException {


        NAR nar = NARS.tmp();


        nar.input("<a-->b>.", "<b-->c>.").run(25);

        nar.input("<a-->b>.", "<b-->c>.");
        nar.stop();

        assertTrue(nar.memory.size() > 5);

        int nc;
        assertTrue((nc = nar.memory.size()) > 0);


        NAR nar2 = NARS.tmp();

        assertTrue(nar.time() > 1);


        assertEquals(nc, nar2.memory.size());


    }

    @Test
    void testFluentBasics() throws Exception {
        int frames = 32;
        AtomicInteger cycCount = new AtomicInteger(0);
        StringWriter sw = new StringWriter();

        NAR m = NARS.tmp()
                .input("<a --> b>.", "<b --> c>.")
                .stopIf(() -> false);
        m.onCycle(nn -> cycCount.incrementAndGet());
        m.trace(sw).run(frames);

        NAR n = NARS.tmp()
                .input("<a --> b>.", "<b --> c>.")
                .stopIf(() -> false);
        n.onCycle(nn -> cycCount.incrementAndGet());
        n.trace(sw);

        ;


        assertTrue(sw.toString().length() > 16);
        assertEquals(frames, cycCount.get());


    }

    @Test
    void testBeforeNextFrameOnlyOnce() {
        AtomicInteger b = new AtomicInteger(0);
        NAR n = NARS.shell();

        n.runLater(b::incrementAndGet);
        n.run(4);
        assertEquals(1, b.get());

    }

    @Test
    void testConceptInstancing() throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        String statement1 = "<a --> b>.";

        Termed a = $.$("a");
        assertNotNull(a);
        Termed a1 = $.$("a");
        assertEquals(a, a1);

        n.input(statement1);
        n.run(4);

        n.input(" <a  --> b>.  ");
        n.run(1);
        n.input(" <a--> b>.  ");
        n.run(1);

        String statement2 = "<a --> c>.";
        n.input(statement2);
        n.run(4);

        Termed a2 = $.$("a");
        assertNotNull(a2);

        Concept ca = n.conceptualize(a2);
        assertNotNull(ca);


    }

    @Test
    void testCycleScheduling() {
        NAR n = NARS.tmp();

        final int[] runs = {0};

        long[] events = {2, 4, 4 /* test repeat */};
        for (long w : events) {
            n.runAt(w, () -> {
                assertEquals(w, n.time());
                runs[0]++;
            });
        }

        n.run(1);
        assertEquals(0, runs[0]); /* nothing yet in that 1st cycle */


        n.run((int) Longs.max(events));
        assertEquals(events.length, runs[0]);
    }

}