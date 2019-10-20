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
        x = (float) 0;
    }


    @Override public boolean read(float[] buf, int readRate) {
        float dt = 1.0f / (float) readRate;


        float r = (freq ) * (float)(Math.PI* 2.0);
        float A = amp();
        float X = x;
        for (int i = 0; i < buf.length;) {
            buf[i++] = (float)Math.sin((double) (X * r)) * A;
            X += dt;
        }
        x = X;
        return true;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float dt = 1.0f / (float) readRate;
        x += dt * (float) samplesToSkip;
    }



}
