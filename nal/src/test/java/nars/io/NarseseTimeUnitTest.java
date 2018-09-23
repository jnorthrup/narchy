package nars.io;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.io.NarseseTest.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NarseseTimeUnitTest {

    /** milliseconds realtime */
    private final NAR n = NARS.realtime(1f).get();

    @Test
    void testOccurence() throws Narsese.NarseseException {
        @Deprecated Task now = task("(a-->b). :|:");
        
        
        Task now1 = task("(a-->b). |"); 
        Task now1withTruth = task("(a-->b). | %1.0;0.90%"); 
        Task now2 = task("(a-->b). +0"); 
        Task next = task("(a-->b). +1");
        Task prev = task("(a-->b). -1");
        assertEquals(now1.start() , now2.start());
        assertEquals(now1.start() , now1withTruth.start());
        assertEquals(now1.start()+1 , next.start());
        assertEquals(now1.start()-1 , prev.start());
    }

    @Test
    void testCycleTimeOccurenceRange() throws Narsese.NarseseException {
//        @Deprecated Task now = task("(a-->b). :|:");

        Task now1 = task("(a-->b). +1..2");
        assertEquals(1 , now1.start());
        assertEquals(2, now1.end());
    }

    @Test
    void testRealtimeOccurrence() throws Narsese.NarseseException {
        Task day = n.inputTask("(a-->b). +1day");
        assertEquals(day.start() - n.time(), 8.64E7, 10000);

        Task minusDay = n.inputTask("(a-->b). -1day");
        assertEquals(minusDay.start() - n.time(), -8.64E7, 10000);

        Task plusHour = n.inputTask("(a-->b). +1h");
        assertEquals(plusHour.start() - n.time(), 60*60*1000, 1000);

        Task plusHour2 = n.inputTask("(a-->b). +1hr");
        assertEquals(plusHour2.toString(), plusHour.toString());

        Task minusHour = n.inputTask("(a-->b). -1h");
        assertEquals(minusHour.start() - n.time(), -60*60*1000, 10000);

        Task year = n.inputTask("(a-->b). +1year");
        assertEquals(year.start() - n.time(), 3.15569521E10, 10000);

    }

    @Test
    void testRealtimeRelativeOccurrenceRange() throws Narsese.NarseseException {

        {
            for (String nowStr : new String[] { "|", "+0", ":|:" }) {
                String taskStr = "(a-->b). " + nowStr + "..+5m";
                Task x = n.inputTask(taskStr);
                System.out.println(taskStr + " " + x);
                assertEquals(x.start() - n.time(), 0, 1000);
                assertEquals(x.end() - n.time(), 5 * 60 * 1000, 1000);
            }
        }

        {
            Task x = n.inputTask("(a-->b). -2h..+5m");
            assertEquals(n.time() - x.start(), 2 * 60 * 60 * 1000, 1000);
            assertEquals(x.end() - n.time(), 5 * 60 * 1000, 1000);
        }

        {
            
            Task x = n.inputTask("(a-->b). +5h..-2m");
            assertEquals(n.time() - x.start(), 2 * 60 * 1000, 1000);
            assertEquals(x.end() - n.time(), 5 * 60 * 60 * 1000, 1000);
        }

        {
            Task x = n.inputTask("(a-->b). +2h..+7h");
            assertEquals(x.start() - n.time(), 2 * 60 * 60 * 1000, 1000);
            assertEquals(x.end() - n.time(), 7 * 60 * 60 * 1000, 1000);
        }
        {
            Task x = n.inputTask("(a-->b). -7h..-2h");
            assertEquals(x.start() - n.time(), -7 * 60 * 60 * 1000, 1000);
            assertEquals(x.end() - n.time(), -2 * 60 * 60 * 1000, 1000);
        }
        {
            Task x = n.inputTask("(a-->b). -2s..|");
            assertEquals(x.start() - n.time(), -2000, 100);
            assertEquals(x.end() - n.time(), 0, 100);
        }

    }

    @Test
    void testTimeDeltaUnits() {

        assertEquals(
                "term(\"&&\",(a,b),(day,1))",
                $$("(a &&+1day b)").toString()
        );


        assertEquals("(a &&+86400000 b)",
                $$("(a &&+1day b)").eval(n).toString()
        );


        assertEquals("(a &&+129600000 b)",
                $$("(a &&+1.5days b)").eval(n).toString()
        );
    }
    @Test
    void testTimeDeltaUnits2() {
        assertEquals("((c &&+259200000 b) &&+604800000 a)",
                $$("(a &&-1week (b &&-3days c))").eval(n).toString()
        );

    }

    @Test
    void testTimeDeltaUnitsAndOccurrence() throws Narsese.NarseseException {
        StringBuilder sb = new StringBuilder();
        n.log(sb);
        n.input("(a &&+1day b)! +5m..+2h %1.0;0.90%");
        assertTrue(sb.toString().contains("(a &&+86400000 b)! "));
        assertTrue(sb.toString().contains("%1.0;.90%"));

    }









}
