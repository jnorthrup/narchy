package nars.task.signal;

import jcog.Paper;
import jcog.Skill;
import nars.truth.Truth;

import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

/** wavelet-like freq/evidence function */
@Paper
@Skill("Wavelet")
abstract public class Truthlet implements Truth {

    abstract public long start();
    abstract public long end();

    public final long mid() { return (start() + end())/2L; }
    public final long range() {
        long e = end();
        long s = start();
        return e - s;
    }

    abstract public void setTime(long newStart, long newEnd);

    @Override
    public float freq() {
        return freq(mid()); //HACK
    }

    @Override
    public float evi() {
        return evi(mid()); //HACK
    }

    public float freq(long when) {
        return truth(when)[0];
    }
    public float evi(long when) {
        return truth(when)[1];
    }

    @Override
    public float conf() {
        return conf(mid()); //HACK
    }
    public float conf(long when) {
        return w2cSafe(truth(when)[1]);
    }

    public boolean containsTime(long when) {
        long s = start();
        if (when == s) return true;
        long e = end();
        return (when > s && when <= e);
    }

    public final float[] truth(long when) {
        float[] fe = new float[2];
        truth(when, fe);
        return fe;
    }

    abstract public void truth(long when, float[] freqEvi);

    /** sets a FreqEvi vector as unknown */
    public static void unknown(float[] freqEvi) {
        freqEvi[0] = Float.NaN;
        freqEvi[1] = 0;
    }


    public static Truthlet impulse(long start, long end, float freqOn, float freqOff, float evi) {
        return new ImpulseTruthlet(
                flat(start, end, freqOn, evi), freqOff, evi
        );
    }
    public static Truthlet step(float freqBefore, long start, float freqOn, long end, float freqAfter, float evi) {
        return new StepTruthlet(
                freqBefore, flat(start, end, freqOn, evi), freqAfter, evi
        );
    }

    public static Truthlet flat(long start, long end, float freq, float evi) {
        return new FlatTruthlet(start, end, freq, evi);
    }

    public static LinearTruthlet slope(long start, float startFreq, long end, float endFreq, float evi) {
        return new LinearTruthlet(start, startFreq, end, endFreq, evi);
    }

}
