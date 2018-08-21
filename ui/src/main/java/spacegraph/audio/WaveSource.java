package spacegraph.audio;

/**
 * Source of a digitized 1D wave signal
 */
public interface WaveSource {

    /**
     * returns the buffer size, in samples
     */
    int start();

    void stop();

    int next(float[] buffer);

    default int channelsPerSample() {
        return 1;
    }

    int samplesPerSecond();
}
