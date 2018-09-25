package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.exe.Loop;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import spacegraph.SpaceGraph;
import spacegraph.audio.Audio;
import spacegraph.audio.Sound;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.sample.SamplePlayer;
import spacegraph.audio.sample.SoundSample;
import spacegraph.audio.speech.TinySpeech;
import spacegraph.audio.synth.string.GuitarHero;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.space2d.widget.meter.WaveView;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.windo.*;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Boolean.TRUE;
import static spacegraph.space2d.container.Gridding.HORIZONTAL;
import static spacegraph.space2d.container.Gridding.VERTICAL;

public class WallTest {

    static GraphEdit newWallWindow() {
        GraphEdit w = new GraphEdit(RectFloat2D.X0Y0WH(0,0,1000, 900));
        SpaceGraph.window(
                //new Bordering(w).borderSize(Bordering.S, 0.25f).south(w.debugger()),
                w,
                1000, 900);
        return w;
    }

    static class TestWallDebugger1 {

        public static void main(String[] args) {

            Wall w = newWallWindow();


            w.add(new PushButton("X")).pos(RectFloat2D.XYXY(10, 10, 200, 200));
            w.add(new PushButton("Y")).pos(RectFloat2D.XYXY(50, 10, 200, 200));
            w.add(new PushButton("Z")).pos(RectFloat2D.XYXY(100, 10, 200, 200));

            //Windo ww = w.add(new PushButton("Y"), 200, 300f);
            //System.out.println(ww);

        }


    }

    static class SwitchedSignal {

        public static void main(String[] args) {

            Wall s = newWallWindow();


            Port A = new Port();
            Windo a = s.add(A).pos(RectFloat2D.Unit.transform(500, 250, 250));


            Port B = LabeledPort.generic();
            Windo b = s.add(B).pos(RectFloat2D.XYWH(+1, 0, 0.25f, 0.25f).scale(500));

            TogglePort AB = new TogglePort();
            s.add(AB).pos(RectFloat2D.XYWH(0, 0, 0.25f, 0.25f).scale(500));

            Loop.of(() -> {
                A.out(Texts.n4(Math.random()));
            }).setFPS(0.3f);
        }

    }
    public static class Box2DTest_FloatMux {

        public static void main(String[] args) {


            Wall s = newWallWindow();

            Surface mux = new Gridding(HORIZONTAL, new LabeledPane("->", new Gridding(VERTICAL,
                    new Port(),
                    new Port()
            )), new LabeledPane("->", new Port()));
            s.add(mux).pos(RectFloat2D.Unit.transform(250, 0, 250));

            Port A = new FloatRangePort(0.5f, 0, 1);
            s.add(A).pos(RectFloat2D.Unit.transform(250, 250, 250));

            Port B = new FloatRangePort(0.5f, 0, 1);
            s.add(B).pos(RectFloat2D.Unit.transform(250, 500, 250));

            Port Y = LabeledPort.generic();
            s.add(Y).pos(RectFloat2D.Unit.transform(250, 750, 250));

        }
    }

    //    public static class Box2DTest_ObjGraph {
//        public static void main(String[] args) {
//            GraphEdit s = SpaceGraph.wall(800, 800);
//
//            ObjectGraph og = new ObjectGraph(2, s) {
//
//                @Override
//                public boolean includeValue(Object v) {
//                    return true;
//                }
//
//                @Override
//                public boolean includeClass(Class<?> c) {
//                    return true;
//                }
//
//                @Override
//                public boolean includeField(Field f) {
//                    return true;
//                }
//            };
//
//            og.forEachNode(n -> {
//                GraphEdit.PhyWindow oo = s.put(new PushButton(n.id().getClass().toString()), RectFloat2D.XYWH(0, 0, 1, 1));
//            });
//
//
//
//
//
//
//        }
//    }
//
//
//    public static class Box2DTest_ProtoWidget {
//
//        public static void main(String[] args) {
//            GraphEdit s = SpaceGraph.wall(800, 800);
//
//            s.put(
//                    new WizardFrame( new ProtoWidget() ),
//                    1, 1);
//
//        }
//    }

    static class TinySpeechTest {
        public static void main(String[] args) {
            //Audio.the().play(TinySpeech.say("eee", 60, 1 ), 1, 1, 0 );

            GraphEdit<Surface> g = new GraphEdit<>(1000, 1000);

            {
                TextEdit e = new TextEdit("a b c d e", true);
                Port p = new Port();
                e.on(p::out);
                g.add(
                        new Bordering(e).set(Bordering.E, p, 0.1f)
                ).pos(0, 0, 250, 250);
            }

            {
                CircularFloatBuffer buffer = new CircularFloatBuffer(2*TinySpeech.SAMPLE_FREQUENCY);
//            for (int i = 0; i < buffer.capacity()/2; i++) {
//                buffer.write(new float[]{(float) Math.sin(i / 500f)});
//                buffer.write(new float[]{(float) Math.sin(i / 500f)});
//            }

                WaveView wave = new WaveView(buffer, 600, 400);
                AtomicBoolean busy = new AtomicBoolean(false);
                Port p = new Port();
                p.on((String text) -> {
                    if (busy.compareAndSet(false, true)) {
                        try {
                            buffer.clear();
                            buffer.write( TinySpeech.say(text, 80, 1f) );
                            wave.update();
                        } finally {
                            busy.set(false);
                        }
                    } else {
                        //pending.set(true);
                    }
                });
                g.add(
                        new Bordering(wave)
                                .set(Bordering.W, p, 0.1f)
                                .set(Bordering.S, new Gridding(
                                        PushButton.awesome("play").click(()->{
                                            Audio.the().play(new SamplePlayer(new SoundSample(buffer.data, TinySpeech.SAMPLE_FREQUENCY)));
                                        })
                                ), 0.1f)
                ).pos(300, 0, 850, 550);
            }

            SpaceGraph.window(g, 1000, 1000);

        }
    }

    public static class StringSynthTest {
        public static void main(String[] args) {
            GraphEdit<Surface> g = new GraphEdit<>(1000, 1000);
            GuitarHero h = new GuitarHero();
            h.amp(1f);

            {
//                AtomicReference<CircularFloatBuffer> targetBuffer = new AtomicReference(null);
                PushButton e = new PushButton("p");

//                e.click(()->{
//                    Synth
//                })
                Port p = new Port((ObjectIntPair<float[]> mixTarget)->{
                    h.next(mixTarget.getOne());
                });
                g.add(
                        new Bordering(new Gridding(e, new FloatSlider(h.amp(), 0, 8f ).on(a->{
                            h.amp(a);
                        }))).set(Bordering.E, p, 0.1f)
                ).pos(0, 0, 250, 250);

            }

            {
                CircularFloatBuffer buffer = new CircularFloatBuffer(44100 * 2);
//            for (int i = 0; i < buffer.capacity()/2; i++) {
//                buffer.write(new float[]{(float) Math.sin(i / 500f)});
//                buffer.write(new float[]{(float) Math.sin(i / 500f)});
//            }

                WaveView wave = new WaveView(buffer, 600, 400);
                Port p = new Port();


//                p.on((String text) -> {
//                    if (busy.compareAndSet(false, true)) {
//                        try {
//                            buffer.clear();
//                            buffer.write( TinySpeech.say(text, 60, 1.5f) );
//                            wave.update();
//                        } finally {
//                            busy.set(false);
//                        }
//                    } else {
//                        //pending.set(true);
//                    }
//                });
                Sound playing = Audio.the().play(new SoundProducer() {
                    {
                        int c = buffer.capacity();
                        for (int i = 0; i < c; i++) {
                            buffer.write(new float[] { 0 });
                        }
                    }
                    @Override
                    public void read(float[] buf, int readRate) {

                        if (p.active()) {
                            p.out(PrimitiveTuples.pair(buf /* TODO buffer mix command object */, readRate));

                            buffer.flush(buf.length);
                            buffer.write(buf);

                            wave.updateLive();
                        }
                    }

                    @Override
                    public void skip(int samplesToSkip, int readRate) {
                        //TODO
                        //buffer.skip(..);
                        //System.out.println("skip " + samplesToSkip);
                    }

                    @Override
                    public boolean isLive() {
                        return true;
                    }

                    @Override
                    public void stop() {

                    }
                });
                //playing.volume(0); //initially off

                g.add(
                        new Bordering(wave)
                                .set(Bordering.W, p, 0.1f)
                ).pos(300, 0, 850, 550);
            }

            SpaceGraph.window(g, 1000, 1000);

        }
    }

    public static class SproutTest {
        public static void main(String[] args) {
            GraphEdit<Surface> g = new GraphEdit<>(1000, 1000) {
                @Override
                protected void starting() {
                    super.starting();

                    FloatRangePort f = new FloatRangePort(50f, 1, 100);
                    Windo x = add(new LabeledPane("ms", f));
                    x.pos(100, 100, 400, 400);
                    Windo xPlus = sprout(x, new PushButton("+").click(()->f.f.add(0.1f)), 0.15f);
                    Windo xMinus = sprout(x, new PushButton("-").click(()->f.f.subtract(0.1f)), 0.15f);

                    Port periodMS = new Port();
                    //TODO phase
                    Port pulse = new Port();
                    Gridding tick = new Gridding(
                        new LabeledPane("period(MS)", periodMS),
                        new LabeledPane("trigger", pulse)
                    ) {
                        float p = Float.NaN;
                        Loop loop = null;
                        AtomicBoolean busy = new AtomicBoolean();

                        protected void tick() {
                            if (busy.compareAndSet(false, true)) {
                                try {
                                    pulse.out(TRUE);
                                } finally {
                                    busy.set(false);
                                }
                            }
                        }

                        @Override
                        protected void stopping() {
                            super.stopping();
                            if (loop!=null) {
                                loop.stop();
                                loop = null;
                            }
                        }

                        {
                            periodMS.on(x ->{
                                synchronized (this) {

                                    if (x instanceof Number) {
                                        p = ((Number) x).floatValue();
                                    } else {
                                        p = Float.NaN;
                                    }

                                    if (p > 0.5f) {
                                        if (loop == null)
                                            loop = Loop.of(this::tick);
                                        loop.setPeriodMS(Math.round(p));
                                    }

                                    if ((p!=p || p < 0.5f)) {
                                        if (loop != null)
                                            loop.stop();
                                    }

                                }
                            });
                        }
                    };
                    add(tick).pos(0, 0, 200, 200);

                    {

                        AtomicBoolean state = new AtomicBoolean(false);
                        Port out = LabeledPort.generic();
                        Port in = new Port((z)->{
                            if (z == TRUE) {
                                out.out(state.getAndSet(!state.getOpaque())); //TODO really make atomic
                            }
                        });

                        Gridding inverter = new Gridding(new LabeledPane("trigger", in), new LabeledPane("state", out)) {
                            @Override
                            protected void paintBelow(GL2 gl, SurfaceRender r) {

                                if (state.getOpaque()) {
                                    gl.glColor4f(0, 0.5f, 0, 0.5f);
                                } else {
                                    gl.glColor4f(0.5f, 0f, 0, 0.5f);
                                }

                                Draw.rect( bounds, gl);
                            }
                        };
                        add(inverter).pos(70, 70, 140, 140);
                    }

                    //add(LabeledPort.generic()).pos(10, 10, 50, 50);
                }
            };
            SpaceGraph.window(g, 1000, 1000);



        }
    }
}
