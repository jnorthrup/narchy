package spacegraph.space2d.widget;

import com.google.common.util.concurrent.RateLimiter;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave1d.DigitizedSignal;
import jcog.signal.wave1d.SignalInput;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.container.time.SignalView;
import spacegraph.space2d.widget.button.MapSwitch;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SignalViewTest {
    public static void main(String[] args) {

        AudioSource audio = new AudioSource();

        SignalInput i = new SignalInput();

//        i.set(audio,1f / 30f/* + tolerance? */);

        SpaceGraph.surfaceWindow(new LabeledPane(MapSwitch.the(Map.of(
                "Audio", ()->{
                    i.set(audio, 2f / 30f/* + tolerance? */);
                    audio.start();
                },
                "Noise", ()->{
                    audio.stop(); //HACK
                    i.set(new NoiseSignal(), 2f/30f);
                }
        )), new SignalView(i).withControls()), 800, 800);

        i.setFPS(20f);
    }

    public static class NoiseSignal implements DigitizedSignal {

        int sampleRate = 5000;
        int frames = 100;
        final Random rng = new XoRoShiRo128PlusRandom();

        @Override
        public int next(float[] target, int targetIndex, int samplesAtMost) {
            for (int i = 0; i < Math.min(sampleRate/frames, samplesAtMost); i++) {
                target[targetIndex++] = rng.nextFloat();
            }
            return samplesAtMost;
        }

        final RateLimiter r = RateLimiter.create(frames);

        @Override
        public boolean hasNext(int samplesAtLeast) {
            return r.tryAcquire(1, 1, TimeUnit.MILLISECONDS);
        }

        @Override
        public int sampleRate() {
            return sampleRate;
        }
    }
}
