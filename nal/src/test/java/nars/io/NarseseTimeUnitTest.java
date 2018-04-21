package nars.io;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.io.NarseseTest.task;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NarseseTimeUnitTest {

    /** milliseconds realtime */
    final NAR n = NARS.realtime(1f).get();

    @Test
    public void testOccurence() throws Narsese.NarseseException {
        @Deprecated Task now = task("<a --> b>. :|:");
        //Task f = task("<a --> b>. :/:");
        //Task p = task("<a --> b>. :\\:");
        Task now1 = task("<a --> b>. |"); //now, shorthand
        Task now1withTruth = task("<a --> b>. | %1.0;0.90%"); //now, shorthand with truth
        Task now2 = task("<a --> b>. +0"); //now, numeric
        Task next = task("<a --> b>. +1");
        Task prev = task("<a --> b>. -1");
        assertEquals(now1.start() , now2.start());
        assertEquals(now1.start() , now1withTruth.start());
        assertEquals(now1.start()+1 , next.start());
        assertEquals(now1.start()-1 , prev.start());
    }
    @Test
    public void testRealtimeOccurrence() throws Narsese.NarseseException {
        Task day = n.inputTask("<a --> b>. +1day");
        assertEquals(day.start() - n.time(), 8.64E7, 10000);

        Task minusDay = n.inputTask("<a --> b>. -1day");
        assertEquals(minusDay.start() - n.time(), -8.64E7, 10000);


        Task plusHour = n.inputTask("<a --> b>. +1h");
        assertEquals(plusHour.start() - n.time(), 60*60*1000, 1000);

        Task minusHour = n.inputTask("<a --> b>. -1h");
        assertEquals(minusHour.start() - n.time(), -60*60*1000, 10000);

        Task year = n.inputTask("<a --> b>. +1year");
        assertEquals(year.start() - n.time(), 3.15569521E10, 10000);

    }


    @Test
    public void testTimeDeltaUnits() throws Narsese.NarseseException {
        assertEquals(
            "term(\"&&\",(a,b),(day,1))",
            $$("(a &&+1day b)").toString()
        );
        assertEquals("(a &&+86400000 b)",
                $$("(a &&+1day b).").eval(n).toString()
        );
//        assertEquals("(a &&+86400000 b). %1.0;0.90%",
//            n.inputTask("(a &&+1day b).").toString()
//        );
    }
}
