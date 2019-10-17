package spacegraph.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.google.common.util.concurrent.RateLimiter;
import jcog.Util;
import jcog.exe.Every;
import jcog.exe.Exe;
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
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.unit.Animating;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.video.*;

import javax.sound.sampled.LineUnavailableException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static spacegraph.SpaceGraph.window;

public class SignalViewTest {

    static final int capacity = 128;

    public static void main(String[] args) {
        SensorNode n, n2;
        try {
            n = new SensorNode();
            n2 = new SensorNode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        n.connectAll();

        Util.sleepMS(2000); //HACK


//        {
//            SignalInput i = new SignalInput();
//            i.set(new NoiseSignal(), 2f / 30f/* + tolerance? */);
//            g.add(new SignalView(i).withControls());
//            i.setFPS(20f);
//        }
//        window(new RealTimeLine(n), 800, 800);
        //window(new Dashboard(n), 800, 800);
        window(new GraphPanel(n), 800, 800);
    }

    public static Timeline2D.AnalyzedEvent capture(BufferedImage t, long dur) {

        long now = System.currentTimeMillis();

        return new Timeline2D.AnalyzedEvent(new AspectAlign(Tex.view(t),
                ((float) t.getHeight()) / t.getWidth()), now - dur, now);
    }

    static class GraphPanel extends Bordering {

        public GraphPanel(SensorNode n) {

            @Deprecated float fps = n.udp.getFPS();
            east(new Gridding(new CheckBox("ON").on((t) -> {
                if (!t)
                    n.udp.stop();
                else
                    n.udp.setFPS(fps);
            }), new PushButton("?")));

            Exe.runLater(() -> {
                GraphEdit2D g = new GraphEdit2D();
                center(g);

                Util.sleepMS(2000);

                Exe.runLater(() -> {
                    Surface local = new RealTimeLine(n);
                    Windo ll = g.add(local).sizeRel(0.25f, 0.25f);


                    n.sensors.values().stream().map(s -> new PushButton(s.id)).forEach(ss -> {
                        Windo w = g.add(ss).sizeRel(0.1f, 0.1f);
                        g.addWire(new Wire(local, ss));
                    });
                });
            });

//            Graph2D<UDPeer.UDProfile> themChart = new Graph2D<UDPeer.UDProfile>(
//                    ()->Iterators.concat(udp.them.iterator(), Iterators.singletonIterator(
//                        new UDPeer.UDProfile(udp.me, udp.addr(), 0)
//                    )),
//                    (NodeVis<UDPeer.UDProfile> t) -> {
//                        t.set(new PushButton(t.id.toString()));
//                        t.pri = 0.5f; //TODO 1/latency
//                    });
        }

    }

    static class Dashboard extends Graph2D {
        public Dashboard(SensorNode n) {
            super();
        }
    }

    abstract public static class Sensor {
        public final String id;
        //TODO lat, lon
        public final Timeline2D.FixedSizeEventBuffer<Timeline2D.SimpleEvent> events = new Timeline2D.FixedSizeEventBuffer(capacity);
        //TODO other metrics: last update, avg bps, etc

        protected Sensor(String id) {
            this.id = id;
        }

        public SensorStatus status() {
            return new SensorStatus(id);
        }

        abstract public boolean on();
    }

    public static class SensorStatus implements Serializable {
        public String id;

        public SensorStatus(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static class Sensor1D extends Sensor {

        public final DigitizedSignal i;
        public final SignalInput in;

        public Sensor1D(String id, DigitizedSignal i) {
            super(id);
            this.i = i;

            float audioFPS = 4;
            float granularity = 2;

            in = new SignalInput();
            in.set(i, 1 / audioFPS);
            in.setFPS(audioFPS * granularity);
        }

        @Override
        public boolean on() {
            return in.isRunning(); //TODO better
        }
    }

    public static class AudioSensor extends Sensor1D {
        public AudioSensor(String id, DigitizedSignal i) {
            super(id, i);
        }
    }

    public static class VideoSensor extends Sensor {

        public final VideoSource video;

        public VideoSensor(String id, VideoSource v) {
            super(id);
            this.video = v;
        }

        @Override
        public boolean on() {
            if (video instanceof WebCam) {
                return ((WebCam) video).webcam.isOpen();
            }
            return true;
        }
    }

    public static class SensorNode {
        final static int SHARE_PERIOD_MS = 500;
        final static int MANIFEST_TTL = 2;

        final Map<String, Sensor> sensors = new ConcurrentHashMap();

        final AtomicBoolean reshare = new AtomicBoolean(true);

        private final UDPeer udp;
        private final HttpServer tcp;

        public SensorNode() throws IOException {
            this(0);
        }

        public SensorNode(int port) throws IOException {
            udp = new UDPeer(port) {

                private Every resharing;

                @Override
                protected void starting() {
                    super.starting();
                    resharing = new Every(() -> {
                        if (reshare.compareAndSet(true, false)) {
                            reshare();
                        }
                    }, SHARE_PERIOD_MS);
                }

                @Override
                protected void stopping() {
                    resharing = null;
                    super.stopping();
                }

                @Override
                public boolean next() {
                    resharing.next();
                    return super.next();
                }

                @Override
                protected void accept(Msg m) {
                    switch (m.cmd()) {
                        case 'P':
                            try {
                                send((byte) 'x', manifest(), m.origin()); //send manifest after pinged
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                            break;
//                        case 'p':
//                            //pong
//                            byte[] payload2 = sensors.toString().getBytes(); //HACK
//                            send(new Msg((byte)'x', (byte)1, me, addr, payload2), m.origin());
//                            break;
                        case 'x':
                            //receive manifest
                            try {
                                System.out.println("got: " + Util.fromBytes(m.data(), List.class) + " from " + m.origin());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
//                        default:
//                            System.out.println("recv: " + m);
//                            break;

                    }
                }

            };

            HttpModel h = new HttpModel() {

                @Override
                public void wssOpen(WebSocket ws, ClientHandshake handshake) {
                    ws.send("hi");
                }

                @Override
                public void response(HttpConnection h) {
                    h.respond("");
                }
            };


            HttpServer tcp = new HttpServer(udp.addr(), h);


            this.tcp = tcp;
            udp.setFPS(5);
            tcp.setFPS(5f);

        }

        private void reshare() {
            if (udp.connected()) {
                try {
                    udp.tellSome((byte) 'x', manifest(), MANIFEST_TTL);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }


        private List<SensorStatus> manifest() {
            return sensors.values().stream().filter(Sensor::on).map(Sensor::status).collect(toList());
        }

        public VideoSensor add(Webcam ww) {
            try {
                WebCam w = new WebCam(ww, false);
                return _add(new VideoSensor(ww.getName(), w));
            } catch (WebcamException e) {
                //TODO logger.debug...
                e.printStackTrace();
                return null;
            }
        }

        public Sensor1D add(DigitizedSignal in) {
            return _add(new Sensor1D(in.toString(), in));
        }

        public AudioSensor add(AudioSource in) {
            return _add(new AudioSensor(in.name(), in));
        }

        private <S extends Sensor> S _add(S s) {
            Sensor t = sensors.put(s.id, s);
            reshare.set(true);
            assert (t == null);
            return s;
        }

        /**
         * discover all available peripherals
         */
        public void connectAll() {

            for (AudioSource in : AudioSource.all()) {
                Exe.runLater(() -> {
                    try {
                        in.start();
                        add(in);
                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                    }
                });
            }

            for (Webcam ww : Webcam.getWebcams()) {
                Exe.runLater(() -> {
                    add(ww);
                });
            }
        }
    }

    public static class RealTimeLine extends Gridding {

        private final SensorNode node;
        float viewWindowSeconds = 4;

        public RealTimeLine(SensorNode node) {
            super(VERTICAL);
            this.node = node;

            node.sensors.values().forEach(s -> {
                if (s instanceof VideoSensor)
                    add(((VideoSensor) s), s.events);
                else if (s instanceof AudioSensor)
                    add(((AudioSensor) s), s.events);
                else
                    throw new jcog.TODO();
            });
        }

        public Timeline2D newTrack(String label, Supplier<Surface> control) {
            return newTrack(new PushButton(label).clicked(() -> window(control.get(), 500, 500)));
        }

        public Timeline2D newTrack(Surface label) {
            Timeline2D g = new Timeline2D();

            add(new LabeledPane(label, new Clipped(new Animating(g, () -> {

                long e = System.currentTimeMillis();
                g.setTime(e - Math.round(viewWindowSeconds * 1000), e); //slide window
            }, 0.04f))));

            return g;
        }

        public void add(VideoSensor v) {

        }

        @Deprecated
        public void add(VideoSensor ww, Timeline2D.FixedSizeEventBuffer<Timeline2D.SimpleEvent> ge) {
            WebCam w = ((WebCam) ww.video); //HACK

            Timeline2D g = newTrack(ww.id, () -> new VideoSurface(w));

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
            int freqs = 128;


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

                float fRMS = (float) rms;
                BitmapMatrixView pp = new BitmapMatrixView(1, freqs,
                        //arrayRendererY(dft.apply(a).floatArray())
                        (xIgnored, y) -> {
                            float fy = (float) (f[y]);
                            if (fy == fy) {


                                fy = (float) (fy < 0 ? -Math.sqrt(-fy) : Math.sqrt(fy));

                                float fs = 0.5f + 0.5f * (fy * Util.unitize(fRMS));
                                float fb = 0.05f + 0.95f * fy;
                                return
                                        Draw.colorHSB(fRMS * 2, fs, fb);
                                //Draw.colorBipolar(f[y])
                            } else {
                                //"static" noise
                                //return Draw.colorBipolar((ThreadLocalRandom.current().nextFloat()*2)-1);
                                return 0;
                            }
                        }
                );
                //pp.tex.mipmap(true);
                pp.cellTouch = false;
                //pp.update();
                //p.add(ff);

                Surface p = pp; //new ScaleXY(pp, 1, Util.lerp((float)Math.sqrt(rms), 0.8f, 1f));


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
            int n = Math.round(Math.min(((float) sampleRate) / frames, samplesAtMost));
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
