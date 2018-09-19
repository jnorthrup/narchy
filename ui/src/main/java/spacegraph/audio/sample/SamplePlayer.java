package spacegraph.audio.sample;

import org.jetbrains.annotations.NotNull;
import spacegraph.audio.SoundProducer;


public class SamplePlayer implements SoundProducer {
    @NotNull
    private final SoundSample sample;
    private float pos;
    private boolean alive = true;


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
                alive = false;
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

        if (alive && pos >= sample.buf.length) {
            alive = false;
        }
    }

    @Override
    public boolean isLive() {
        return alive;
    }

    @Override
    public void stop() {
        alive = false;
    }
}