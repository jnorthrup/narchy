package spacegraph.space2d.widget;

import jcog.exe.Exe;
import jcog.signal.Tensor;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.SpaceGraph;
import spacegraph.audio.Audio;
import spacegraph.audio.sample.SamplePlayer;
import spacegraph.audio.sample.SoundSample;
import spacegraph.audio.speech.TinySpeech;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.chip.AudioOutPort;
import spacegraph.space2d.widget.chip.StringSynthChip;
import spacegraph.space2d.widget.chip.WaveViewChip;
import spacegraph.space2d.widget.meter.WaveBitmap;
import spacegraph.space2d.widget.port.*;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.text.LabeledPane;
import spacegraph.space2d.widget.textedit.TextEdit;
import spacegraph.space2d.widget.windo.Windo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static spacegraph.space2d.container.grid.Gridding.HORIZONTAL;
import static spacegraph.space2d.container.grid.Gridding.VERTICAL;

public class GraphEditTest {

    //    static class TestWallDebugger1 {
//
//        public static void main(String[] args) {
//
//            Wall w = newWallWindow();
//
//
//            w.addAt(new PushButton("X")).pos(RectFloat.XYXY(10, 10, 20, 20));
//            w.addAt(new PushButton("Y")).pos(RectFloat.XYXY(50, 10, 200, 200));
//            w.addAt(new PushButton("Z")).pos(RectFloat.XYXY(100, 10, 200, 200));
//
//            //Windo ww = w.addAt(new PushButton("Y"), 200, 300f);
//            //System.out.println(ww);
//
//        }
//
//
//    }


        public static void main(String[] args) {


            GraphEdit2D s = GraphEdit2D.graphWindow(1000, 1000);

            Surface mux = new Gridding(HORIZONTAL, LabeledPane.the("->", new Gridding(VERTICAL,
                    new Port(),
                    new Port()
            )), LabeledPane.the("->", new Port()));
            s.add(mux).pos(RectFloat.Unit.transform(250, 0, 250));

            Port A = new FloatRangePort(0.5f, 0, 1);
            s.add(A).pos(RectFloat.Unit.transform(250, 250, 250));

            Port B = new FloatRangePort(0.5f, 0, 1);
            s.add(B).pos(RectFloat.Unit.transform(250, 500, 250));

            Port Y = LabeledPort.generic();
            s.add(Y).pos(RectFloat.Unit.transform(250, 750, 250));

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

    public static class TinySpeechTest extends GraphEdit2D {
        {
            //Audio.the().play(TinySpeech.say("eee", 60, 1 ), 1, 1, 0 );

            GraphEdit2D g = graphWindow(1000, 1000);

            {
                TextEdit e = new TextEdit(8, 1).text("a b c d e");
                Port p = new Port();
                e.onChange(p::out);
                g.add(
                        new Bordering(e).set(Bordering.E, p, 0.1f)
                ).sizeRel(0.5f, 0.5f);
            }

            {
                CircularFloatBuffer buffer = new CircularFloatBuffer(2 * TinySpeech.SAMPLE_FREQUENCY);
//            for (int i = 0; i < buffer.capacity()/2; i++) {
//                buffer.write(new float[]{(float) Math.sin(i / 500f)});
//                buffer.write(new float[]{(float) Math.sin(i / 500f)});
//            }

                WaveBitmap wave = new WaveBitmap(buffer, 600, 400);
                AtomicBoolean busy = new AtomicBoolean(false);
                TypedPort p = new TypedPort<>(String.class, new Consumer<String>() {
                    @Override
                    public void accept(String text) {
                        if (busy.compareAndSet(false, true)) {
                            try {
                                buffer.clear();
                                //buffer.write(TinySpeech._say(text)); //TODO
                                wave.update();
                            } finally {
                                busy.set(false);
                            }
                        } else {
                            //pending.setAt(true);
                        }
                    }
                });
                RectFloat r = RectFloat.XYXY((float) 300, (float) 0, (float) 850, (float) 550);
                ((Surface) g.add(
                        new Bordering(wave)
                                .set(Bordering.W, p, 0.1f)
                                .set(Bordering.S, new Gridding(
                                        PushButton.awesome("play").clicked(new Runnable() {
                                            @Override
                                            public void run() {
                                                Audio.the().play(new SamplePlayer(new SoundSample(buffer.data, TinySpeech.SAMPLE_FREQUENCY)));
                                            }
                                        })
                                ), 0.1f)
                )).pos(r);
            }


        }
        public static void main(String[] args) {
            SpaceGraph.window(new TinySpeechTest(), 1000, 1000);

        }
    }

    public static class StringSynthTest {
        public static void main(String[] args) {
            GraphEdit2D g = new GraphEdit2D();
            SpaceGraph.window(g, 1600, 1000);

            Exe.runLater(new Runnable() {
                @Override
                public void run() {
//                RectFloat r5 = RectFloat.XYXY((float) 500, (float) 300, (float) 250, (float) 250);
//                ((Surface) g.add(new SpectrogramChip())).pos(r5);

//                RectFloat r4 = RectFloat.XYXY((float) 400, (float) 400, (float) 250, (float) 250);
//                ((Surface) g.add(new AudioCaptureChip())).pos(r4);

                    RectFloat r2 = RectFloat.XYXY((float) 100, (float) 100, (float) 250, (float) 250);
                    ((Surface) g.add(new StringSynthChip())).pos(r2);

                    RectFloat r1 = RectFloat.XYXY((float) 300, (float) 0, (float) 850, (float) 550);
                    ((Surface) g.add(new WaveViewChip())).pos(r1);

                    RectFloat r = RectFloat.XYXY((float) 300, (float) 230, (float) 450, (float) 350);
                    ((Surface) g.add(new AudioOutPort())).pos(r);


                }
            });


        }

    }

//    public static class SproutTest {
//        public static void main(String[] args) {
//            GraphEdit<Surface> g = new GraphEdit<>(1000, 1000) {
//                @Override
//                protected void starting() {
//                    super.starting();
//
//                    FloatRangePort f = new FloatRangePort(50f, 1, 100);
//                    Windo x = addAt(new LabeledPane("ms", f));
//                    x.pos(100, 100, 400, 400);
//                    Windo xPlus = sprout(x, new PushButton("+").click(() -> f.f.addAt(0.1f)), 0.15f);
//                    Windo xMinus = sprout(x, new PushButton("-").click(() -> f.f.subtract(0.1f)), 0.15f);
//
//                    Gridding tick = new PulseChip();
//                    addAt(tick).pos(0, 0, 200, 200);
//
//                    {
//
//                        AtomicBoolean state = new AtomicBoolean(false);
//                        Port out = LabeledPort.generic();
//                        Port in = new Port((z) -> {
//                            if (z == TRUE) {
//                                out.out(state.getAndSet(!state.getOpaque())); //TODO really make atomic
//                            }
//                        });
//
//                        Gridding inverter = new Gridding(new LabeledPane("trigger", in), new LabeledPane("state", out)) {
//                            @Override
//                            protected void paintIt(GL2 gl, SurfaceRender r) {
//
//                                if (state.getOpaque()) {
//                                    gl.glColor4f(0, 0.5f, 0, 0.5f);
//                                } else {
//                                    gl.glColor4f(0.5f, 0f, 0, 0.5f);
//                                }
//
//                                Draw.rect(bounds, gl);
//                            }
//                        };
//                        addAt(inverter).pos(70, 70, 140, 140);
//
//                        //addAt(new SwitchChip(4)).pos(170, 170, 240, 240);
//                        addAt(new FunctionSelectChip<>(
//                                Double.class, Double.class,
//                                Map.of("sin", Math::sin, "cos", Math::cos))).pos(170, 170, 240, 240);
//                    }
//
//                    //addAt(LabeledPort.generic()).pos(10, 10, 50, 50);
//                }
//            };
//            SpaceGraph.window(g, 1000, 1000);
//
//
//        }
//
//    }

    public static class AutoAdaptTest {

        public static void main(String[] args) {
            GraphEdit2D w = GraphEdit2D.graphWindow(1000, 1000);
            w.add(new IntPort()).posRel(0.5f,0.5f,0.05f,0.05f);
            w.add(new IntPort()).posRel(0.5f,0.5f,0.05f,0.05f);
            w.add(new IntPort()).posRel(0.5f,0.5f,0.05f,0.05f);
            w.add(new IntPort()).posRel(0.5f,0.5f,0.05f,0.05f);

            w.add(new TypedPort<>(Tensor.class)).posRel(0.5f,0.5f,0.05f,0.05f);
        }
    }

    public static class SproutPortTest {

        public static void main(String[] args) {
            GraphEdit2D w = GraphEdit2D.graphWindow(1000, 1000);

            //w.addBox(0f, 0f, 0.2f, 0.2f, 0.01f);

            Windo x = w.add(
                    //new OKSurface("NOT OK")
                    new XYSlider().chip()
            ).posRel(0.5f, 0.5f, 0.1f, 0.1f);

            for (int i = 0; i < 3; i++) {
                Windo y = w.addWeak(new TogglePort()).posRel(x, -0.5f, 0.5f, 0.05f, 0.05f);

                w.addWire(new Wire(x.the(), y.the()));
            }


        }
    }


}