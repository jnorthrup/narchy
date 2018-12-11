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


    @Override public void read(float[] buf, int readRate) {
        float dt = 1.0f / readRate;


        float r = (freq ) * (float)(Math.PI* 2.0f);
        float A = amp();
        float X = x;
        for (int i = 0; i < buf.length;) {
            buf[i++] = (float)Math.sin(X * r) * A;
            X += dt;
        }
        x = X;

    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float dt = 1.0f / readRate;
        x += dt * samplesToSkip;
    }



}
