package nars.experiment.trackxy;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.test.control.TrackXY;
import jcog.tree.rtree.rect.RectFloat;
import nars.*;
import nars.agent.FrameTrigger;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
import nars.exe.UniExec;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorView;
import nars.index.concept.CaffeineIndex;
import nars.op.stm.STMLinkage;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.clock.RealTime;
import org.eclipse.collections.impl.block.factory.Comparators;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.video.Draw;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static nars.Op.GOAL;
import static nars.Op.IMPL;

public class TrackXY_NAR extends NAgentX {

    static boolean
            sourceNumerics = true,
            targetNumerics = false,
            targetCam = !targetNumerics,
            gui = true;

    //W = 3, H = 1;
    //W = 5, H = 1;
    public static final int derivationStrength = 8;

    static float fps = 16;
    static int durMS = Math.round(1000/(fps));


    static float camResolution = 0.1f;
    static int volMax = 11;
    final Bitmap2DSensor cam;
    private final TrackXY track;


    static int experimentTime = 3000000;


    final public AtomicBoolean trainer = new AtomicBoolean(false);


    //final public AtomicBoolean log = new AtomicBoolean(true);


    protected TrackXY_NAR(NAR nar, TrackXY xy) {
        super("trackXY",
                //FrameTrigger.cycles(W*H*2),
                //FrameTrigger.durs(1),
                FrameTrigger.fps(fps),
                nar);

        int W = xy.W;
        int H = xy.H;
        this.track = new TrackXY(W, H);

        assert (sourceNumerics | targetNumerics | targetCam);

        if (sourceNumerics) {
            senseNumberBi($.the("sx"), new FloatNormalized(() -> track.cx, 0, W - 1));
            if (H > 1)
                senseNumberBi($.the("sy"), new FloatNormalized(() -> track.cy, 0, H - 1));
        }


        if (targetNumerics) {
            senseNumberBi($.the("tx"), new FloatNormalized(() -> track.tx, 0, W));
            if (H > 1)
                senseNumberBi($.the("ty"), new FloatNormalized(() -> track.ty, 0, H));
        }

        if (targetCam) {
            Bitmap2DSensor<jcog.signal.wave2d.ArrayBitmap2D> c = new Bitmap2DSensor<>(/*id*/  (Term) null, track.grid, nar);
            c.resolution(camResolution);
            addSensor(c);
            /*id*/
            this.cam = c;

        } else {
            this.cam = null;
        }

        //actionPushButton();
        actionPushButtonMutex();
        //actionSwitch();
        //actionTriState();

//        {
//            curiosity.enable.setAt(false);
//            GraphEdit w = new GraphEdit(RectFloat.X0Y0WH(0, 0, 256, 256));
//
//            w.addAt(new KeyboardChip.ArrowKeysChip() {
//                @Override
//                protected void keyLeft() {
//                    long now = nar.time();
//                    nar().want(0.1f, $.the("left"), now, now+dur, 1f, 0.02f);
//                }
//                @Override
//                protected void keyRight() {
//                    long now = nar.time();
//                    nar().want(0.1f, $.the("right"), now, now+dur, 1f, 0.02f);
//                }
//            }).pos(0, 0, 256, 256);
//
//            window(w, 256, 256);
//        }

//        if (alwaysTrain) {
//            curiosity.enable.setAt(false);
//            onFrame(x -> {
//                track.act();
//            });
//        } else {
        onFrame(track::act);

        FloatSupplier nearness = () -> 1f - (track.dist() / track.distMax());

        rewardNormalized($.the("good"), 0, 1, nearness);
        //rewardNormalized($.the("better"), -0.1f,  +0.1f, new FloatFirstOrderDifference(nar::time, nearness) );

//        }

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


        onFrame(() -> {

            if (trainer.getOpaque()) {
                long now = nar.time();
                if (track.ty < track.cy) {
                    nar().want(0.1f, $.the("down"), now, now + durMS, 1f, 0.02f);
                } else if (track.ty > track.cy) {
                    nar().want(0.1f, $.the("up"), now, now + durMS, 1f, 0.02f);
                }
                if (track.tx < track.cx) {
                    nar().want(0.1f, $.the("left"), now, now + durMS, 1f, 0.02f);
                } else if (track.tx > track.cx) {
                    nar().want(0.1f, $.the("right"), now, now + durMS, 1f, 0.02f);
                }
            }
        });
    }


    public static void main(String[] args) {

        NAR n = newNAR();

        //final int W = 3, H = 1;
        final int W = 4, H = 4;

        TrackXY_NAR a = new TrackXY_NAR(n, new TrackXY(W, H));


//        //if (rl) {
//        {
//            RLBooster rlb = new RLBooster(a,
//
//                    //DQN2::new,
//                    HaiQae::new,
//                    //HaiQ::new,
//
//                    false);
//            a.curiosity.enable.setAt(false);
//
//            window(new Gridding(
//                Stream.of(((HaiQ) (rlb.agent)).q,((HaiQ) (rlb.agent)).et).map(
//                        l -> {
//
//                            BitmapMatrixView i = new BitmapMatrixView(l);
//                            a.onFrame(i::update);
//                            return i;
//                        }
//                ).collect(toList()))
//            , 800, 800);
//
////            window(
////                    new LSTMView(
////                            ((LivePredictor.LSTMPredictor) ((DQN2) rlb.agent).valuePredict).lstm.agent
////                    ), 800, 800
////            );
////
//////            window(new Gridding(
//////                Stream.of(((DQN2) (rlb.agent)).valuePredict.layers).map(
//////                        l -> {
//////
//////                            BitmapMatrixView i = new BitmapMatrixView(l.input);
//////                            BitmapMatrixView w = new BitmapMatrixView(l.weights);
//////                            BitmapMatrixView o = new BitmapMatrixView(l.output);
//////
//////                            a.onFrame(i::update);
//////                            a.onFrame(w::update);
//////                            a.onFrame(o::update);
//////
//////                            return new Gridding(i, w, o);
//////                        }
//////                ).collect(toList()))
//////            , 800, 800);
//        }


        if (gui)
            gui(n, a);

        n.run(experimentTime);


        //printGoals(n);
        //printImpls(n);

        //n.stats(System.out);
        //n.conceptsActive().forEach(System.out::println);

        //n.tasks().forEach(System.out::println);
    }

    public static void gui(NAR n, TrackXY_NAR a) {
        n.runLater(() -> {

            GraphEdit window = GraphEdit.window(800, 800);

            window.add(NARui.agent(a)).posRel(0.5f, 0.5f, 0.1f, 0.1f);
            window.add(NARui.top(n)).posRel(0.5f, 0.5f, 0.1f, 0.1f);

            //window.addAt(new ExpandingChip("x", ()->NARui.top(n))).posRel(0.8f,0.8f,0.25f,0.25f);
//            window.addAt(new HubMenuChip(new PushButton("NAR"), NARui.menu(n))).posRel(0.8f,0.8f,0.25f,0.25f);

            if (a.cam != null) {
                window.add(Splitting.column(new VectorSensorView(a.cam, n) {
                    @Override
                    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                        super.paint(gl, surfaceRender);
                        RectFloat at = cellRect(a.track.cx, a.track.cy, 0.5f, 0.5f);
                        gl.glColor4f(1, 0, 0, 0.9f);
                        Draw.rect(at.move(x(), y(), 0.01f), gl);
                    }
                }.withControls(), 0.1f, new ObjectSurface<>(a.track))).posRel(0.5f, 0.5f, 0.3f, 0.3f);
            }
        });
    }

    static NAR newNAR() {
        NARS nb = new NARS.DefaultNAR(0, true)
                .exe(new UniExec(1, 2 /* force concurrent */))
                .time(new RealTime.MS().dur(durMS))
                //.time(new CycleTime().dur(dur))
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

//        n.beliefConfDefault.setAt(0.5f);
//        n.goalConfDefault.setAt(0.5f);


        n.goalPriDefault.set(0.5f);
       n.beliefPriDefault.set(0.1f);
        n.questionPriDefault.set(0.05f);
        n.questPriDefault.set(0.05f);


//        n.freqResolution.setAt(0.1f);
//        n.confResolution.setAt(0.05f);


        //n.freqResolution.setAt(0.04f);

        n.termVolumeMax.set(volMax);
        n.timeResolution.set(Math.max(1, durMS));

        {


            Deriver d = new BatchDeriver(Derivers.nal(n,
                    6, 8
                    //"induction.goal.nal"
                    //1, 8
                    //2, 8
                    , "motivation.nal"
            )) {
//                    @Override
//                    public float puncFactor(byte conclusion) {
//                        return conclusion == GOAL ? 1 : 0.01f; //super.puncFactor(conclusion);
//                    }
            };


            ((BatchDeriver) d).tasklinksPerIteration.set(derivationStrength);


            new STMLinkage(n, 1) {
//                @Override
//                public boolean keep(Task newEvent) {
//                    return newEvent.isGoal();
//                }
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
//                if (tt instanceof DerivedTask && tt.isGoal()) {
//                    //if (n.concept(tt) instanceof ActionConcept)
//                    System.out.println(tt.proof());
////                    Term ttt = tt.target();
////                    if (tt.expectation() > 0.5f && tt.start() > n.time()-n.dur() && tt.start() < n.time() + n.dur()) {
////                        boolean l = ttt.toString().equals("left");
////                        boolean r = ttt.toString().equals("right");
////                        if (l || r) {
////
////                            float wantsDir = l ? -1 : +1;
////                            float needsDir = a.track.tx - a.track.cx;
////
////                            String summary = (Math.signum(wantsDir)==Math.signum(needsDir)) ? "OK" : "WRONG";
////                            System.err.println(ttt + " " + n2(wantsDir) + " ? " + n2(needsDir) + " " + summary);
////                        }
////                    }
//                }
            }, GOAL);

        }
        return n;
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
            actionPushButtonMutex($.the("up"), $.the("down"), (b) -> {
                if (b) {
                    float pcy = track.cy;
                    track.cy = Util.clamp(track.cy + track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
                    return track.cy != pcy;
                } else
                    return false;
            }, (b) -> {
                if (b) {
                    float pcy = track.cy;
                    track.cy = Util.clamp(track.cy - track.controlSpeed.floatValue(), 0, track.grid.height() - 1);
                    return track.cy != pcy;
                } else
                    return false;
            });
        }

        actionPushButtonMutex($.the("left"), $.the("right"), (b) -> {
            if (b) {
                float pcx = track.cx;
                track.cx = Util.clamp(track.cx - track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
                return track.cx != pcx;
            } else
                return false;
        }, (b) -> {
            if (b) {
                float pcx = track.cx;
                track.cx = Util.clamp(track.cx + track.controlSpeed.floatValue(), 0, track.grid.width() - 1);
                return track.cx != pcx;
            } else
                return false;
        });
    }

}
