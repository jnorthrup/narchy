package spacegraph.audio;

/** Monaural sound source */
public interface SoundProducer {
    void read(float[] buf, int readRate);
    void skip(int samplesToSkip, int readRate);
    boolean isLive();

    default float getAmplitude() { return 1.0f; }



    interface Amplifiable {
        void setAmplitude(float a);
    }
}