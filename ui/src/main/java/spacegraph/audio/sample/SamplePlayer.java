package spacegraph.audio.sample;

import jcog.Util;
import spacegraph.audio.SoundProducer;

import static java.lang.System.arraycopy;


public class SamplePlayer extends SoundProducer {

    private final SoundSample sample;
    private int pos;


    public SamplePlayer(SoundSample sample) {
        this.sample = sample;
        pos = sample.start;
    }

    @Override
    public void read(float[] out, int readRate) {
        if (Util.equals(sample.rate, readRate, 1f/readRate)) {
            readDirect(out);
        } else {
            readResampled(out, readRate);
        }
    }

    private void readDirect(float[] out) {
        int remain = sample.end - pos;
        int toCopy = Math.min(out.length, remain);
        arraycopy(sample.buf, pos, out, 0, toCopy);
        pos += toCopy;

        if (remain <= toCopy)
            stop();
    }

    private void readResampled(float[] out, int readRate) {
        float step = (sample.rate) / readRate;
        float pos = this.pos;

        float[] in = sample.buf;
        float end = sample.end - 0.5f;

        for (int i = 0; i < out.length; i++) {
            if (pos >= end) {
                stop();
                break;
            }
            float next = in[Math.round(pos)];
            out[i] = next;
            pos = Math.min(end, pos + step);
        }
        this.pos = Math.round(pos);
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