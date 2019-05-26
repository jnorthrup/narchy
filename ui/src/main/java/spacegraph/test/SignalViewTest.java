package spacegraph.test;

import com.github.sarxos.webcam.Webcam;
import com.google.common.util.concurrent.RateLimiter;
import jcog.Util;
import jcog.exe.Loop;
import jcog.net.UDPeer;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import jcog.random.XoRoShiRo128PlusRandom;
import jcog.signal.wave1d.DigitizedSignal;
import jcog.signal.wave1d.FreqDomain;
import jcog.signal.wave1d.SignalInput;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.unit.Animating;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.video.Draw;
import spacegraph.video.Tex;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

import javax.sound.sampled.LineUnavailableException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static spacegraph.SpaceGraph.window;

public class SignalViewTest {

    public static void main(String[] args) {
        window(newSignalView(), 800, 800);
    }

    public static Surface newSignalView()  {
        SensorNode n, n2;
        try {
            n = new SensorNode();
            n2 = new SensorNode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RealTimeLine cc = new RealTimeLine(n);

        for (Webcam ww : Webcam.getWebcams()) {
            VideoSensor v = n.add(ww);
            cc.add(ww, v.events);
        }

        for (AudioSource in : AudioSource.all()) {
            try {
                in.start();
                AudioSensor a = n.add(in);
                cc.add(a, a.events);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        }



//        {
//            SignalInput i = new SignalInput();
//            i.set(new NoiseSignal(), 2f / 30f/* + tolerance? */);
//            g.add(new SignalView(i).withControls());
//            i.setFPS(20f);
//        }
        return cc;
    }

    public static Timeline2D.AnalyzedEvent capture(BufferedImage t, long dur) {

        long now = System.currentTimeMillis();

        return new Timeline2D.AnalyzedEvent(new AspectAlign(Tex.view(t),
                ((float) t.getHeight()) / t.getWidth()), now - dur, now);
    }

    abstract public static class Sensor {
        public final String id;
        //lat, lon

        static final int capacity = 128;
        public final Timeline2D.FixedSizeEventBuffer<Timeline2D.SimpleEvent> events = new Timeline2D.FixedSizeEventBuffer(capacity);

        protected Sensor(String id) {
            this.id = id;
        }

    }

    public static class Sensor1D extends Sensor {

        public final DigitizedSignal i;
        public final SignalInput in;

        public Sensor1D(String id, DigitizedSignal i) {
            super(id);
            this.i = i;

            float audioFPS = 8;
            float granularity = 2;

            in = new SignalInput();
            in.set(i, 1/audioFPS);
            in.setFPS(audioFPS * granularity);

        }
    }
    public static class AudioSensor extends Sensor1D {
        public AudioSensor(String id, DigitizedSignal i) {
            super(id, i);
        }
    }
    public static class VideoSensor extends Sensor {

        public VideoSensor(String id) {
            super(id);
        }
    }

    public static class SensorNode {
        final Map<String,Sensor> sensors = new ConcurrentHashMap();

        public SensorNode() throws IOException {
            this(0);
        }

        public SensorNode(int port) throws IOException {
            UDPeer u = new UDPeer(port);
            u.setFPS(10);

            HttpServer server = null;
            try {
                server = new HttpServer(u.host(), port, new HttpModel() {

                    @Override
                    public void wssOpen(WebSocket ws, ClientHandshake handshake) {
                        ws.send("hi");
                    }

                    @Override
                    public void response(HttpConnection h) {
                        h.respond("");
                    }
                });
                server.setFPS(10f);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public VideoSensor add(Webcam ww) {
            return _add(new VideoSensor(ww.getName()));
        }

        public Sensor1D add(DigitizedSignal in) {
            return _add(new Sensor1D(in.toString(), in));
        }

        public AudioSensor add(AudioSource in) {
            return _add(new AudioSensor(in.name(), in));
        }

        private <S extends Sensor> S _add(S s) {
            Sensor t = sensors.put(s.id, s);
            assert(t==null);
            return s;
        }

    }

    public static class RealTimeLine extends Gridding {

        private final SensorNode node;
        float viewWindowSeconds = 8;

        public RealTimeLine(SensorNode node) {
            super(VERTICAL);
            this.node = node;
        }

        public Timeline2D newTrack(String label, Supplier<Surface> control) {
            return newTrack(new PushButton(label).clicked(() -> window(control.get(), 500, 500)));
        }

        public Timeline2D newTrack(Surface label) {
            Timeline2D g = new Timeline2D();

            add(new LabeledPane(label, new Animating(g, () -> {

                long e = System.currentTimeMillis();
                g.setTime(e - Math.round(viewWindowSeconds * 1000), e); //slide window
            }, 0.04f)));

            return g;
        }

        public void add(VideoSensor v) {

        }

        @Deprecated public void add(Webcam ww, Timeline2D.FixedSizeEventBuffer<Timeline2D.SimpleEvent> ge) {
            Timeline2D g = newTrack(ww.getName(), () -> new VideoSurface(new WebCam(ww)));


            WebCam w = new WebCam(ww, false);
            float camFPS = 0.5f;
            Loop.of(() -> {
                try {
                    BufferedImage ii = w.webcam.getImage();
                    if (ii != null)
                        ge.add(capture(ii, Math.round(1000 / camFPS)));

                } catch (Exception e) {
                    //ignore
                }
            }).setFPS(camFPS);

            g.addEvents(ge, v -> v.set(((Surface) (v.id.name))), new Timeline2DEvents.LinearTimelineUpdater<>());

        }

        public void add(AudioSensor in, Timeline2D.FixedSizeEventBuffer<Timeline2D.SimpleEvent> ge) {
            int freqs = 256;


//                FloatSlider preAmp = new FloatSlider("preAmp", 1, 0, 16f);

            //new Gridding(new VectorLabel(in.name()), preAmp)
            Timeline2D g = newTrack(in.toString(), () -> new PushButton(in.toString()));



            FreqDomain dft = new FreqDomain(freqs, 1);
            in.in.wave.on(a -> {
//                    long e = System.currentTimeMillis();

                //System.out.println(bufferTimeMs + "ms " + Math.round(n-bufferTimeMs)/2 + ".." + n);

                //WaveBitmap p = new WaveBitmap(new ArrayTensor(a.data.clone()), i.sampleRate, 200, 200);
                //p.setTime(s, e);

//                    Plot2D p = new Plot2D(a.data.length, Plot2D.Line);
//                    p.add(src, a.data);


//                    Util.mul(preAmp.asFloat(), a.data);

                double rms = 0;
                for (float x : a.data) {
                    rms += x * x;
                }
                rms /= a.data.length;
                rms = Math.sqrt(rms);


                //Gridding p = new Gridding();
                //p.color.set(rms*4, 0, 0, 1);

                float[] f = dft.apply(a).floatArray();

                Surface p;
                float fRMS = (float) rms;
                BitmapMatrixView pp = new BitmapMatrixView(1, freqs,
                        //arrayRendererY(dft.apply(a).floatArray())
                        (xIgnored, y) -> {
                            float fy = (float) (f[y]);
                            float fs = 0.5f + 0.5f * (fy * Util.unitize(fRMS));
                            float fb = 0.05f + 0.95f * fy;
                            return
                                    Draw.colorHSB(fRMS * 2, fs, fb);
                            //Draw.colorBipolar(f[y])
                        }
                );
                //pp.tex.mipmap(true);
                pp.cellTouch = false;
                pp.update();
                //p.add(ff);

                p = pp; //new ScaleXY(pp, 1, Util.lerp((float)Math.sqrt(rms), 0.8f, 1f));


                //p.add(new FreqSpectrogram( 8, 1).set(a));

//                    long s = e - Math.round( a.volume()/(float)i.sampleRate);

                SignalInput.RealTimeTensor ra = (SignalInput.RealTimeTensor) a;

                ge.add(new Timeline2D.AnalyzedEvent(p, ra.start, ra.end));
            });


            g.addEvents(ge, v -> v.set(((Surface) (v.id.name))), new Timeline2DEvents.LinearTimelineUpdater<>());

//                WaveBitmap w = new WaveBitmap(new ArrayTensor(i.data), i.sampleRate, i.data.length, 250);
//                w.setTime(before, now);
            //g.add(w);


        }
    }


    public static class NoiseSignal implements DigitizedSignal {

        final Random rng = new XoRoShiRo128PlusRandom();
        int sampleRate = 5000;
        int frames = 100;
        final RateLimiter r = RateLimiter.create(frames);

        @Override
        public int next(float[] target, int targetIndex, int samplesAtMost) {
            int n = Math.round(Math.min(((float)sampleRate) / frames, samplesAtMost));
            for (int i = 0; i < n; i++) {
                target[targetIndex++] = rng.nextFloat();
            }
            return samplesAtMost;
        }

        @Override
        public boolean hasNext(int samplesAtLeast) {
            return r.tryAcquire(1, 0, TimeUnit.MILLISECONDS);
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
