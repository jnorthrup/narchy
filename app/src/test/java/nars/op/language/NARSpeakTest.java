package nars.op.language;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NARSpeakTest {

    @Test
    public void testVocalization1() {
        NAR n = NARS.tmp();
        StringBuilder b = new StringBuilder();
        Vocalization s = new Vocalization(n, 1f, (w) -> {
            
            b.append(n.time() + ":" + w + ' ');
        });

        n.synch(); 

        s.speak($.the("x"), 1, $.t(1f, 0.9f));
        s.speak($.the("not_x"), 1, $.t(0f, 0.9f));
        s.speak($.the("y"), 2, $.t(1f, 0.9f));
        s.speak($.the("z"), 4, $.t(0.95f, 0.9f));
        s.speak($.the("not_w"), 6, $.t(1f, 0.9f));
        assertEquals(5, s.vocalize.size()); 
        n.run(5);
        assertEquals("1:x 2:y 4:z ", b.toString());
        assertEquals(1, s.vocalize.size()); 


    }

    @Test @Disabled
    public void testHearGoal() throws Narsese.NarseseException {
        NAR n = NARS.threadSafe();
        n.termVolMax.set(16);
        n.freqResolution.set(0.1f);
        n.dtDither.set(50);
//        n.confMin.setAt(0.1f);

        n.log();
        n.input(
                "$1.0 (hear($1) =|> speak($1)).",
                "$1.0 (speak($1) =|> hear($1)).",
                "$1.0 (hear(#1) && speak(#1))!",
                "$1.0 speak(#1)!",
                "$1.0 speak(?1)@"
        );

        n.startFPS(40f);

        NARHear.hear(n, "a b c d e f g", "", 100, 0.1f);

        Util.sleepMS(5000);

        n.stop().tasks(true, false, true, false).forEach(x -> {
            System.out.println(x);
        });

        System.out.println();




    }
}