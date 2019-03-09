package nars.truth;

import nars.$;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.polation.LinearTruthProjection;
import nars.truth.polation.TruthIntegration;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruthPolationTest {

    int serial = 0;
    static final Term x = $$("x");

    @Test
    public void testEqualEvidenceInSubInterval_of_Task() {
        //TODO repeat test for TruthletTask to test trapezoidal integration
        NALTask T = t(1f, 0.5f, 0, 4);
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
            LinearTruthProjection t = new LinearTruthProjection(0, W, 1);
            NALTask T = t(1f, 0.5f, 0, i);
            t.add(T);
            Truth tt = t.truth();
            System.out.println(T + "\t" +
                    TruthIntegration.eviAvg(T, 0, i, 1) + " eviInteg\t => " + tt + ";evi=" + tt.evi());

            /*assertEquals(T.freq(), tt.freq());
            assertEquals(T.evi() * ((float)i)/W, tt.evi(), 0.1f); //percentage of evidence being occluded
            assertEquals(T.conf(), tt.conf(), 0.01f);*/
        }
    }
    @Test
    public void testEvidenceIntegration_ConservedSingleTask_Half_Duration() {
        LinearTruthProjection t = new LinearTruthProjection(0, 10, 1);
        {
            float conf = 0.5f;
            t.add(t(1f, conf, 0, 10));
            @Nullable Truth tt = t.truth();
            assertEquals(1f, tt.freq());
            assertEquals(conf, tt.conf());
        }
    }


    private NALTask t(float freq, float conf, long start, long end) {
        long[] stamp = new long[] { serial++ };
        return NALTask.the(x, BELIEF, $.t(freq, conf), (long) 0, start, end, stamp);
    }
}