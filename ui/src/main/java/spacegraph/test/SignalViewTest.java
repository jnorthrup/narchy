package spacegraph.test;

import com.google.common.util.concurrent.RateLimiter;
import jcog.exe.Loop;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave1d.DigitizedSignal;
import jcog.signal.wave1d.SignalInput;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.widget.button.PushButton;

import javax.sound.sampled.LineUnavailableException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SignalViewTest {

    public static void main(String[] args) {
        SpaceGraph.window(newSignalView(), 800, 800);
    }

    public static Surface newSignalView() {
        //Gridding g = Gridding.column();

        long now = System.currentTimeMillis();
        long before = now -  2* 1000;

        Timeline2D g = new Timeline2D(before, now + 2 * 1000);
        int capacity = 50;
        Timeline2D.SimpleTimelineEvents ge = new Timeline2D.SimpleTimelineEvents();

        Loop.of(()-> {
            long e = System.currentTimeMillis();
            g.setTime(e - 5 * 1000, e); //HACK force update

            while (ge.size() > capacity)
                ge.pollFirst();

        }).setFPS(2);




        AudioSource.all().forEach(in -> {
            try {
                in.start();
                String src = Integer.toString(in.hashCode()); //i.toString();

                SignalInput i = new SignalInput();
                i.set(in, 0.2f);
//                g.add(new SignalView(i).withControls());
                i.setFPS(8f);
                i.wave.on(a->{
                    long e = System.currentTimeMillis();
                    double bufferTimeMs = 1000*((double)a.volume())/i.sampleRate;
                    long s = Math.round(e - bufferTimeMs);

                    //System.out.println(bufferTimeMs + "ms " + Math.round(n-bufferTimeMs)/2 + ".." + n);

                    //WaveBitmap p = new WaveBitmap(new ArrayTensor(a.data.clone()), i.sampleRate, 200, 200);
                    //p.setTime(s, e);

//                    Plot2D p = new Plot2D(a.data.length, Plot2D.Line);
//                    p.add(src, a.data);

                    float rms = 0;
                    for (float x : a.data) {
                        rms += x*x;
                    }
                    rms/=a.data.length;
                    rms = (float) Math.sqrt(rms);


                    PushButton p = new PushButton(src);
                    p.color.set(rms*4, 0, 0, 1);

                    ge.add(new Timeline2D.SimpleEvent(p, s+1, e-1));
                });

//                WaveBitmap w = new WaveBitmap(new ArrayTensor(i.data), i.sampleRate, i.data.length, 250);
//                w.setTime(before, now);
                //g.add(w);


            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        });

        //g.setTime(before, now); //HACK force update
        g.addEvents(ge, (nv)-> nv.set(((Surface)(nv.id.name)))); // new PushButton(nv.id.toString())));

//        {
//            SignalInput i = new SignalInput();
//            i.set(new NoiseSignal(), 2f / 30f/* + tolerance? */);
//            g.add(new SignalView(i).withControls());
//            i.setFPS(20f);
//        }
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
