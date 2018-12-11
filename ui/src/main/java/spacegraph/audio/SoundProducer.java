package spacegraph.audio;

import jcog.math.FloatRange;

/** Monaural sound source */
public abstract class SoundProducer {
    private boolean live = true;


    public abstract void read(float[] buf, int readRate);

    public abstract void skip(int samplesToSkip, int readRate);

    public final void stop() {
        live = false;
    }
    public final boolean isLive() {
        return live;
    }

    public abstract static class Amplifiable extends SoundProducer {

        public final FloatRange amp = new FloatRange(1f, 0, 1f);

        public final float amp() { return amp.floatValue(); }

        public final void amp(float a) {
            amp.set(a);
        }
    }
}