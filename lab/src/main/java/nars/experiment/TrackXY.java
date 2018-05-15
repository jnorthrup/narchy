package nars.experiment;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.ql.HaiQae;
import jcog.signal.ArrayBitmap2D;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.*;
import nars.concept.scalar.SwitchAction;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.exe.UniExec;
import nars.gui.Vis;
import nars.index.concept.CaffeineIndex;
import nars.op.RLBooster;
import nars.op.stm.ConjClustering;
import nars.task.DerivedTask;
import nars.time.clock.CycleTime;
import nars.util.signal.Bitmap2DSensor;
import nars.video.CameraSensorView;
import org.eclipse.collections.impl.block.factory.Comparators;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.video.Draw;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static nars.Op.BELIEF;
import static nars.Op.INH;
import static spacegraph.SpaceGraph.window;

/* 1D and 2D grid tracking */
public class TrackXY extends NAgent {

    final ArrayBitmap2D view;
    public Bitmap2DSensor cam;
    //target position, to reach
    float tx, ty;
    //source position, to control
    float sx, sy;
    Consumer<TrackXY> updater =
            //new RandomTarget();
            //new CyclicTarget();
            new CircleTarget();
    private final float controlSpeed =
            1f;
    //0.5f;
    private final float visionContrast = 0.9f;

    protected TrackXY(int x, int y) {
        super("trackXY", null);
        this.view = new ArrayBitmap2D(x, y);
    }

    public static void main(String[] args) {

        boolean nars = true;
        boolean rl = false;

        int dur = 2;

        NARS nb = new NARS()
                .exe(new UniExec(64))
                .time(new CycleTime().dur(dur))
                .index(
                        //new HijackConceptIndex(4 * 1024, 4)
                        new CaffeineIndex(32 * 1024)
                );


        NAR n = nb.get();

        n.termVolumeMax.set(30);
//        n.priDefault(BELIEF, 0.2f);
//        n.priDefault(GOAL, 0.5f);
        n.activateConceptRate.set(0.5f);
//        n.forgetRate.set(0.9f);


        TrackXY t = new TrackXY(4, 4);
        n.on(t);

        int experimentTime = 1280;
        n.synch();
//        n.run(1);

//        n.runLater(() -> {
//            TemporalMetrics m = new TemporalMetrics(experimentTime + 100);
//            n.onCycle(() -> m.update(n.time()));
//            m.add("reward", () -> t.reward);
//            m.add("happy", () -> {
//                float h = t.happy.asFloat();
//                if (h != h)
//                    return 0.5f;
//                return h;
//            });
//            m.add("dex", () -> t.dexterity());
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                String filename = "/tmp/x.csv";
//                System.err.println("saving " + m + " to: " + filename);
//                try {
//                    m.printCSV(filename);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }));
//        });



        if (rl) {
            new RLBooster(t,
                    //DQN::new,
                    //HaiQ::new,
                    HaiQae::new,
                    //RandomAgent::new,
                    1);
            t.curiosity.set(0);
        }
        if (nars) {

            //
            //n.log();

//            for (String action : new String[]{"up", "down", "left", "right"}) {
//                //n.goal($.the(action), Tense.Present, 0f, 0.1f);
//                n.goal($.the(action), Tense.Present, 1f, 0.1f);
//            }

            Deriver d = new MatrixDeriver(Derivers.nal(
                    //1,
                    1,
                    8, n,
                    //"list.nal",
                    "motivation.nal"));

            ((MatrixDeriver)d).conceptsPerIteration.set(32);
            n.timeFocus.set(2);

            ConjClustering cjB = new ConjClustering(n, BELIEF,
                    //(tt)->true,
                    Task::isInput,
                    4, 16);

//            ConjClustering cjG = new ConjClustering(n, GOAL,
//                    (tt)->true,
//                    //(tt) -> tt.isInput(),
//                    5, 16);

//            Implier ii = new Implier(t , 0, 1);

//            ArithmeticIntroduction ai = new ArithmeticIntroduction(4, n);

            window(new Gridding(
                    new AutoSurface(d),
                    new AutoSurface(cjB)
//                    new AutoSurface(cjG)

                    //,new AutoSurface(ai)
            ), 400, 300);
            n.onTask(tt -> {
                if (tt instanceof DerivedTask && tt.isGoal()) {
                    System.out.println(tt.proof());
                }
            });
        }

//        n.log();

//        n.startFPS(fps);
//        t.runFPS(fps);
        n.onCycle(t);
        final double[] rewardSum = {0};
        n.onCycle(() ->

        {
            rewardSum[0] += t.reward;
        });


        n.runLater(() -> {
            window(Vis.top(n), 800, 250);
            NAgentX.chart(t);
            window(new CameraSensorView(t.cam, n) {
                @Override
                protected void paint(GL2 gl, int dtMS) {
                    super.paint(gl, dtMS);
                    RectFloat2D at = cellRect(t.sx, t.sy, 0.5f, 0.5f);
                    gl.glColor4f(1, 0, 0, 0.9f);
                    Draw.rect(gl, at.move(x(), y(), 0.01f));
                }
            }.withControls(), 800, 800);
        });
        n.run(experimentTime);

        long now = n.time();
        List<Task> l = n.tasks(false, false, true, false)
                .sorted(Comparators.byFloatFunction(tt -> -tt.evi(now, dur)))
                .collect(Collectors.toList());
        l.forEach(System.out::println);

        //n.startFPS(10f);
        //t.runFPS(10f);

//        System.out.println(
//
//                n4(rewardSum[0] / n.time()) + " avg reward");

//        System.exit(0);
    }

    @Override
    protected void starting(NAR nar) {

        super.starting(nar);

        //actionTriState();
        //actionPushButton();
        actionSwitch();

        this.cam = new Bitmap2DSensor<>(id /* (Term) null*/, view, nar);
        this.cam.pixelPri.set(0.2f);
        //this.cam.resolution(0.1f);
        sensorCam.add(cam);

        //senseNumber($.the("x"), ()->sx/(view.width()-1));
        //senseNumber($.the("y"), ()->sy/(view.height()-1));


        randomize();
    }

    private void actionSwitch() {
        SwitchAction s = new SwitchAction(nar, (a)->{
            switch (a) {
                case 0: {
                    //up
                    float dy = -1;
                    float py = sy;
                    sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
                    return !Util.equals(py, sy, 0.01f);
                }
                case 1: {
                    //down
                    float dy = +1;
                    float py = sy;
                    sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
                    return !Util.equals(py, sy, 0.01f);
                }
                case 2: {
                    //left
                    float dx = -1;
                    float px = sx;
                    sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
                    return !Util.equals(px, sx, 0.01f);
                }
                case 3: {
                    //right
                    float dx = +1;
                    float px = sx;
                    sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
                    return !Util.equals(px, sx, 0.01f);
                }
                case 4: {
                    //stay
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
        SpaceGraph.window(Vis.beliefCharts(64, s.sensors, nar), 300, 300);
    }

    private void actionTriState() {

        if (view.height() > 1) {
            actionTriState($.inh($.the("Y"), id), (dy) -> {
                float py = sy;
                sy = Util.clamp(sy + controlSpeed * dy, 0, view.height() - 1);
                return !Util.equals(py, sy, 0.01f);
            });
        }

        actionTriState($.inh($.the("X"), id), (dx) -> {
            float px = sx;
            sx = Util.clamp(sx + controlSpeed * dx, 0, view.width() - 1);
            return !Util.equals(px, sx, 0.01f);
        });
    }

    private void actionPushButton() {
        if (view.height() > 1) {
            actionPushButton(INH.the($.the("up"), id), () -> {
                sy = Util.clamp(sy + controlSpeed, 0, view.height() - 1);

            });
            actionPushButton(INH.the($.the("down"), id), () -> {
                sy = Util.clamp(sy - controlSpeed, 0, view.height() - 1);
            });
        }

        actionPushButton(INH.the($.the("right"), id), () -> {
            sx = Util.clamp(sx + controlSpeed, 0, view.width() - 1);
        });
        actionPushButton(INH.the($.the("left"), id), () -> {
            sx = Util.clamp(sx - controlSpeed, 0, view.width() - 1);
        });
    }

    protected void randomize() {
        this.tx = random().nextInt(view.width());
        this.sx = random().nextInt(view.width());
        if (view.height() > 1) {
            this.ty = random().nextInt(view.height());
            this.sy = random().nextInt(view.height());
        } else {
            this.ty = this.sy = 0;
        }
    }

    public int width() {
        return view.width();
    }

    public int height() {
        return view.height();
    }

    @Override
    protected synchronized float act() {

        synchronized (view) {
            Consumer<TrackXY> u = updater;
            if (u != null)
                u.accept(this);

            view.set((x, y) -> {

//                float dist = (float) Math.sqrt(Util.sqr(tx - x) + Util.sqr(ty - y));
//                return Math.max(0, 1 - dist * visionContrast);


                float distOther = (float) Math.sqrt(Util.sqr(tx - x) + Util.sqr(ty - y));
                float distSelf = (float) Math.sqrt(Util.sqr(sx - x) + Util.sqr(sy - y));
                return Util.unitize(
                        Math.max(1 - distOther * visionContrast,
                                1 - distSelf * visionContrast
                        ));


//                if (Util.equals(sx, x, controlSpeed) && Util.equals(sy, y, controlSpeed)) {
//                    //show cursor in negative
//                    return 0f;
//                } else {
//                    float dist = (float) Math.sqrt(Util.sqr(tx - x) + Util.sqr(ty - y));
//                    //return 0.5f + 0.5f * Math.max(0, 1 - dist * visionContrast);
//                    return 0.55f + 0.5f * Math.max(0, 1 - dist * visionContrast);
//                }

            });
        }

        cam.input();


        float maxDist = (float) Math.sqrt(width() * width() + height() * height());
        float dist = (float) Math.sqrt(Util.sqr(tx - sx) + Util.sqr(ty - sy))
                / maxDist;


        //return 1f/(1f+dist);
        //return controlSpeed - dist*dist; //controlSpeed is margin of tolerance
        return -dist; //chase
        //return +dist; //avoid
    }

    public static class RandomTarget implements Consumer<TrackXY> {
        private final float targetSpeed = 0.5f;

        public void accept(TrackXY t) {

            float tx, ty;
            tx = Util.clamp(t.tx + 2 * targetSpeed * (t.random().nextFloat() - 0.5f), 0, t.width() - 1);
            if (t.height() > 1) {
                ty = Util.clamp(t.ty + 2 * targetSpeed * (t.random().nextFloat() - 0.5f), 0, t.height() - 1);
            } else {
                ty = 0;
            }

            t.tx = tx;
            t.ty = ty;
        }
    }

    public static class CyclicTarget implements Consumer<TrackXY> {


        float speed = 0.02f;
        float x = 0;

        @Override
        public void accept(TrackXY t) {
            x += speed;
            t.tx = (((float) Math.sin(x) * 0.5f) + 0.5f) * (t.width() - 1);
//            int tw = t.width();
//            if (t.tx > tw -1)
//                t.tx -= tw;
            t.ty = 0;
        }
    }

    /**
     * ellipse, technically
     */
    public static class CircleTarget implements Consumer<TrackXY> {


        float speed =
                0.02f;
        float theta = 0;


        @Override
        public void accept(TrackXY t) {
            theta += speed;

            t.tx = (((float) Math.cos(theta) * 0.5f) + 0.5f) * (t.width() - 1);

            if (t.height() > 1)
                t.ty = (((float) Math.sin(theta) * 0.5f) + 0.5f) * (t.height() - 1);
            else
                t.ty = 0;

//            int tw = t.width();
//            if (t.tx > tw -1)
//                t.tx -= tw;

        }

    }
}

