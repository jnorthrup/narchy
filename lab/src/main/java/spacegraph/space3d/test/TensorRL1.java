//package spacegraph.space3d.test;
//
//import jcog.learn.Agent;
//import jcog.learn.Autoencoder;
//import jcog.learn.ql.HaiQ;
//import jcog.math.FloatRange;
//import jcog.math.IntIntToObjectFunc;
//import jcog.math.IntRange;
//import jcog.math.random.XoRoShiRo128PlusRandom;
//import jcog.math.tensor.Tensor;
//import jcog.math.tensor.TensorFunc;
//import jcog.math.tensor.TensorLERP;
//import jcog.signal.Bitmap2D;
//import nars.NAR;
//import nars.NARS;
//import nars.agent.NAgent;
//import nars.agent.util.RLBooster;
//import nars.experiment.PoleCart;
//import spacegraph.SpaceGraph;
//import spacegraph.space2d.container.Scale;
//import spacegraph.space2d.container.Splitting;
//import spacegraph.space2d.container.grid.Gridding;
//import spacegraph.space2d.widget.meta.AutoSurface;
//import spacegraph.space2d.widget.meter.AutoUpdateMatrixView;
//import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
//import spacegraph.space2d.widget.slider.XYSlider;
//import spacegraph.space2d.widget.text.Label;
//import spacegraph.space2d.widget.text.LabeledPane;
//import spacegraph.space2d.widget.windo.Dyn2DSurface;
//import spacegraph.space2d.widget.windo.Port;
//
//import java.util.Random;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Function;
//
//public class TensorRL1 {
//
//    public static void main(String[] args) {
//
//
//        Dyn2DSurface p = SpaceGraph.wall(1200, 1000);
//
//
//
//        {
//            p.put(new EnvChip(
//                   PoleCart::new
//
//
//
//
//            ), 1.75f, 1);
//        }
//
//
//        p.put(new AutoencoderChip(), 0.25f, 1);
//        p.put(new AutoencoderChip(), 0.25f, 1);
//
//
//
//        p.put(new AgentChip(
//                HaiQ::new
//
//        ), 1, 1);
//
//    }
//
//    public static class EnvChip extends Gridding {
//        public EnvChip(Function<NAR, NAgent> env) {
//            super();
//
//            NAR n = new NARS.DefaultNAR(0, true).get();
//
//            NAgent a = env.apply(n);
//
//            a.curiosity.set(0f);
//
//            n.startFPS(25f);
//
//            final Port rewardPort = new Port();
//            final Port sensePort = new Port();
//            final DummyAgent[] agent = new DummyAgent[]{new DummyAgent(1, 1)};
//
//            RLBooster adapter = new RLBooster(a, (int ii, int aa) -> {
//                return agent[0] = new DummyAgent(ii, aa) {
//                    @Override
//                    public int act(float r, float[] nextObservation) {
//                        rewardPort.out(r);
//                        sensePort.out(nextObservation);
//                        return super.act(r, nextObservation);
//                    }
//                };
//            }, 1) {
//                @Override
//                public void accept(NAR ignored) {
//                    a.sensorCam.forEach(c -> {
//                        c.update();
//                        c.input();
//                    });
//                    super.accept(ignored);
//                }
//            };
//
//            set(
//                    new LabeledPane(a.getClass().getSimpleName(), new AutoSurface<>(a)),
//                    new LabeledPane("sense", sensePort),
//                    new LabeledPane("reward", rewardPort),
//                    new LabeledPane("act", new Port()
//                        .specify(
//                            ()->new IntRange(-1, 0, adapter.actions()))
//                        .on((IntRange i) -> {
//                            assert(i.min == 0 && i.max == adapter.actions());
//                            agent[0].nextAct = i.intValue();
//                        }))
//            );
//
//        }
//
//        private static class DummyAgent extends Agent {
//
//            public float[] sense;
//            public int nextAct;
//            public float reward;
//
//            public DummyAgent(int ii, int aa) {
//                super(ii, aa);
//                sense = new float[1];
//                nextAct = 0;
//                reward = 0;
//            }
//
//            @Override
//            public int act(float reward, float[] nextObservation) {
//                this.sense = nextObservation;
//                this.reward = reward;
//
//                return nextAct;
//            }
//        }
//    }
//
//    public static class SketchChip extends Splitting {
//        final Sketch2DBitmap bmp = new Sketch2DBitmap(4, 4);
//        private final Port outPort;
//
//        public SketchChip() {
//            super();
//            split(0.1f);
//            outPort = new Port() {
//                float[] a = new float[16];
//
//                @Override
//                public void prePaint(int dtMS) {
//                    super.prePaint(dtMS);
//
//                    for (int i = 0; i < bmp.pix.length; i++) {
//                        a[i] = Bitmap2D.decodeRed(bmp.pix[i]);
//                    }
//                    out(a);
//                }
//            };
//            set(bmp/*.state(Widget.META)*/, outPort);
//        }
//    }
//
//    static void noiseChip(Dyn2DSurface p) {
//        {
//            final Random rng = new XoRoShiRo128PlusRandom(1);
//            final TensorFunc randomVector = Tensor.randomVectorGauss(16, 0, 1, rng);
//            final FloatRange lerpRate = new FloatRange(0.01f, 0, 1f);
//            final TensorLERP lerpVector = new TensorLERP(randomVector, lerpRate);
//
//            p.put(new Gridding(0.25f,
//
//
//
//                            new LabeledPane("rng", new AutoUpdateMatrixView(
//                                    lerpVector.data
//                            )),
//                            new LabeledPane("lerp", new XYSlider().on((x, y) -> {
//                                lerpRate.set(x);
//                            })),
//                            new LabeledPane("out", new Port() {
//                                @Override
//                                public void prePaint(int dtMS) {
//                                    super.prePaint(dtMS);
//
//                                    lerpVector.update();
//                                    out(lerpVector.data);
//                                }
//                            })),
//                    0.5f, 0.5f);
//        }
//
//    }
//
//    private static class AgentChip extends Gridding {
//
//        public final Port INPUT, REWARD, ACTION;
//        private float reward = 0;
//
//        float[] in = new float[1];
//
//        Agent agent;
//
//        private int lastAction = 0;
//
//
//        private final IntIntToObjectFunc<Agent> builder;
//
//        class Config {
//
//            int actions = 1;
//
//            protected synchronized void reset(boolean resetAgent, boolean resetView) {
//
//                if (resetAgent) {
//                    agent = builder.apply(in.length, actions);
//                }
//
//                if (resetView) {
//                    view.set(
//                            new Gridding(
//                                    new LabeledPane(agent.getClass().getSimpleName(),
//                                            new AutoSurface<>(agent)),
//                                    new AutoUpdateMatrixView(in)
//                            ));
//
//                    if (agent instanceof HaiQ) {
//
//                        view.addAll(
//                                new AutoUpdateMatrixView((((HaiQ) agent).q)),
//                                new AutoUpdateMatrixView((((HaiQ) agent).et))
//                        );
//                    }
//                }
//            }
//
//            public void update(float[] i) {
//                if (in.length != i.length || config.actions != agent.actions) {
//                    in = i;
//                    reset(true, true);
//                } else if (i != in) {
//                    in = i;
//                    reset(false, true);
//                }
//
//            }
//        }
//
//        final Config config = new Config();
//
//        final Gridding view = new Gridding();
//
//
//        public AgentChip(IntIntToObjectFunc<Agent> builder) {
//            super();
//            this.builder = builder;
//            config.reset(true, true);
//
//            Label actionLabel = new Label("");
//            ACTION = new Port().obey(((IntRange p)->{
//                assert(p.min == 0);
//                config.actions = p.max;
//            }));
//            ACTION.update(() -> {
//                actionLabel.text(String.valueOf(lastAction));
//            }).content(new Scale(actionLabel, 0.5f));
//
//
//            set(
//                    new AutoSurface<>(config),
//
//                    new LabeledPane("reward", REWARD = new Port().on((i) -> {
//                        AgentChip.this.reward = (float) i;
//
//                        int a = agent.act(reward, in);
//
//                        lastAction = a;
//
//                        ACTION.out(new IntRange(a, 0, config.actions));
//                    })),
//
//                    new LabeledPane("sense", INPUT = new Port((float[] i) -> {
//                        config.update(i);
//                    })),
//
//                    view,
//
//                    new LabeledPane("act", ACTION)
//
//            );
//
//
//        }
//    }
//
//    public static class AutoencoderChip extends Gridding {
//
//        Autoencoder ae;
//
//        private final Random rng = new XoRoShiRo128PlusRandom(1);
//
//        class Config {
//            public final FloatRange learningRate = new FloatRange(0.05f, 0, 1f);
//            public final FloatRange noiseLevel = new FloatRange(0.005f, 0, 0.1f);
//            public final IntRange outputs = new IntRange(2, 2, 64);
//            public final AtomicBoolean sigmoidIn = new AtomicBoolean(false);
//            public final AtomicBoolean sigmoidOut = new AtomicBoolean(true);
//
//        }
//
//        final Config config = new Config();
//
//        final Gridding view = new Gridding();
//
//        protected synchronized void reset(int inputs) {
//            Autoencoder ae = this.ae = new Autoencoder(inputs, config.outputs.intValue(), rng);
//            view.set(
//                    new AutoUpdateMatrixView(ae.x),
//                    new Gridding(
//                            new AutoUpdateMatrixView(ae.hbias),
//                            new AutoUpdateMatrixView(ae.vbias)
//                    ),
//                    new AutoUpdateMatrixView(ae.W),
//                    new AutoUpdateMatrixView(ae.y),
//                    new AutoUpdateMatrixView(ae.z)
//            );
//
//
//        }
//
//        public AutoencoderChip() {
//            super();
//
//            reset(1);
//
//            set(
//                    new LabeledPane("in", new Port((float[] x) -> {
//
//                        if (ae.x.length != x.length || ae.outputs() != config.outputs.intValue()) {
//                            reset(x.length);
//                        }
//
//                        ae.put(x, config.learningRate.floatValue(), config.noiseLevel.floatValue(), 0f,
//                                config.sigmoidIn.get(),
//                                config.sigmoidOut.get());
//                    })),
//
//                    new LabeledPane("Autoencode", new AutoSurface(config)),
//
//                    view,
//
//                    new LabeledPane("out", new Port()
//                            .update((p) -> p.out(ae.y))
//                    )
//            );
//
//        }
//    }
//
//}
