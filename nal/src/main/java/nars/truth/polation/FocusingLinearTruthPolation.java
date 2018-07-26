package nars.truth.polation;

import nars.Task;

/**
 * duration is adjusted to the minimum distance of all components, thus
 * 'focusing' or 'narrowing' the duration if components contribute
 * truth within or nearer than it.
 */
public class FocusingLinearTruthPolation extends LinearTruthPolation {


    private final static int minDur =
            0;
    //1;


    public FocusingLinearTruthPolation(long start, long end, int dur) {
        super(start, end, dur);
    }

    @Override
    public TruthPolation add(Task t) {
        super.add(t);

        if (dur > minDur) {
            if (!t.isEternal()) {
                dur = Math.max(minDur, Math.min(dur, (int) t.minTimeTo(start, end)));
            }
        }
        return this;
    }


}
