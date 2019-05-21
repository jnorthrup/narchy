package spacegraph.test;

import com.google.common.util.concurrent.RateLimiter;
import jcog.Util;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave1d.DigitizedSignal;
import jcog.signal.wave1d.FreqDomain;
import jcog.signal.wave1d.SignalInput;
import spacegraph.SpaceGraph;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.unit.Animating;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.container.unit.ScaleXY;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.video.Tex;
import spacegraph.video.WebCam;

import javax.sound.sampled.LineUnavailableException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static spacegraph.space2d.widget.meter.BitmapMatrixView.arrayRendererY;

public class SignalViewTest {

    public static void main(String[] args) {
        SpaceGraph.window(newSignalView(), 800, 800);
    }

    public static class RealTimeLine extends Gridding {

        float viewWindowSeconds = 8;

        public RealTimeLine() {
            super(VERTICAL);
        }

        public Timeline2D newTrack(String label) {
            Timeline2D g = new Timeline2D();

            add(LabeledPane.the(label, new Animating(g, ()->{

                long e = System.currentTimeMillis();
                g.setTime(e - Math.round(viewWindowSeconds * 1000), e); //slide window
            }, 0.1f)));

            return g;
        }
    }

    public static Surface newSignalView() {
        RealTimeLine cc = new RealTimeLine();

        int capacity = 128;
        float fps = 8;
        float granularity = 2;
        int freqs = 64;

        for (WebCam w : List.of(WebCam.the())) {
            Timeline2D g = cc.newTrack(w.webcam.getName());
            Timeline2D.SimpleTimelineEvents ge = new Timeline2D.SimpleTimelineEvents();

            //TODO best synch / non-async webcam capture mode
            w.tensor.on((t)->{
                if (Math.random() < 0.05f) {
                    long now = System.currentTimeMillis();

                    if (ge.size() + 1 > capacity)
                        ge.pollFirst();

                    ge.add(new Timeline2D.AnalyzedEvent(new AspectAlign(Tex.view(t.img), ((float)t.height())/t.width()), now - 500, now));
                }
            });


            g.addEvents(ge, v-> v.set(((Surface)(v.id.name))), new Timeline2DEvents.LinearTimelineUpdater<>());
        }

        AudioSource.all().forEach(in -> {
            try {


                Timeline2D g = cc.newTrack(in.name());
                Timeline2D.SimpleTimelineEvents ge = new Timeline2D.SimpleTimelineEvents();




                SignalInput i = new SignalInput();
                i.set(in, 1/fps);
//                g.add(new SignalView(i).withControls());
                i.setFPS(fps * granularity);

                in.start();

                FreqDomain dft = new FreqDomain(freqs, 1);
                i.wave.on(a->{
//                    long e = System.currentTimeMillis();

                    //System.out.println(bufferTimeMs + "ms " + Math.round(n-bufferTimeMs)/2 + ".." + n);

                    //WaveBitmap p = new WaveBitmap(new ArrayTensor(a.data.clone()), i.sampleRate, 200, 200);
                    //p.setTime(s, e);

//                    Plot2D p = new Plot2D(a.data.length, Plot2D.Line);
//                    p.add(src, a.data);

                    double rms = 0;
                    for (float x : a.data) {
                        rms += x*x;
                    }
                    rms/=a.data.length;
                    rms = Math.sqrt(rms);


                    //Gridding p = new Gridding();
                    //p.color.set(rms*4, 0, 0, 1);

                    Surface p;
                    BitmapMatrixView pp = new BitmapMatrixView( 1, freqs, arrayRendererY(dft.apply(a).floatArray()));
                    pp.cellTouch = false;
                    pp.update();
                    //p.add(ff);
                    p = new ScaleXY(pp, 1, Util.lerp((float)Math.sqrt(rms), 0.8f, 1f));



                    //p.add(new FreqSpectrogram( 8, 1).set(a));

//                    long s = e - Math.round( a.volume()/(float)i.sampleRate);

                    SignalInput.RealTimeTensor ra = (SignalInput.RealTimeTensor) a;

                    if (ge.size()+1 > capacity)
                        ge.pollFirst();

                    ge.add(new Timeline2D.AnalyzedEvent(p, ra.start, ra.end));
                });


                g.addEvents(ge, v-> v.set(((Surface)(v.id.name))), new Timeline2DEvents.LinearTimelineUpdater<>());

//                WaveBitmap w = new WaveBitmap(new ArrayTensor(i.data), i.sampleRate, i.data.length, 250);
//                w.setTime(before, now);
                //g.add(w);


            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        });


//        {
//            SignalInput i = new SignalInput();
//            i.set(new NoiseSignal(), 2f / 30f/* + tolerance? */);
//            g.add(new SignalView(i).withControls());
//            i.setFPS(20f);
//        }
        return cc;
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

        @Override
        public long time() {
            return System.currentTimeMillis();
        }
    }
}
