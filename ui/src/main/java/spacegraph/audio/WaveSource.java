package spacegraph.audio;

import jcog.signal.buffer.CircularFloatBuffer;

/**
 * Source of a digitized 1D wave signal
 */
public interface WaveSource {

    /**
     * returns the buffer size, in samples
     */
    int start();

    void stop();

    int next(CircularFloatBuffer buffer);

    default int channelsPerSample() {
        return 1;
    }

    int samplesPerSecond();
}
