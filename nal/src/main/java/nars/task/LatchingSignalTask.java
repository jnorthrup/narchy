package nars.task;

import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;

public class LatchingSignalTask extends SignalTask {


    public LatchingSignalTask(Term t, byte punct, Truth truth, long start, long end, long stamp) {
        super(t, punct, truth, start, end, stamp);
    }

    @Override
    public float evi(long when, long dur) {
        if (stretch != Frozen) {
            long s = start();
            if (when >= s) {
                 when = s;  //same value as if it were at the start for any value after the start, even if it hasnt been stretched this far
             }
        }

        return super.evi(when, dur);
    }

}
