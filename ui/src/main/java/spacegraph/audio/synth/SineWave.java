package spacegraph.audio.synth;

import spacegraph.audio.SoundProducer;

/**
 * Created by me on 2/4/15.
 */
public class SineWave extends SoundProducer.Amplifiable {

    private final float freq;
    private float x;

    public SineWave(float freq) {
        this.freq = freq;
        x = 0;
    }


    @Override public boolean read(float[] buf, int readRate) {
        var dt = 1.0f / readRate;


        var r = (freq ) * (float)(Math.PI* 2.0f);
        var A = amp();
        var X = x;
        for (var i = 0; i < buf.length;) {
            buf[i++] = (float)Math.sin(X * r) * A;
            X += dt;
        }
        x = X;
        return true;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        var dt = 1.0f / readRate;
        x += dt * samplesToSkip;
    }



}
