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
        if (Util.equals(sample.rate, readRate, 1f/readRate)) {
            return readDirect(out);
        } else {
            return readResampled(out, readRate);
        }
    }

    private boolean readDirect(float[] out) {
        var remain = sample.end - pos;
        if (remain <= 0)
            return false;

        var toCopy = Math.min(out.length, remain);
        arraycopy(sample.buf, pos, out, 0, toCopy);
        pos += toCopy;

        return remain > toCopy;
    }

    private boolean readResampled(float[] out, int readRate) {
        var step = (sample.rate) / readRate;
        float pos = this.pos;

        var in = sample.buf;
        var end = sample.end - 0.5f;

        for (var i = 0; i < out.length; i++) {
            if (pos >= end)
                return false;

            var next = in[Math.round(pos)];
            out[i] = next;
            pos = Math.min(end, pos + step);
        }
        this.pos = Math.round(pos);
        return true;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        var step = sample.rate / readRate;
        pos += step * samplesToSkip;
    }

}