package spacegraph.audio;

import jcog.math.FloatRange;

/**
 * Monaural (1-channel) sound source
 * */
@FunctionalInterface public interface SoundProducer {

    /** return false to signal the sound has stopped */
    boolean read(float[] buf, int readRate);

    /** implement to provide skip handling */
    default void skip(int samplesToSkip, int readRate) {

    }

    @Deprecated
    abstract class Amplifiable implements SoundProducer {

        public final FloatRange amp = new FloatRange(1f, (float) 0, 1f);

        public final float amp() { return amp.floatValue(); }

        public final void amp(float a) {
            amp.set(a);
        }
    }
}