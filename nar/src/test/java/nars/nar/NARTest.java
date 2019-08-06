package nars.nar;

import com.google.common.primitives.Longs;
import nars.*;
import nars.concept.Concept;
import nars.term.Termed;
import nars.term.util.TermTest;
import nars.test.TestNAR;
import nars.util.RuleTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;
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
        m.trace(sw);
        m.run(frames);

        NAR n = NARS.tmp()
                .input("<a --> b>.", "<b --> c>.")
                .stopIf(() -> false);
        n.onCycle(nn -> cycCount.incrementAndGet());
        n.trace(sw);

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

    @ParameterizedTest
    @ValueSource(strings = {
            "a:b. b:c.",
            "a:b. b:c. c:d! a@",
            "d(x,c). :|: (x<->c)?",
            "((x &&+1 b) &&+1 c). :|: (c && --b)!"
    })
    void testNARTaskSaveAndReload(String input) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);

        final AtomicInteger count = new AtomicInteger();


        Set<Task> written = new HashSet();

        NAR a = NARS.tmp()

                .input(new String[]{input});
        a
                .run(16);
        a
                .synch()
                .outputBinary(baos, (Task t) -> {
                    assertTrue(written.add(t), () -> "duplicate: " + t);
                    count.incrementAndGet();
                    return true;
                })

        ;

        byte[] x = baos.toByteArray();
        out.println(count.get() + " tasks serialized in " + x.length + " bytes");

        NAR b = NARS.shell()
                .inputBinary(new ByteArrayInputStream(x));


        Set<Task> aHas = new HashSet();

        a.tasks().forEach((Task t) -> aHas.add(t));

        assertEquals(count.get(), aHas.size());

        assertEquals(written, aHas);

        Set<Task> bRead = new HashSet();

        b.tasks().forEach(t -> bRead.add(t));

        assertEquals(aHas, bRead);


    }


    @Test
    void testA() {
        String somethingIsBird = "bird:$x";
        String somethingIsAnimal = "animal:$x";
        testIntroduction(somethingIsBird, Op.IMPL, somethingIsAnimal, "bird:robin", "animal:robin");
    }


    private void testIntroduction(String subj, Op relation, String pred, String belief, String concl) {

        NAR n = NARS.shell();

        new TestNAR(n)
                .believe('(' + subj + ' ' + relation + ' ' + pred + ')')
                .believe(belief)
                .mustBelieve(4, concl, 0.81f);
    }

    @Test
    void posNegQuestion() {


        RuleTest.get(new TestNAR(NARS.shell()),
                "a:b?", "(--,a:b).",
                "a:b.",
                0, 0, 0.9f, 0.9f);
    }

    @Test void testImageConceptualize() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        TermTest.assertEq("(x,z(y))", n.conceptualize("(x, (y --> (z,/)))").term());

    }
}