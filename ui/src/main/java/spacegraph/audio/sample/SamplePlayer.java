package spacegraph.audio.sample;

import spacegraph.audio.SoundProducer;


public class SamplePlayer extends SoundProducer {

    private final SoundSample sample;
    private float pos;


    public SamplePlayer(SoundSample sample) {
        this.sample = sample;
    }

    @Override
    public void read(float[] buf, int readRate) {
        float step = (sample.rate ) / readRate;

        float[] sb = sample.buf;

        for (int i = 0; i < buf.length; i++) {
            float next;
            if (pos >= sb.length) {
                next = 0;
                stop();
            } else {
                next = sb[(int) (pos)];
            }
            buf[i] = next;
            pos += step;
        }


    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float step = sample.rate / readRate;
        pos += step * samplesToSkip;

        if (pos >= sample.buf.length) {
            stop();
        }
    }

}