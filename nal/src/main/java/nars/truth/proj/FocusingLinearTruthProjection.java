package nars.truth.proj;

import nars.Task;

/**
 * duration is adjusted to the minimum distance of all components, thus
 * 'focusing' or 'narrowing' the duration if components contribute
 * truth within or nearer than it.
 */
public class FocusingLinearTruthProjection extends LinearTruthProjection {


    private final static int minDur =
            0;
            //1;


    public FocusingLinearTruthProjection(long start, long end, int dur) {
        super(start, end, dur);
    }

    @Override
    public boolean add(Task t) {

        if (super.add(t)) {

            if (dur > minDur) {
                if (!t.isEternal()) {
                    dur = Math.max(minDur, Math.min(dur, (int) t.minTimeTo(start, end)));
                }
            }
            return true;
        }
        return false;
    }


}
