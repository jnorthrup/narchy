package nars.task.signal;

import jcog.TODO;
import nars.NAR;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.XTERNAL;

/**
 * TODO implement Task directly avoiding redudant fields that this overrides
 */
public class TruthletTask extends SignalTask {

    public final Truthlet truthlet;

    public TruthletTask(Term t, byte punct, Truthlet truth, NAR n) {
        this(t, punct, truth, n.time.nextStamp());
    }

    public TruthletTask(Term t, byte punct, Truthlet truth, long stamp) {
        super(t, punct, truth, XTERNAL, XTERNAL, stamp);
        assert (punct == BELIEF || punct == GOAL);
        this.truthlet = truth;
        this.slidingEnd = end();
    }

    @Override
    public long start() {
        return truthlet.start();
    }

    @Override
    public long end() {
        return truthlet.end();
    }

    public void end(long newEnd) {

        if (truthlet instanceof RangeTruthlet) {

            super.end(newEnd); //HACK

            ((RangeTruthlet) truthlet).end = newEnd;

        } else {
            throw new TODO();
        }
    }

    @Nullable
    public final Truth truth(long when, long dur, float minConf) {
        Truth t = truth(when);
        if (t != null) {
            if (t.conf() >= minConf)
                return t;
        }
        return null;
    }
//    @Override
//    public float freq() {
//        return 0.5f;
//    }
//
//    @Override
//    public float conf() {
//        return super.conf();
//    }
//
//    @Override
//    public float coordF(boolean maxOrMin, int dimension) {
//        switch (dimension) {
//            case 0:
//                return maxOrMin ? end() : start();
//            case 1:
//            case 2:
//                return maxOrMin ? 0 : 1; //entire range, by default
//            default:
//                throw new UnsupportedOperationException();
//        }
//    }

    @Override
    public @Nullable Truth truth(long when, long dur) {
        return truth(when);
    }

    public @Nullable Truth truth(long when) {
        float[] tl = truthlet.truth(when);
        float f = tl[0];
        return f == f ? new PreciseTruth(f, tl[1] /* evi */, false) : null;
    }


    @Override
    public float evi(long when, long dur) {
        return truthlet.truth(when)[1];
    }
}
