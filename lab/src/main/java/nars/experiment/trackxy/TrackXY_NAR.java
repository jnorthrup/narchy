package nars.experiment.trackxy;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.LivePredictor;
import jcog.learn.ql.DQN2;
import jcog.math.FloatNormalized;
import jcog.test.control.TrackXY;
import jcog.tree.rtree.rect.RectFloat;
import nars.*;
import nars.agent.FrameTrigger;
import nars.agent.util.RLBooster;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
import nars.exe.UniExec;
import nars.gui.LSTMView;
import nars.gui.NARui;
import nars.index.concept.CaffeineIndex;
import nars.op.stm.STMLinkage;
import nars.sensor.Bitmap2DSensor;
import nars.task.DerivedTask;
import nars.time.clock.CycleTime;
import nars.video.VectorSensorView;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.impl.block.factory.Comparators;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.video.Draw;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static jcog.Texts.n4;
import static nars.Op.GOAL;
import static nars.Op.IMPL;
import static spacegraph.SpaceGraph.window;

public class TrackXY_NAR extends NAgentX {

    public static final int derivationStrength = 32;
    static boolean
            nars = true, rl = !nars,
            sourceNumerics = true, targetNumerics = false,
            targetCam = true,
            gui = true;
    static int
            W = 4, H = 4;
            //W = 3, H = 1;
            //W = 5, H = 1;
    static int dur = 7;
    static float camResolution = 0.1f;
    static int volMax = 9;
    final Bitmap2DSensor cam;
    private final TrackXY track;

    private float rewardSum = 0;
    static int experimentTime = 20000;


    final public AtomicBoolean trainer = new AtomicBoolean(false);
    private final boolean alwaysTrain = false;

    //final public AtomicBoolean log = new AtomicBoolean(true);


    protected TrackXY_NAR(NAR nar, int W, int H) {
        super("trackXY",
                //FrameTrigger.cycles(W*H*2),
                FrameTrigger.durs(1),
                //FrameTrigger.fps(1),
                nar);

        this.track = new TrackXY(W, H);

        assert(sourceNumerics | targetNumerics | targetCam);

        if (sourceNumerics) {
            senseNumber($.inh("sx", id), new FloatNormalized(() -> track.cx, 0, W));
            if (H > 1)
                senseNumber($.inh("sy", id), new FloatNormalized(() -> track.cy, 0, H));
        }


        if (targetNumerics) {
            senseNumber($.inh("tx", id), new FloatNormalized(() -> track.tx, 0, W));
            if (H > 1)
                senseNumber($.inh("ty", id), new FloatNormalized(() -> track.ty, 0, H));
        }

        if (targetCam) {
            this.cam = addCamera(new Bitmap2DSensor<>(id /* (Term) null*/, track.grid, nar));
            cam.resolution(camResolution);
        } else {
            this.cam = null;
        }

        //actionPushButton();
        actionPushButtonMutex();
        //actionSwitch();
        //actionTriState();


        if (alwaysTrain) {
            curiosity.enable.set(false);
            onFrame(x -> {
                track.act();
            });
        } else {
            rewardNormalized("reward", -1, 1, () -> {
                float r = track.act();
                if (r == r)
                    rewardSum += r;
                return r;
            });
        }

        track.randomize();

//        Param.DEBUG = true;
//        nar().log(new FilterPrintStream(System.out) {
//
//            @Override
//            public void print(String s) {
//                super.print(s);
//                Util.sleepMS(200);
//            }
//        }, (t)->log.getOpaque() /*&& t instanceof Task && ((Task)t).isGoal()*/);


        onFrame(()->{
            long now = nar.time();
           if (trainer.getOpaque()) {
                if (track.ty < track.cy) {
                    nar().want(0.1f, $.inh("down", id), now, now+dur, 1f, 0.02f);
                } else if (track.ty > track.cy) {
                    nar().want(0.1f, $.inh("up", id), now, now+dur, 1f, 0.02f);
                }
               if (track.tx < track.cx) {
                   nar().want(0.1f, $.inh("left", id), now, now+dur, 1f, 0.02f);
               } else if (track.tx > track.cx) {
                   nar().want(0.1f, $.inh("right", id), now, now+dur, 1f, 0.02f);
               }
           }
        });
    }


    public static void main(String[] args) {

//        boolean rl = false;



        //8;
        //2 * (W * H) /* to allow pixels to be read at the rate of 1 pixel per cycle */;

        NARS nb = new NARS.DefaultNAR(0, true)
                .exe(new UniExec())
                .time(new CycleTime().dur(dur))
                .index(
                        new CaffeineIndex(4 * 1024 * 10)
                        //new HijackConceptIndex(4 * 1024, 4)
                );


        NAR n = nb.get();

//        Param.DEBUG = true;
//        n.run(10); //skip:
//        n.onTask((t)->{
//            if (!t.isEternal() && t.range() > n.time()) {
//                System.err.println("long-range:\n" + t.proof());
//            }
//        });

//        n.beliefConfDefault.set(0.5f);
//        n.goalConfDefault.set(0.5f);


//        n.goalPriDefault.set(0.99f);
//        n.beliefPriDefault.set(0.01f);
//        n.questionPriDefault.set(0.01f);
//        n.questPriDefault.set(0.01f);


//        n.freqResolution.set(0.1f);
//        n.confResolution.set(0.05f);


        TrackXY_NAR a = new TrackXY_NAR(n, W, H);

        //n.freqResolution.set(0.04f);

        n.termVolumeMax.set(volMax);
        n.timeResolution.set(Math.max(1,dur/2));

        if (rl) {
            RLBooster rlb = new RLBooster(a,

                    DQN2::new,
                    //HaiQae::new,
                    //HaiQ::new,

                    true);
            a.curiosity.enable.set(false);

            window(
                    new LSTMView(
                            ((LivePredictor.LSTMPredictor) ((DQN2) rlb.agent).valuePredict).lstm.agent
                    ), 800, 800
            );

//            window(new Gridding(
//                Stream.of(((DQN2) (rlb.agent)).valuePredict.layers).map(
//                        l -> {
//
//                            BitmapMatrixView i = new BitmapMatrixView(l.input);
//                            BitmapMatrixView w = new BitmapMatrixView(l.weights);
//                            BitmapMatrixView o = new BitmapMatrixView(l.output);
//
//                            a.onFrame(i::update);
//                            a.onFrame(w::update);
//                            a.onFrame(o::update);
//
//                            return new Gridding(i, w, o);
//                        }
//                ).collect(toList()))
//            , 800, 800);
        }
        if (nars) {


            Deriver d = new BatchDeriver(Derivers.nal(n,
                    //6, 8
                    1, 8
                    //2, 8
                    //, "motivation.nal"
            )) {
//                    @Override
//                    public float puncFactor(byte conclusion) {
//                        return conclusion == GOAL ? 1 : 0.01f; //super.puncFactor(conclusion);
//                    }
            };


            ((BatchDeriver) d).conceptsPerIteration.set(derivationStrength);


            new STMLinkage(n, 1) {
                @Override
                public boolean keep(Task newEvent) {
                    return newEvent.isGoal();
                }
            };

//            ConjClustering cjB = new ConjClustering(n, BELIEF,
//                    //x -> true,
//                    Task::isInput,
//                    2, 8);


//            window(new Gridding(
//                    new AutoSurface(d),
//                    new AutoSurface(cjB)
//
//            ), 400, 300);

            //n.log();

            Param.DEBUG = true;
            n.onTask(tt -> {
                if (tt instanceof DerivedTask && tt.isGoal()) {
                    System.out.println(tt.proof());
                }
            }, GOAL);

        }


        if (gui) {
            n.runLater(() -> {
                window(
                        NARui.agent(a)
                        , 400, 400);
                window(new Gridding(NARui.top(n), new ObjectSurface(a.track)), 800, 250);

//            NARui.agentWindow(t);
                if (a.cam != null) {
                    window(new VectorSensorView(a.cam, n) {
                        @Override
                        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                            super.paint(gl, surfaceRender);
                            RectFloat at = cellRect(a.track.cx, a.track.cy, 0.5f, 0.5f);
                            gl.glColor4f(1, 0, 0, 0.9f);
                            Draw.rect(at.move(x(), y(), 0.01f), gl);
                        }
                    }.withControls(), 800, 800);
                }
            });
        }

//        a.onFrame(() -> {
//            Util.sleepMS(10);
//        });

        int epoch = 200;
        DescriptiveStatistics rh = new DescriptiveStatistics(epoch * 4);
        a.onFrame(() -> {
            float r = a.reward();
            rh.addValue(r);
            if (n.time() % epoch == 0) {
                System.out.println(n4(rh.getMean()));
            }

//            long now = n.time();
//            if (now % epoch == 0) {
//                System.out.println(
//                        //"reward mean: " +
//                        a.rewardSum / epoch);
//                a.rewardSum = 0;
//            }
        });

        n.run(experimentTime);


        //printGoals(n);
        //printImpls(n);

        //n.stats(System.out);
        //n.conceptsActive().forEach(System.out::println);

        n.tasks().forEach(System.out::println);
    }


    public static void printGoals(NAR n) {
        int dur = n.dur();
        long now = n.time();
        List<Task> l = n.tasks(false, false, true, false)
                .sorted(Comparators.byFloatFunction(tt -> -tt.evi(now, dur)))
                .collect(toList());
        l.forEach(System.out::println);
        System.out.println();
    }

    public static void printImpls(NAR n) {
        int dur = n.dur();
        long now = n.time();
        List<Task> l = n.tasks(true, false, false, false)
                .filter(x -> x.op() == IMPL)
                .sorted(Comparators.byFloatFunction(tt -> -tt.evi(now, dur)))
                .collect(toList());
        l.forEach(System.out::println);
        System.out.println();
    }


//        private void actionSwitch() {
//            SwitchAction s = new SwitchAction(nar, (a) -> {
//                float controlSpeed = track.controlSpeed.floatValue();
//                switch (a) {
//                    case 0: {
//
//                        float dy = -1;
//                        float py = sy;
//                        sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
//                        return !Util.equals(py, sy, 0.01f);
//                    }
//                    case 1: {
//
//                        float dy = +1;
//                        float py = sy;
//                        sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
//                        return !Util.equals(py, sy, 0.01f);
//                    }
//                    case 2: {
//
//                        float dx = -1;
//                        float px = sx;
//                        sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
//                        return !Util.equals(px, sx, 0.01f);
//                    }
//                    case 3: {
//
//                        float dx = +1;
//                        float px = sx;
//                        sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
//                        return !Util.equals(px, sx, 0.01f);
//                    }
//                    case 4: {
//
//                        return true;
//                    }
//                    default:
//                        throw new UnsupportedOperationException();
//                }
//            }, INH.the($.the("up"), id),
//                    INH.the($.the("down"), id),
//                    INH.the($.the("left"), id),
//                    INH.the($.the("right"), id),
//                    INH.the($.the("stay"), id)
//            );
//            addSensor(s);
//            SpaceGraph.window(NARui.beliefCharts(s.sensors, nar), 300, 300);
//        }

    private void actionTriState() {

        if (track.grid.height() > 1) {
            actionTriState($.p($.the("Y"), id), (dy) -> {
                float py = track.cy;
                track.cy = Util.clamp(track.cy + track.controlSpeed.floatValue() * dy, 0, track.grid.height() - 1);
                return !Util.equals(py, track.cy, 0.01f);
            });
        }

        actionTriState($.p($.the("X"), id), (dx) -> {
            float px = track.cx;
            track.cx = Util.clamp(track.cx + track.controlSpeed.floatValue() * dx, 0, track.grid.width() - 1);
            return !Util.equals(px, track.cx, 0.01f);
        });
    }


    private void actionPushButton() {


        actionPushButton($.inh("right", id), (b) -> {
            if (b)
                track.cx = Util.clamp(track.cx + track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        });
        actionPushButton($.inh("left", id), (b) -> {
            if (b)
                track.cx = Util.clamp(track.cx - track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        });

        if (track.grid.height() > 1) {
            actionPushButton($.inh("up", id), (b) -> {
                if (b)
                    track.cy = Util.clamp(track.cy + track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            });
            actionPushButton($.inh("down", id), (b) -> {
                if (b)
                    track.cy = Util.clamp(track.cy - track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            });
        }

    }

    private void actionPushButtonMutex() {
        if (track.grid.height() > 1) {
            actionPushButtonMutex($.inh("up", id), $.inh("down", id), (b) -> {
                if (b)
                    track.cy = Util.clamp(track.cy + track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            }, (b) -> {
                if (b)
                    track.cy = Util.clamp(track.cy - track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            });
        }

        actionPushButtonMutex($.inh("right", id), $.inh("left", id), (b) -> {
            if (b)
                track.cx = Util.clamp(track.cx + track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        }, (b) -> {
            if (b)
                track.cx = Util.clamp(track.cx - track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        });
    }

}
