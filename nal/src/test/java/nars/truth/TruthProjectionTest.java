package nars.truth;

import nars.$;
import nars.task.AbstractTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.proj.LinearTruthProjection;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruthProjectionTest {

    static final AtomicInteger serial = new AtomicInteger();
    static final Term x = $$("x");

    @Test
    public void testEqualEvidenceInSubInterval_of_Task() {
        //TODO repeat test for TruthletTask to test trapezoidal integration
        AbstractTask T = t(1f, 0.5f, 0, 4);
        for (int s = 0; s <=4; s++)
            for (int e = s; e<=4; e++ )
                assertEquals(T.truth(), T.truth(s, e, 0));
        assertTrue( T.truth(2, 10, 1).conf() < T.truth(2, 5, 1).conf());
        assertEquals(T.truth(), T.truth(0, 4, 0));
        assertEquals(T.truth(), T.truth(0, 4, 1));
        assertEquals(T.truth(), T.truth(2, 3, 1));

//        Truth wa = T.truth(2, 5, 1);
//        Truth w = Truth.weaker(T.truth(), wa);
//        assertNotEquals(T.truth(), w);
    }

    @Test
    public void testEvidenceIntegration_ConservedSingleTask_Full_Duration() {
        int W = 10;
        for (int i = W-1; i >=1; i--)  {
            LinearTruthProjection t = new LinearTruthProjection(0, W);
            AbstractTask T = t(1f, 0.5f, 0, i);
            t.add(T);
            Truth tt = t.truth();
            System.out.println(T + "\t" +
                    T.eviAvg(0, i, 1, false) + " eviInteg\t => " + tt + ";evi=" + tt.evi());

            /*assertEquals(T.freq(), tt.freq());
            assertEquals(T.evi() * ((float)i)/W, tt.evi(), 0.1f); //percentage of evidence being occluded
            assertEquals(T.conf(), tt.conf(), 0.01f);*/
        }
    }
//
//    @Test
//    public void testEvidenceIntegration_ConservedSingleTask_Half_Duration() {
//        float conf = 0.5f;
//        NALTask t = t(1f, conf, 0, 10);
//
//        //monotonically decrease evidence further away from task, regardless of observation time
//        for (long now : new long[] { -10, -5, 0, 5, 10, 15}) {
//            assertTrue(t.eviRelative(1, now) == t.eviRelative(9, now));
//            assertTrue(t.eviRelative(-1, now) > t.eviRelative(-2, now));
//            assertTrue(t.eviRelative(10, now) > t.eviRelative(11, now));
//            assertTrue(t.eviRelative(11, now) > t.eviRelative(12, now));
//        }
//        //monotonically decrease evidence further away from task, regardless of tgt time
//        for (long tgt : new long[] { -10, -5, 0, 5, 10, 15}) {
//            double a = t.eviRelative(tgt, 0);
//            double b = t.eviRelative(tgt, 20);
//            assertTrue(a > b);
//        }
//
//
//        LinearTruthProjection p = new LinearTruthProjection(0, 10);
//        p.add(t);
//        @Nullable Truth tt = p.truth();
//        assertEquals(1f, tt.freq());
//        assertEquals(conf, tt.conf());
//    }


    static private AbstractTask t(float freq, float conf, long start, long end) {
        long[] stamp = new long[] { serial.getAndIncrement() };
        return NALTask.the(x, BELIEF, $.t(freq, conf), (long) 0, start, end, stamp);
    }

}