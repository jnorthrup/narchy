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
    private float beat;
    private float x;
    private boolean playing;

    public BinauralTones(float initialBeat, float initialCarrier) {
        beat = initialBeat;
        carrier = initialCarrier;
        x = 0;
        playing = true;
    }


    @Override public void read(float[] buf, int readRate) {
        float dt = 1.0f / readRate;

        float leftRate = (carrier - (beat / 2.0f)) * (float)(Math.PI* 2.0f);
        float rigtRate = (carrier + (beat / 2.0f)) * (float)(Math.PI* 2.0f);
        for (int i = 0; i < buf.length-1; /*stereo*/) {
            buf[i++] = (float)FastMath.sin( x * leftRate );
            buf[i++] = (float)FastMath.sin( x * rigtRate );
            x += dt;
        }

    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float dt = 1.0f / readRate;
        x += dt * samplesToSkip;
    }

    @Override
    public boolean isLive() {
        return playing;
    }

    @Override
    public void stop() {
        playing = false;
    }
}
