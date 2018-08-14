package nars.truth;

import nars.$;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.polation.LinearTruthPolation;
import nars.truth.polation.TruthIntegration;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TruthPolationTest {

    int serial = 0;
    static final Term x = $$("x");

    @Test
    public void testEqualEvidenceInSubInterval_of_Task() {
        //TODO repeat test for TruthletTask to test trapezoidal integration
        NALTask T = t(1f, 0.5f, 0, 4);
        assertEquals(T.truth(), T.truth(2, 1));
        assertEquals(T.truth(), T.truth(0, 4, 1));
        assertEquals(T.truth(), T.truth(2, 3, 1));
        assertNotEquals(T.truth(), Truth.weaker(T.truth(), T.truth(2, 5, 1)));
    }

    @Test
    public void testEvidenceIntegration_ConservedSingleTask_Full_Duration() {
        int W = 10;
        for (int i = W-1; i >=1; i--)  {
            LinearTruthPolation t = new LinearTruthPolation(0, W, 1);
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
        LinearTruthPolation t = new LinearTruthPolation(0, 10, 1);
        {
            t.add(t(1f, 0.5f, 0, 10));
            @Nullable Truth tt = t.truth();
            assertEquals(1f, tt.freq());
            assertEquals(w2c(c2w(0.5f)/2), tt.conf());
        }
    }


    private NALTask t(float freq, float conf, long start, long end) {
        return new NALTask(x,BELIEF, $.t(freq, conf), 0, start, end, new long[] { serial++ });
    }
}