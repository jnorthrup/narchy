package nars.task.util.signal;

import nars.NAL;
import nars.time.Tense;

public class StepTruthlet extends ProxyTruthlet {
    public float freqBefore;
    public float freqAfter;
    public float eviInactive;

    public StepTruthlet(float freqBefore, Truthlet active, float freqAfter, float eviInactive) {
        super(active);
        this.freqBefore = freqBefore;
        this.freqAfter = freqAfter;
        this.eviInactive = eviInactive;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        var s = start();
        var e = end();

        if (s <= when && when <= e) {
            super.truth(when, freqEvi); 
        } else {

            var sd = Math.abs(s - when);
            var ed = Math.abs(e - when);
            long dist; float f;
            if (sd <= ed) {
                dist = sd;
                f = freqBefore;
            } else {
                dist = ed;
                f = freqAfter;
            }
            freqEvi[0] = f;
            freqEvi[1] = (float) NAL.evi(eviInactive, dist, Math.max(1, Tense.occToDT(e - s) / 2));
        }
    }

}
