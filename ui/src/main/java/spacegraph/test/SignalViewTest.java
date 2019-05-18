package spacegraph.test;

import com.google.common.util.concurrent.RateLimiter;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave1d.DigitizedSignal;
import jcog.signal.wave1d.SignalInput;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.SignalView;

import javax.sound.sampled.LineUnavailableException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SignalViewTest {

    public static void main(String[] args) {
        SpaceGraph.window(newSignalView(), 800, 800);
    }

    public static Surface newSignalView() {
        Gridding g = new Gridding();

        AudioSource.all().forEach(audio -> {
            SignalInput i = new SignalInput();
            try {
                audio.start();
                i.set(audio, 1/10f);
                g.add(new SignalView(i).withControls());
                i.setFPS(20f);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        });
        {
            SignalInput i = new SignalInput();
            i.set(new NoiseSignal(), 2f / 30f/* + tolerance? */);
            g.add(new SignalView(i).withControls());
            i.setFPS(20f);
        }
        return g;
    }
//    public static LabeledPane newSignalView() {
//        AudioSource audio = new AudioSource();
//
//        SignalInput i = new SignalInput();
//
////        i.set(audio,1f / 30f/* + tolerance? */);
//
//        ButtonSet<?> menu = MapSwitch.the(Map.of(
//                "Audio", () -> {
//                    i.set(audio, 2f / 30f/* + tolerance? */);
//                    audio.start();
//                },
//                "Noise", () -> {
//                    audio.stop(); //HACK
//                    i.set(new NoiseSignal(), 5f / 30f);
//                }
//        ));
//        menu.buttons.get(1).on(true);
//
//        LabeledPane s = new LabeledPane(menu, new SignalView(i).withControls());
//
//        i.setFPS(20f);
//        return s;
//    }

    public static class NoiseSignal implements DigitizedSignal {

        int sampleRate = 5000;
        int frames = 100;
        final Random rng = new XoRoShiRo128PlusRandom();

        @Override
        public int next(float[] target, int targetIndex, int samplesAtMost) {
            int n = Math.min(sampleRate / frames, samplesAtMost);
            for (int i = 0; i < n; i++) {
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
