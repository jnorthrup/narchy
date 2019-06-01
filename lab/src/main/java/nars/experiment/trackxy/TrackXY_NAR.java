package nars.experiment.trackxy;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.test.control.TrackXY;
import jcog.tree.rtree.rect.RectFloat;
import nars.*;
import nars.agent.GameTime;
import nars.derive.BatchDeriver;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.exe.impl.UniExec;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorView;
import nars.memory.CaffeineMemory;
import nars.op.stm.STMLinkage;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.clock.CycleTime;
import org.eclipse.collections.impl.block.factory.Comparators;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.video.Draw;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static jcog.Texts.n2;
import static nars.Op.GOAL;
import static nars.Op.IMPL;

public class TrackXY_NAR extends GameX {

    static boolean
            sourceNumerics = true,
            targetNumerics = false,
            targetCam = !targetNumerics,
            gui = true;

    //W = 3, H = 1;
    //W = 5, H = 1;
//    public static final int derivationStrength = 2;

//    static float fps = 16;
//    static int durMS = Math.round(1000/(fps));

    static int dur = 8;

    static float camResolution = 0.1f;
    static int volMax = 20;
    final Bitmap2DSensor cam;
    private final TrackXY track;


    static int experimentTime = 3000000;


    final public AtomicBoolean trainer = new AtomicBoolean(false);

    final public AtomicBoolean log = new AtomicBoolean(true);

    protected TrackXY_NAR(NAR nar, TrackXY xy) {
        super("trackXY",
                //FrameTrigger.cycles(W*H*2),
                GameTime.durs(1),
                //FrameTrigger.fps(fps),
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
            Bitmap2DSensor<jcog.signal.wave2d.ArrayBitmap2D> c = new Bitmap2DSensor<>(/*id*/  (Term) null,
                    track.grid, nar);
            c.resolution(camResolution);
            addSensor(c);
            /*id*/
            this.cam = c;

        } else {
            this.cam = null;
        }

        actionPushButton();
        //actionPushButtonMutex();
        //actionSwitch();
        //actionTriState();

        actionAccelerate();

//        {
//            curiosity.enable.set(false);
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
//            curiosity.enable.set(false);
//            onFrame(x -> {
//                track.act();
//            });
//        } else {
        onFrame(track::act);

        FloatSupplier nearness = () -> 1f - (track.dist() / track.distMax());

        reward(nearness);
        //rewardNormalized($.the("better"), -0.1f,  +0.1f, new FloatFirstOrderDifference(nar::time, nearness) );

//        }

        track.randomize();


        onFrame(() -> {

            if (trainer.getOpaque()) {
                long now = nar.time();
                int durMS = dur();
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


    private void actionAccelerate() {
//        actionDial($.inh($.p(id, $.the("speed")), $.the(-1)),
//                $.inh($.p(id, $.the("speed")), $.the(+1)),
//                        track.controlSpeed, 100);
//        actionTriStateContinuous($.inh(id, $.the("speed")), (a)->{
////            System.out.println(a);
//            track.controlSpeed.add(a/100f);
//            return true; //TODO check change
//        });

        //FloatAveragedWindow _controlSpeed = new FloatAveragedWindow(8, 0.05f);
        actionUnipolar($.inh(id, $.the("speed")), (float a)->{
//            System.out.println(a);
            //track.controlSpeed.add(Math.pow(((a-0.5)*2),3)/100f);
            float cc = (float) Math.pow(a, 1f);
            //float c = _controlSpeed.valueOf(cc);
            float c = Util.lerp(cc, 0.01f, 0.2f);
            track.controlSpeed.set(c);
            return c;
            //TODO check change
        });
    }


    public static void main(String[] args) {

        NARS nb = new NARS.DefaultNAR(0, true)
                .exe(new UniExec())
                //.time(new RealTime.MS().dur(durMS))
                .time(new CycleTime().dur(dur))
                .index(
                        new CaffeineMemory(8 * 1024 * 10)
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

//        n.attn.links.capacity(1024);

        n.goalPriDefault.amp(0.5f);
        n.beliefPriDefault.amp(0.1f);
        n.questionPriDefault.amp(0.05f);
        n.questPriDefault.amp(0.05f);


        n.freqResolution.set(0.02f);
//        n.confResolution.set(0.05f);


        //n.freqResolution.set(0.04f);

        n.termVolMax.set(volMax);
        //n.dtDither.set(Math.max(1, durMS));


        Deriver d = new BatchDeriver(Derivers.nal(n,
                //6, 8
                //"induction.goal.nal"
                1, 8
                //2, 8
                //, "motivation.nal"
        )) {
//                    @Override
//                    public float puncFactor(byte conclusion) {
//                        return conclusion == GOAL ? 1 : 0.01f; //super.puncFactor(conclusion);
//                    }
        };


//        ((BatchDeriver) d).premisesPerIteration.set(derivationStrength);


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


        final int W = 4, H = 1;
        //final int W = 3, H = 3;

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
//            a.curiosity.enable.set(false);
//
//        }


        if (gui) {
            //n.runLater(() -> {



                EditGraph2D g = EditGraph2D.window(800, 800);
                g.windoSizeMinRel(0.02f, 0.02f);


                g.add(NARui.agent(a)).posRel(0.5f, 0.5f, 0.4f, 0.3f);
                g.add(NARui.top(n)).posRel(0.5f, 0.5f, 0.2f, 0.1f);
                g.add(NARui.attentionUI(n)).sizeRel(0.25f, 0.25f);


//                g.build(a, new AutoBuilder<>(2, (context, features, obj) -> {
//                        //return new TextEdit(features.toString());
//                    //g.add(new ObjectSurface(features)).sizeRel(0.1f,0.1f);
//                    return features.isEmpty() ? new ObjectSurface(obj) : new ObjectSurface(features, 2);
//                })).sizeRel(0.2f, 0.2f);

//                g.add(new PIDChip(new MiniPID(0.01, 0.01, 0.01))).sizeRel(0.1f, 0.1f);

//                {
//                    FloatAveragedWindow dexThrough = new FloatAveragedWindow(16, 0.1f);
//                    Agenterator aa = n.control.agent(
//                            () -> dexThrough.valueOf((float) (a.dexterity() + 0.01f * a.happiness())),
//                            (i, o) -> new HaiQae(i, 32, o));
//                    HaiQChip haiQChip = new HaiQChip((HaiQae) aa.agent) ;
//                    g.add(haiQChip).posRel(0.5f, 0.5f, 0.2f, 0.2f);
//
//
//                    haiQChip.getPlot().add("reward", aa::asFloat /* warning: THIS asFloat CALL ACTUALLY INVOKES THE AGENT */);
//
////        n.onCycle(()->haiQChip.next(reward));
//                    n.onCycle(
//                    //DurService.on(n,
//                            ()->{  haiQChip.next(); });
//                }
//
//                g.add(new LogContainer(32) {
//                    {
//                        LogContainer z = this; //HACK
//                        n.onTask(t->z.append(t.toStringWithoutBudget()));
//                    }
//                }).sizeRel(0.25f, 0.25f);


                //window.addAt(new ExpandingChip("x", ()->NARui.top(n))).posRel(0.8f,0.8f,0.25f,0.25f);
//            window.addAt(new HubMenuChip(new PushButton("NAR"), NARui.menu(n))).posRel(0.8f,0.8f,0.25f,0.25f);

                if (a.cam != null) {
                    g.add(Splitting.column(new VectorSensorView(a.cam, a) {
                        @Override
                        protected void paint(GL2 gl, ReSurface reSurface) {
                            super.paint(gl, reSurface);
                            RectFloat at = cellRect(a.track.cx, a.track.cy, 0.5f, 0.5f);
                            gl.glColor4f(1, 0, 0, 0.9f);
                            Draw.rect(at.move(x(), y(), 0.01f), gl);
                        }
                    }.withControls(), 0.1f, new ObjectSurface<>(a.track))).posRel(0.5f, 0.5f, 0.3f, 0.3f);
                }
            //});
        }


        //NAL.DEBUG = true;
        n.onTask(tt -> {
            if (!tt.isInput()) /*if (tt instanceof DerivedTask)*/ {
                //if (n.concept(tt) instanceof ActionConcept)
                if (tt.expectation() > 0.5f && tt.start() > n.time()-n.dur() && tt.start() < n.time() + n.dur()) {
                    Term ttt = tt.term();
                    boolean l = ttt.equals(a.actions.get(0).term());
                    boolean r = ttt.equals(a.actions.get(1).term());
                    if (l || r) {

                        float wantsDir = l ? -1 : +1;
                        float needsDir = a.track.tx - a.track.cx;

                        String summary = (Math.signum(wantsDir)==Math.signum(needsDir)) ? "OK" : "WRONG";
                        System.out.println(ttt + " " + n2(wantsDir) + " ? " + n2(needsDir) + " " + summary);
                        System.out.println(tt.proof());
                        System.out.println();
                    }
                }
            }
        }, GOAL);

        n.synch();
        n.run(experimentTime);



        //printGoals(n);
        //printImpls(n);

        //n.stats(System.out);
        //n.conceptsActive().forEach(System.out::println);

        //n.tasks().forEach(System.out::println);
    }


    public static void printGoals(NAR n) {
        int dur = n.dur();
        long now = n.time();
        List<Task> l = n.tasks(false, false, true, false)
                .sorted(Comparators.byFloatFunction(tt -> (float) -tt.evi(now, dur)))
                .collect(toList());
        l.forEach(System.out::println);
        System.out.println();
    }

    public static void printImpls(NAR n) {
        int dur = n.dur();
        long now = n.time();
        List<Task> l = n.tasks(true, false, false, false)
                .filter(x -> x.op() == IMPL)
                .sorted(Comparators.byFloatFunction(tt -> (float) -tt.evi(now, dur)))
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
