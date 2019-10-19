package spacegraph.audio;

import jcog.math.FloatRange;

/** Monaural sound source
 *
 * TODO remove Amplifiable subclass, make this @FunctionalInterface that only requires read() to be impl
 * move all mutable state to Sound which is created and returned when this is played.
 * */
public abstract class SoundProducer {

    public abstract void read(float[] buf, int readRate);

    /** implement to provide skip handling */
    public void skip(int samplesToSkip, int readRate) {

    }

    @Deprecated private boolean live = true;
    @Deprecated public final void stop() {
        live = false;
    }
    @Deprecated public final boolean isLive() {
        return live;
    }

    @Deprecated public abstract static class Amplifiable extends SoundProducer {

        public final FloatRange amp = new FloatRange(1f, 0, 1f);

        public final float amp() { return amp.floatValue(); }

        public final void amp(float a) {
            amp.set(a);
        }
    }
}