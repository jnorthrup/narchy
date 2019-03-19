package jcog.signal.wave1d;

/** 1D signal stream reader.  like an Iterator for continuous signal streams */
public interface DigitizedSignal {

    /** analogous to: line.read(...) */
    int next(float[] target, int targetIndex, int samplesAtMost);

    boolean hasNext(int samplesAtLeast);
}
