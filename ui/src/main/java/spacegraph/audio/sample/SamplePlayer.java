package spacegraph.audio.sample;

import jcog.Util;
import spacegraph.audio.SoundProducer;

import static java.lang.System.arraycopy;


public class SamplePlayer implements SoundProducer {

    private final SoundSample sample;
    private int pos;

    public SamplePlayer(SoundSample sample) {
        this.sample = sample;
        pos = sample.start;
    }

    @Override
    public boolean read(float[] out, int readRate) {
        if (Util.equals(sample.rate, (float) readRate, 1f/ (float) readRate)) {
            return readDirect(out);
        } else {
            return readResampled(out, readRate);
        }
    }

    private boolean readDirect(float[] out) {
        int remain = sample.end - pos;
        if (remain <= 0)
            return false;

        int toCopy = Math.min(out.length, remain);
        arraycopy(sample.buf, pos, out, 0, toCopy);
        pos += toCopy;

        return remain > toCopy;
    }

    private boolean readResampled(float[] out, int readRate) {
        float step = (sample.rate) / (float) readRate;
        float pos = (float) this.pos;

        float[] in = sample.buf;
        float end = (float) sample.end - 0.5f;

        for (int i = 0; i < out.length; i++) {
            if (pos >= end)
                return false;

            float next = in[Math.round(pos)];
            out[i] = next;
            pos = Math.min(end, pos + step);
        }
        this.pos = Math.round(pos);
        return true;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float step = sample.rate / (float) readRate;
        pos = (int) ((float) pos + step * (float) samplesToSkip);
    }

}