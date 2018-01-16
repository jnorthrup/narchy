package nars.language;

import jcog.Util;
import nars.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NARSpeakTest {

    @Test
    public void testVocalization1() {
        NAR n = NARS.tmp();
        StringBuilder b = new StringBuilder();
        SpeechPlan s = new SpeechPlan(n, 1f, (w) -> {
            //System.out.println(n.time() + " " + w);
            b.append(n.time() + ":" + w + " ");
        });

        n.time.synch(n); //activate the service HACK

        s.speak($.the("x"), 1, $.t(1f, 0.9f));
        s.speak($.the("not_x"), 1, $.t(0f, 0.9f));
        s.speak($.the("y"), 2, $.t(1f, 0.9f));
        s.speak($.the("z"), 4, $.t(0.95f, 0.9f));
        s.speak($.the("not_w"), 6, $.t(1f, 0.9f));
        assertEquals(5, s.vocalize.size()); //not_w, scheduled for a future cycle
        n.run(5);
        assertEquals("1:x 2:y 4:z ", b.toString());
        assertEquals(1, s.vocalize.size()); //not_w, scheduled for a future cycle


    }

    @Test
    public void testHearGoal() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.termVolumeMax.set(16);
        n.freqResolution.set(0.1f);
        n.confMin.set(0.1f);

        Param.DEBUG = true;

        n.startFPS(40f);

        NARHear.hear(n, "a b c d e f g", "", 100);

        Util.sleep(2000);

        n.stop().tasks(true, false, false, false).forEach(x -> {
            System.out.println(x);
        });

        System.out.println();

        n.log();
        n.input("$1.0 (hear($1) ==> speak($1)).",
                "$1.0 speak(#1)!"
                );


        n.run(5000);
    }
}