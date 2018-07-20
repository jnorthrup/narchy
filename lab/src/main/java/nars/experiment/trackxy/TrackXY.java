package nars.experiment.trackxy;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.math.MutableEnum;
import jcog.signal.ArrayBitmap2D;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.*;
import nars.agent.FrameTrigger;
import nars.agent.NAgent;
import nars.concept.action.SwitchAction;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
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
import spacegraph.SpaceGraph;
import spacegraph.space2d.SurfaceRender;
import spacegraph.video.Draw;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static jcog.Util.unitize;
import static nars.Op.*;
import static spacegraph.SpaceGraph.window;

/* 1D and 2D grid tracking */
public class TrackXY extends NAgent {

    final Bitmap2DSensor cam;
    final ArrayBitmap2D view;

    final int W, H;

    float tx, ty;

    float sx, sy;

    public final FloatRange controlSpeed = new FloatRange(0.5f, 0, 4f);

    public final FloatRange targetSpeed = new FloatRange(0.05f, 0, 2f);

    public final FloatRange visionContrast = new FloatRange(0.9f, 0, 1f);

    public final MutableEnum<TrackXYMode> mode =
            new MutableEnum<TrackXYMode>(TrackXYMode.class).set(TrackXYMode.CircleTarget);

    static boolean targetNumerics = false, targetCam = true;

    private transient float lastDistance = Float.POSITIVE_INFINITY;


    protected TrackXY(NAR nar, int W, int H) {
        super("trackXY",
                FrameTrigger.durs(2),
                //FrameTrigger.fps(1),
                nar);

        senseNumber($.inh("sx", id), new FloatNormalized(() -> sx, 0, W));
        if (H > 1)
            senseNumber($.inh("sy", id), new FloatNormalized(() -> sy, 0, H));


        if (targetNumerics) {
            senseNumber($.inh("tx", id), new FloatNormalized(() -> tx, 0, W));
            if (H > 1)
                senseNumber($.inh("ty", id), new FloatNormalized(() -> ty, 0, H));
        }

        this.view = new ArrayBitmap2D(this.W = W, this.H = H);
        if (targetCam) {
            this.cam = sense(new Bitmap2DSensor<>(id /* (Term) null*/, view, nar));
            this.cam.resolution(0.2f);
        } else {
            this.cam = null;
        }

        actionPushButtonMutex();
        //actionSwitch();
        //actionTriState();


        reward(this::act);

        randomize();

    }

    public static void main(String[] args) {

        boolean nars = true;
//        boolean rl = false;

        int dur = 16;

        NARS nb = new NARS()
                .exe(new UniExec())
                .time(new CycleTime().dur(dur))
                .index(
                        new CaffeineIndex(2 * 1024*20)
                        //new HijackConceptIndex(4 * 1024, 4)
                );


        NAR n = nb.get();
        n.dtDither.set(1);
        n.timeFocus.set(2);
//        n.freqResolution.set(0.02f);
//        n.goalPriDefault.set(0.9f);
//        n.beliefPriDefault.set(0.3f);
//        n.questionPriDefault.set(0.1f);
//        n.questPriDefault.set(0.1f);


        n.termVolumeMax.set(17);


        TrackXY t = new TrackXY(n, 3, 2);


//        if (rl) {
//            new RLBooster(t,
//
//                    HaiQae::new,
//
//                    1);
//            t.curiosity.set(0);
//        }
        if (nars) {


            Deriver d = new MatrixDeriver(Derivers.nal(n,
                    1,
                    8
                    //,"curiosity.nal"
                    , "motivation.nal"
            ));

//            ((MatrixDeriver) d).conceptsPerIteration.set(8);


            new STMLinkage(n, 1, false);

            ConjClustering cjB = new ConjClustering(n, BELIEF,
                    x -> true,
                    //Task::isInput,
                    4, 16);


//            window(new Gridding(
//                    new AutoSurface(d),
//                    new AutoSurface(cjB)
//
//            ), 400, 300);

            Param.DEBUG = true;
            n.onTask(tt -> {
                if (tt instanceof DerivedTask && tt.isGoal()) {
                    System.out.println(tt.proof());
                }
            });

        }


        n.runLater(() -> {
            window(NARui.agent(t), 800, 800);

            window(NARui.top(n), 800, 250);

//            NARui.agentWindow(t);
            if (t.cam != null) {
                window(new CameraSensorView(t.cam, n) {
                    @Override
                    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                        super.paint(gl, surfaceRender);
                        RectFloat2D at = cellRect(t.sx, t.sy, 0.5f, 0.5f);
                        gl.glColor4f(1, 0, 0, 0.9f);
                        Draw.rect(at.move(x(), y(), 0.01f), gl);
                    }
                }.withControls(), 800, 800);
            }
        });


        int experimentTime = 35000;
        n.run(experimentTime);

        printGoals(n);
        printImpls(n);


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


    private void actionSwitch() {
        SwitchAction s = new SwitchAction(nar, (a) -> {
            float controlSpeed = this.controlSpeed.floatValue();
            switch (a) {
                case 0: {

                    float dy = -1;
                    float py = sy;
                    sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
                    return !Util.equals(py, sy, 0.01f);
                }
                case 1: {

                    float dy = +1;
                    float py = sy;
                    sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
                    return !Util.equals(py, sy, 0.01f);
                }
                case 2: {

                    float dx = -1;
                    float px = sx;
                    sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
                    return !Util.equals(px, sx, 0.01f);
                }
                case 3: {

                    float dx = +1;
                    float px = sx;
                    sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
                    return !Util.equals(px, sx, 0.01f);
                }
                case 4: {

                    return true;
                }
                default:
                    throw new UnsupportedOperationException();
            }
        }, INH.the($.the("up"), id),
                INH.the($.the("down"), id),
                INH.the($.the("left"), id),
                INH.the($.the("right"), id),
                INH.the($.the("stay"), id)
        );
        onFrame(s);
        SpaceGraph.window(NARui.beliefCharts(64, s.sensors, nar), 300, 300);
    }

    private void actionTriState() {

        if (view.height() > 1) {
            actionTriState($.inh($.the("Y"), id), (dy) -> {
                float py = sy;
                sy = Util.clamp(sy + this.controlSpeed.floatValue() * dy, 0, view.height() - 1);
                return !Util.equals(py, sy, 0.01f);
            });
        }

        actionTriState($.inh($.the("X"), id), (dx) -> {
            float px = sx;
            sx = Util.clamp(sx + this.controlSpeed.floatValue() * dx, 0, view.width() - 1);
            return !Util.equals(px, sx, 0.01f);
        });
    }

    private void actionPushButton() {
        if (view.height() > 1) {
            actionPushButton(INH.the($.the("up"), id), () -> {
                sy = Util.clamp(sy + this.controlSpeed.floatValue(), 0, view.height() - 1);

            });
            actionPushButton(INH.the($.the("down"), id), () -> {
                sy = Util.clamp(sy - this.controlSpeed.floatValue(), 0, view.height() - 1);
            });
        }

        actionPushButton(INH.the($.the("right"), id), () -> {
            sx = Util.clamp(sx + this.controlSpeed.floatValue(), 0, view.width() - 1);
        });
        actionPushButton(INH.the($.the("left"), id), () -> {
            sx = Util.clamp(sx - this.controlSpeed.floatValue(), 0, view.width() - 1);
        });
    }

    private void actionPushButtonMutex() {
        if (view.height() > 1) {
            actionPushButtonMutex(INH.the($.the("up"), id), INH.the($.the("down"), id), (b) -> {
                if (b)
                    sy = Util.clamp(sy + this.controlSpeed.floatValue(), 0, view.height() - 1);
            }, (b) -> {
                if (b)
                    sy = Util.clamp(sy - this.controlSpeed.floatValue(), 0, view.height() - 1);
            });
        }

        actionPushButtonMutex(INH.the($.the("right"), id), INH.the($.the("left"), id), (b) -> {
            if (b)
                sx = Util.clamp(sx + this.controlSpeed.floatValue(), 0, view.width() - 1);
        }, (b) -> {
            if (b)
                sx = Util.clamp(sx - this.controlSpeed.floatValue(), 0, view.width() - 1);
        });
    }

    protected void randomize() {
        this.tx = random().nextInt(view.width());
        //this.sx = random().nextInt(view.width());
        if (view.height() > 1) {
            this.ty = random().nextInt(view.height());
            //this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.sy = 0;
        }
    }

    protected float act() {


        mode.get().accept(this);

        view.set((x, y) -> {


            float distOther = (float) Math.sqrt(Util.sqr(tx - x) + Util.sqr(ty - y));
            //float distSelf = (float) Math.sqrt(Util.sqr(sx - x) + Util.sqr(sy - y));
            return unitize(1 - distOther * visionContrast.floatValue());
//                return Util.unitize(
//                        Math.max(1 - distOther * visionContrast,
//                                1 - distSelf * visionContrast
//                        ));


        });


        if (cam != null) {
            cam.input();
        }


        float maxDist = (float) Math.sqrt(W * W + H * H);
        float distance = (float) Math.sqrt(Util.sqr(tx - sx) + Util.sqr(ty - sy));

        this.lastDistance = distance;

        float distRatio = distance / maxDist;


        return -distRatio;

    }


    public enum TrackXYMode implements Consumer<TrackXY> {

        FixedCenter {
            float x;

            @Override
            public void accept(TrackXY t) {
                x += t.targetSpeed.floatValue();
                t.tx = t.W / 2;
                t.ty = t.H / 2;
            }
        },

        RandomTarget {
            @Override
            public void accept(TrackXY t) {
                float targetSpeed = t.targetSpeed.floatValue();
                float tx, ty;
                tx = Util.clamp(t.tx + 2 * targetSpeed * (t.random().nextFloat() - 0.5f), 0, t.W - 1);
                if (t.H > 1) {
                    ty = Util.clamp(t.ty + 2 * targetSpeed * (t.random().nextFloat() - 0.5f), 0, t.H - 1);
                } else {
                    ty = 0;
                }

                t.tx = tx;
                t.ty = ty;
            }
        },

        CircleTarget {
            float theta;

            @Override
            public void accept(TrackXY t) {
                theta += t.targetSpeed.floatValue();

                t.tx = (((float) Math.cos(theta) * 0.5f) + 0.5f) * (t.W - 1);

                if (t.H > 1)
                    t.ty = (((float) Math.sin(theta) * 0.5f) + 0.5f) * (t.H - 1);
                else
                    t.ty = 0;

            }
        }

    }


}

