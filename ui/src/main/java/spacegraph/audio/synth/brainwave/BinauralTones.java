package spacegraph.audio.synth.brainwave;

import org.apache.commons.math3.util.FastMath;
import spacegraph.audio.SoundProducer;

/**
 * Binaural Beats for Human Brainwave Entrainment and Hemispheric Synchronization
 * http:
 *
 * TODO make this a set of 2 SoundProducer's, set at fixed ambient Left/Right positions
 */
public class BinauralTones implements SoundProducer {

    private final float carrier;
    private final float beat;
    private float x;

    public BinauralTones(float initialBeat, float initialCarrier) {
        beat = initialBeat;
        carrier = initialCarrier;
        x = (float) 0;
    }

    @Override public boolean read(float[] buf, int readRate) {
        float dt = 1.0f / (float) readRate;

        float leftRate = (carrier - (beat / 2.0f)) * (float)(Math.PI* 2.0);
        float rigtRate = (carrier + (beat / 2.0f)) * (float)(Math.PI* 2.0);
        for (int i = 0; i < buf.length-1; /*stereo*/) {
            buf[i++] = (float)FastMath.sin((double) (x * leftRate));
            buf[i++] = (float)FastMath.sin((double) (x * rigtRate));
            x += dt;
        }

        return true;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float dt = 1.0f / (float) readRate;
        x += dt * (float) samplesToSkip;
    }

}
