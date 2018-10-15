package nars.experiment.trackxy;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.experiment.TrackXY;
import jcog.lab.Lab;
import jcog.lab.util.Optimization;
import jcog.learn.ql.HaiQae;
import jcog.math.FloatNormalized;
import jcog.tree.rtree.rect.RectFloat;
import nars.*;
import nars.agent.FrameTrigger;
import nars.agent.util.RLBooster;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.impl.MatrixDeriver;
import nars.exe.Attention;
import nars.exe.UniExec;
import nars.gui.NARui;
import nars.index.concept.CaffeineIndex;
import nars.op.stm.ConjClustering;
import nars.op.stm.STMLinkage;
import nars.sensor.Bitmap2DSensor;
import nars.task.DerivedTask;
import nars.time.clock.CycleTime;
import nars.video.CameraSensorView;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.video.Draw;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static nars.Op.*;
import static spacegraph.SpaceGraph.window;

public class TrackXY_NAR extends NAgentX {

    static boolean
            targetNumerics = false,
            targetCam = true,
            gui = true;

    final Bitmap2DSensor cam;
    private final TrackXY track;
    private float rewardSum = 0;

    protected TrackXY_NAR(NAR nar, int W, int H) {
        super("trackXY",
                //FrameTrigger.cycles(W*H*2),
                FrameTrigger.durs(1),
                //FrameTrigger.fps(1),
                nar);

        this.track = new TrackXY(W, H);

        senseNumber($.inh("sx", id), new FloatNormalized(() -> track.cx, 0, W));
        if (H > 1)
            senseNumber($.inh("sy", id), new FloatNormalized(() -> track.cy, 0, H));


        if (targetNumerics) {
            senseNumber($.inh("tx", id), new FloatNormalized(() -> track.tx, 0, W));
            if (H > 1)
                senseNumber($.inh("ty", id), new FloatNormalized(() -> track.ty, 0, H));
        }

        if (targetCam) {
            this.cam = addCamera(new Bitmap2DSensor<>(id /* (Term) null*/, track.grid, nar));
        } else {
            this.cam = null;
        }

        //actionPushButtonMutex();
        //actionSwitch();
        actionTriState();


        reward(() -> {
            float r = track.act();
            if (r == r)
                rewardSum += r;
            return r;
        });

        track.randomize();


    }


    public static void main(String[] args) {

        boolean nars = false, rl = true;
//        boolean rl = false;

//        Param.DEBUG = true;
        int W = 4;
        int H = 1;
        int dur =
                4;
        //8;
        //2 * (W * H) /* to allow pixels to be read at the rate of 1 pixel per cycle */;

        NARS nb = new NARS.DefaultNAR(0, true)
                .attention(() -> new Attention(96))
                .exe(new UniExec())
                .time(new CycleTime().dur(dur))
                .index(
                        new CaffeineIndex(4 * 1024 * 10)
                        //new HijackConceptIndex(4 * 1024, 4)
                );


        NAR n = nb.get();

        n.timeResolution.set(dur);


//        n.goalPriDefault.set(0.99f);
//        n.beliefPriDefault.set(0.01f);
//        n.questionPriDefault.set(0.01f);
//        n.questPriDefault.set(0.01f);


//            n.termVolumeMax.set(9);
//        n.freqResolution.set(0.1f);
//        n.confResolution.set(0.05f);


        TrackXY_NAR a = new TrackXY_NAR(n, W, H);


        if (rl) {
            new RLBooster(a,

                    HaiQae::new,
                    //HaiQ::new,

                    true);
            a.curiosity.set(0);
        }
        if (nars) {


            Deriver d = new MatrixDeriver(Derivers.nal(n,
                        //6, 8
                    1, 8
//                    //,"curiosity.nal"
                    , "motivation.nal"
            )) {
//                    @Override
//                    public float puncFactor(byte conclusion) {
//                        return conclusion == GOAL ? 1 : 0.01f; //super.puncFactor(conclusion);
//                    }
            };


            ((MatrixDeriver) d).conceptsPerIteration.set(4);


            new STMLinkage(n, 1) {
                @Override
                public boolean hold(Task newEvent) {
                    return newEvent.isGoal();
                }
            };

            ConjClustering cjB = new ConjClustering(n, BELIEF,
                    //x -> true,
                    Task::isInput,
                    2, 8);


//            window(new Gridding(
//                    new AutoSurface(d),
//                    new AutoSurface(cjB)
//
//            ), 400, 300);

            //n.log();

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
                    window(new CameraSensorView(a.cam, n) {
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

        a.onFrame(()->{
            Util.sleepMS(10);
        });
//        a.onFrame(() -> {
//            long now = n.time();
//            if (now % 1000 == 0) {
//                System.out.println(
//                        //"reward mean: " +
//                        a.rewardSum / now);
//                a.rewardSum = 0;
//            }
//        });

        int experimentTime = 56000;
        n.run(experimentTime);


        //printGoals(n);
        //printImpls(n);

        //n.stats(System.out);
        //n.conceptsActive().forEach(System.out::println);

    }


    public static void printGoals(NAR n) {
        int dur = n.dur();
        long now = n.time();
        List<Task> l = n.tasks(false, false, true, false)
                .sorted(Comparators.byFloatFunction(tt -> -tt.evi(now, dur)))
                .collect(Collectors.toList());
        l.forEach(System.out::println);
        System.out.println();
    }

    public static void printImpls(NAR n) {
        int dur = n.dur();
        long now = n.time();
        List<Task> l = n.tasks(true, false, false, false)
                .filter(x -> x.op() == IMPL)
                .sorted(Comparators.byFloatFunction(tt -> -tt.evi(now, dur)))
                .collect(Collectors.toList());
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
        if (track.grid.height() > 1) {
            actionPushButton(INH.the($.the("up"), id), () -> {
                track.cy = Util.clamp(track.cy + track.controlSpeed.floatValue(), 0, track.grid.height() - 1);

            });
            actionPushButton(INH.the($.the("down"), id), () -> {
                track.cy = Util.clamp(track.cy - track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            });
        }

        actionPushButton(INH.the($.the("right"), id), () -> {
            track.cx = Util.clamp(track.cx + track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        });
        actionPushButton(INH.the($.the("left"), id), () -> {
            track.cx = Util.clamp(track.cx - track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        });
    }

    private void actionPushButtonMutex() {
        if (track.grid.height() > 1) {
            actionPushButtonMutex($.func("up", id), $.func("down", id), (b) -> {
                if (b)
                    track.cy = Util.clamp(track.cy + track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            }, (b) -> {
                if (b)
                    track.cy = Util.clamp(track.cy - track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
            });
        }

        actionPushButtonMutex($.func("right", id), $.func("left", id), (b) -> {
            if (b)
                track.cx = Util.clamp(track.cx + track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        }, (b) -> {
            if (b)
                track.cx = Util.clamp(track.cx - track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
        });
    }

    static class Optimize {
        public static void main(String[] args) {
            Lab<NAR> l = new Lab<>(() -> {
                NAR n = NARS.tmp();
                n.random().setSeed(System.nanoTime());
                return n;
            });
            l.hints.put("autoInc", 3);
            l.varAuto(new Lab.DiscoveryFilter() {
                @Override
                protected boolean includeField(Field f) {
                    if (f.getName().startsWith("DEBUG"))
                        return false;

                    return super.includeField(f);
                }
            });

            int experiments = 256;
            int experimentCycles = 4096;

            Optimization<NAR, TrackXY_NAR> o = l.optimize((Supplier<NAR> s) -> {
                        return new TrackXY_NAR(s.get(), 4, 4);
                    },
                    (TrackXY_NAR t) -> {
                        t.nar.run(experimentCycles);
                        return t.rewardSum;
                    }
                    , experiments);
////            o.sense("numConcepts",
////                (TestNARSuite t) -> t.sum((NAR n) -> n.concepts.size()))
//                    .sense("derivedTask",
//                            (TestNARSuite t) -> t.sum((NAR n)->n.emotion.deriveTask.getValue()))

            o.runSync().print();

            RealDecisionTree t = o.tree(4, 8);
            t.print();
            t.printExplanations();


        }
    }

}
