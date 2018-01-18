package nars.experiment;

import jcog.math.FloatParam;
import jcog.signal.Bitmap2D;
import nars.*;
import nars.experiment.tetris.TetrisState;
import nars.op.java.Opjects;
import nars.term.Term;
import nars.util.signal.CameraSensor;

import static nars.experiment.tetris.TetrisState.*;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends NAgentX implements Bitmap2D {

    public final FloatParam timePerFall = new FloatParam(1f, 1f, 32f);

    public static final int tetris_width = 8;
    public static final int tetris_height = 16;

    //private static SensorConcept[][] concept;
    //private int afterlife = TIME_PER_FALL * tetris_height * tetris_width;
    static boolean easy;

    private TetrisState state;

    private final CameraSensor<Bitmap2D> pixels;

    public Tetris(NAR nar) throws Narsese.NarseseException {
        this(nar, Tetris.tetris_width, Tetris.tetris_height);
    }

    public Tetris(NAR nar, int width, int height) throws Narsese.NarseseException {
        this(nar, width, height, 1);
    }

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(NAR nar, int width, int height, int timePerFall) throws Narsese.NarseseException {
        super("tetris", nar);

        state = new TetrisState(width, height, timePerFall) {
            @Override
            protected int nextBlock() {


                if (easy) {
                    //EASY MODE
                    return 1; //square blocks
                    //return 0; //long blocks
                } else {
                    return super.nextBlock(); //all blocks
                }
            }

//            @Override
//            protected void die() {
//                //nar.time.tick(afterlife);
//                super.die();
//            }
        };

//        view.children().add(new TetrisVisualizer(state, 2, false) {
//            @Override
//            public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
////
////                switch (charCode) {
////                    case 'a':
////                        if (motorRotate!=null)
////                            nar.goal(motorRotate, Tense.Present, pressed ? 0f : 0.5f, gamma);
////                        break;
////                    case 's':
////                        if (motorRotate!=null)
////                            nar.goal(motorRotate, Tense.Present, pressed ? 1f : 0.5f, gamma);
////                        break;
////                    case 'z':
////                        nar.goal(motorLeftRight, Tense.Present, pressed ? 0f : 0.5f, gamma);
////                        break;
////                    case 'x':
////                        nar.goal(motorLeftRight, Tense.Present, pressed ? 1f : 0.5f, gamma);
////                        break;
////                }
//
//                return true;
//            }
//        });


        addCamera(
                pixels = new CameraSensor<>((x, y)->
                    //$.func("cam", id, $.the(x), $.the(y))
                    $.inh($.p(x,y), id)
                , this, nar)
                //.resolution(0.1f)
        );
        //pixels.resolution(0.1f);


        actionsReflect();
        //actionsTriState();
        actionsToggle();

        state.reset();


//        view = new Grid(
////                    new MatrixView(tetris_width, tetris_height, (x, y, gl) -> {
////                        float r = nar.pri(concept[x][y], Float.NaN);
////                        gl.glColor3f(r, 0, 0);
////                        return 0f;
////                    }),
//                new MatrixView(tetris_width, tetris_height, (x, y, gl) -> {
//                    SensorConcept cxy = pixels.concept(x, y);
//                    long now = this.now;
//                    int dur = nar.dur();
//                    float b = cxy.beliefFreq(now, dur);
//                    Truth gg = cxy.goal(now, dur);
//                    float gp, gn;
//                    float g = gg!=null ? gg.freq() : Float.NaN;
//                    if (g == g) {
//                        g -= 0.5f;
//                        g *= 2f;
//                        float c = gg.conf();
//                        if (g < 0) {
//                            gp = 0;
//                            gn = -g * c;
//                        } else {
//                            gp = g * c;
//                            gn = 0;
//                        }
//                    } else {
//                        gp = gn = 0;
//                    }
//                    gl.glColor3f(gp, b, gn);
//                    return 0f;
//                })
////                    new MatrixView(tetris_width, tetris_height, (x, y, gl) -> {
////                        long then = (long) (now + DUR * 8);
////                        Truth f = concept[x][y].belief(then, DUR);
////                        float fr, co;
////                        if (f == null) {
////                            fr = 0.5f;
////                            co = 0;
////                        } else {
////                            fr = f.freq();
////                            co = f.conf();
////                        }
////                        gl.glColor4f(0, fr, 0, 0.25f + 0.75f * co);
//////                        Draw.colorPolarized(gl,
//////                                concept[x][y].beliefFreq(then, 0.5f) -
//////                                        concept[x][y].beliefFreq(now, 0.5f));
////                        return 0f;
////                    })
//
//        );
//        view.layout();
    }

    private void actionsReflect() {

        Opjects oo = new Opjects(nar);
        oo.executionThreshold.set(0.6f);

        oo.methodExclusions.add("toVector");
        state =
                //oo.the("tetris", this.state);
                oo.a("tetris", TetrisState.class, tetris_width, tetris_height, 2);

    }


    void actionsToggle() {
        final Term LEFT = $.inh("left", id);
        final Term RIGHT = $.inh("right", id);
        final Term ROT = $.inh("rotCW", id);

        actionToggle(LEFT, () -> state.act(TetrisState.LEFT));
        actionToggle(RIGHT, () -> state.act(TetrisState.RIGHT));
        actionPushButton(ROT, () -> state.act(CW));
        //actionToggle($.p("rotCCW"), ()-> state.take_action(CCW));
    }

    void actionsTriState() {


        actionTriState($.func("X", id), (i) -> {
            switch (i) {
                case -1:
                    state.act(LEFT);
                    break;
                case 0:
                    break;
                case +1:
                    state.act(RIGHT);
                    break;
            }
        });

        actionPushButton($.func("R", id), () -> state.act(CW));

//        actionTriState($("R"), (i) -> {
//            switch (i) {
//                case -1:
//                    state.take_action(CCW);
//                    break;
//                case 0:
//                    break;
//                case +1:
//                    state.take_action(CW);
//                    break;
//            }
//        });

//        actions.add(new ActionConcept($("x(tetris)"), nar, (b, d) -> {
//            //float alpha = nar.confidenceDefault(BELIEF);
//
//            if (d != null) {
//                float x = d.freq();
//                float alpha = d.conf();
//                //System.out.println(d + " " + x);
//                if (x > actionThresholdHigh) {
//                    if (state.take_action(RIGHT))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(1, alpha);
//                } else if (x < actionThresholdLow) {
//                    if (state.take_action(LEFT))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(0, alpha);
//                } else {
//                    //return $.t(0.5f, alpha); //no action taken or move ineffective
//                }
//            }
//            //return $.t(0.5f, alpha); //no action taken or move ineffective
//            return null;
//        }));

        //if (rotate) {
//        actions.add(new ActionConcept($("rotate(tetris)"), nar, (b, d) -> {
//
//            if (d != null) {
//                float r = d.freq();
//                float alpha = d.conf();
//                if (r > actionThresholdHigh) {
//                    if (state.take_action(CW))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(1, alpha);
//                } else if (r < actionThresholdLow) {
//                    if (state.take_action(CCW))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(0, alpha);
//                } else {
//                    //return $.t(0.5f, alpha); //no action taken or move ineffective
//                }
//            }
//            //return $.t(0.5f, alpha); //no action taken or move ineffective
//            return null;
//        }));
//        } else {
//            motorRotate = null;
//        }

        //actions.add(motorDown = new MotorConcept("(down)", nar));
//        if (downMotivation > actionThresholdHigh) {
//            state.take_action(FALL);
//        }
    }

    @Override
    public int width() {
        return state.width;
    }

    @Override
    public int height() {
        return state.height;
    }

    @Override
    public float brightness(int xx, int yy) {
        int index = yy * state.width + xx;
        return state.seen[index] > 0 ? 1f : 0f;
    }

//    public void sensors(NAR nar, TetrisState state, List<SensorConcept> sensors) {
//
//
//
//        concept = new SensorConcept[state.width][state.height];
//
//        Atomic tetris = $.the("tetris");
//        for (int y = 0; y < state.height; y++) {
//            int yy = y;
//            for (int x = 0; x < state.width; x++) {
//                int xx = x;
//                Compound squareTerm =
//                        //$.p(x, y);
//
//                        $.inh(
//                                //$.func(
//
//                                 /*   $.p(
//                                            $.pRadix(x, PIXEL_RADIX, state.width),
//                                            $.pRadix(y, PIXEL_RADIX, state.height))*/
//                                $.p( $.pRecurse( $.radixArray(x, PIXEL_RADIX, state.width) ),
//                                     $.pRecurse( $.radixArray(y, PIXEL_RADIX, state.height) ) ),
//
////                                $.secte( $.pRecurseIntersect( 'x', $.radixArray(x, PIXEL_RADIX, state.width) ),
////                                        $.pRecurseIntersect( 'y', $.radixArray(y, PIXEL_RADIX, state.height) ) ),
//
//                                tetris
//                                //        x, y
//
//                        )
//                        //$.p(
//                        //$.the("tetris"))
//                        //, $.the(state.time)))
//                        ;
//
//                //System.out.println(x + " " + y + " "  + squareTerm);
//
//                //$.p($.pRadix(x, 4, state.width), $.pRadix(y, 4, state.height));
//                int index = yy * state.width + xx;
//                @NotNull SensorConcept s = new SensorConcept(squareTerm, nar,
//                        () -> state.seen[index] > 0 ? 1f : 0f,
//
//                        //null //disable input
//
//                        (v) -> $.t(v, nar.confidenceDefault(BELIEF))
//                )
//                        //timing(0, visionSyncPeriod)
//                        ;
//
////                FloatSupplier defaultPri = s.sensor.pri;
////                s.pri( () -> defaultPri.asFloat() * 0.25f );
//
//                concept[x][y] = s;
//
//                s.pri(pixelPri);
//
//                sensors.add(s);
//
//            }
//        }
//    }

//    //TODO
//    public static class NARCam {
//        public int width;/*how wide our board is*/
//        public int height;/*how tall our board is*/
//
//
//    }

//    /**
//     * RLE/scanline input method: groups similar pixels (monochrome) into a runline using a integer range
//     */
//    protected void input() {
//
////        float thresh = 0.5f;
////
////        inputAxis(thresh, true);
////        inputAxis(thresh, false);
//    }

//    private void inputAxis(float thresh, boolean horizontal) {
//        int hh = horizontal ? state.height : state.width;
//        for (int y = 0; y < hh; ) {
//
//            int start = 0, end = 0;
//            int sign = 0;
//
//            int ww = horizontal ? state.width : state.height;
//            for (int x = 0; x < ww; ) {
//
//                int i;
//                if (horizontal)
//                    i = y * ww + x;
//                else
//                    i = x * hh + y;
//
//                float s = state.seen[i];
//
//                if (x == 0) {
//                    //beginning of span
//                    sign = (int) Math.signum(s);
//                } else {
//
//                    if (sign > 0) {
//                        if (s < (thresh)) {
//                            sign = -1;
//                        } else {
//                            end = x;  //continue span
//                        }
//                    }
//                    if (sign < 0) {
//                        if (s > (1f - thresh)) {
//                            sign = +1;
//                        } else {
//                            end = x; //continue span
//                        }
//                    }
//                }
//
//                //if it switched or reach the end of the line
//                if (end != x || (x >= ww - 1)) {
//                    //end of span
//                    if (end - start == 1) {
//                        inputBlock(start, start + 1, sign, horizontal);
//                    } else {
//                        inputSpan(start, end, y, sign, horizontal);
//                    }
//                }
//
//                x++;
//            }
//
//            y++;
//        }
//    }
//
//    private void inputSpan(int start, int end, int axis, int sign, boolean horizontal) {
//
//        Truth t = $.t(sign > 0 ? 1f : 0f,
//                //(float)Math.pow(alpha, end-start)
//                alpha
//        );
//        if (t == null)
//            return; //too low confidence
//
//        Term range = new Termject.IntInterval(start, end);
//        Term fixed = new IntTerm(axis);
//
//        //TODO collect evidence stamp
//        nar.believe(
//                horizontal ? $.p(range, fixed) : $.p(fixed, range),
//                Tense.Present,
//                t.freq(), t.conf() //HACK this parameters sux
//        );
//    }
//
//    private void inputBlock(int x, int y, float v, boolean horizontal) {
//
//        Truth t = $.t(v > 0 ? 1f : 0f,
//                //(float)Math.pow(alpha, end-start)
//                alpha
//        );
//        if (t == null)
//            return; //too low confidence
//
//        //TODO collect evidence stamp
//        nar.believe(
//                horizontal ? $.p(x, y) : $.p(y, x),
//                Tense.Present,
//                t.freq(), t.conf() //HACK this parameters sux
//        );
//    }


    @Override
    public float act() {

        state.timePerFall = Math.round(timePerFall.floatValue());
        state.next();
        return state.score;

    }


//        public static void newTimeWindow(NAR n) {
//            LogIndex li = new LogIndex();
//
//            final int cap = 90;
//            long start = System.currentTimeMillis();
//            long end = start + 5000;
//            float width = 2f;
//            SpaceGraph s = new SpaceGraph(
//                    new NARSpace(n) {
//                        @Override
//                        protected void get(Collection displayNext) {
//                            li.containing(new Rect1D.DefaultRect1D(start, end), x -> {
//                                if (displayNext.size() > cap) {
//                                    return false;
//                                }
//                                Spatial w = space.getOrAdd(x, t -> new ConceptWidget(n, $.the(t.toString()), 1));
//
//                                ((SimpleSpatial) w).moveX((-0.5f + (float) (x.from() - start) / (end - start)) * width, 0.8f);
//                                //((SimpleSpatial)w).moveY(0, 0.4f);
//                                //((SimpleSpatial)w).moveZ(0, 0.4f);
//                                displayNext.add(w);
//                                return true;
//                            });
//                        }
//                    }.with(
//                            new Flatten()
//                            //new Spiral()
//                            //new FastOrganicLayout()
//                    )
//            );
////        ForceDirected forceDirect = new ForceDirected();
////        //forceDirect.repelSpeed = 0.5f;
////        s.dyn.addBroadConstraint(forceDirect);
//
//
//            s.show(1300, 900);
//        }

    public static void main(String[] args) {
        //Param.DEBUG = true;


        NAR nn = NAgentX.runRT((n) -> {
            Tetris a = null;
            try {
                n.freqResolution.set(0.02f);
                n.confResolution.set(0.02f);
                a = new Tetris(n, Tetris.tetris_width, Tetris.tetris_height);
//                a.nar.log();

                //a.durations.setValue(2f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

//            try {
//                InterNAR i = new InterNAR(n, 8, 0);
//                i.runFPS(5);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


            Tetris aa = a;
//            n.onTask((t) -> {
//                if (t.isEternal() && !t.isInput()) {
//                    System.err.println(t.proof());
//                }
//                if (t.isGoal() && (t.term().equals(aa.sad.term()) || t.term().equals(aa.happy.term()))) {
//                    System.err.println(t.proof());
//                }
//
//            });

            return a;
        }, 20f);


//
//        for (int i = 0; i < 10000; i++) {
//            System.out.println(((NARS)n).stats());
//            Util.sleep(500);
//        }
//
//        n.stop();
//        a.stop();

//        //nar.index.print(System.out);
//        n.forEachTask(System.out::println);
//
//        //NAR.printActiveTasks(nar, true);
//        //NAR.printActiveTasks(nar, false);
//        n.printConceptStatistics();
//        new TaskStatistics().add(n).print();


        //NARBuilder.newALANN(clock, 4, 64, 5, 4, 1);

//            n.onCycle(new Runnable() {
//
//                long forgetPeriod = 200;
//                long lastForget = n.time();
//
//                @Override
//                public void run() {
//                    long now = n.time();
//                    if (now - lastForget > forgetPeriod) {
//                        System.err.println("BREATHE");
//                        ((Default)n).core.active.clear();
//                        lastForget = now;
//                    }
//                }
//            });


        //nar.derivedEvidenceGain.setValue(2f);


        //nar.truthResolution.setValue(0.05f);

        //NAR nar = new TaskNAR(32 * 1024, new MultiThreadExecutioner(4, 4096), clock);
//            MySTMClustered stm = new MySTMClustered(nar, 64, '.', 4, false, 2);
//            MySTMClustered stmGoal = new MySTMClustered(nar, 16, '!', 2, false, 1);


        //nar.linkFeedbackRate.setValue(0.05f);

        //newTimeWindow(nar);


//        Random rng = new XorShift128PlusRandom(1);
//        //Multi nar = new Multi(3,512,
//        int maxVol = 32;
//
//        Executioner e = Tetris.exe;
//        ((MultiThreadExecutioner)exe).sync(false);
//
//        Default nar = new Default(1024,
//                256, 2, 3, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*128, maxVol/2, false, e),
//                //new MapDBIndex(new DefaultConceptBuilder(rng), 200000, Executors.newSingleThreadScheduledExecutor()),
//                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 200000, 8192, 2),
//                new FrameClock(), e
//        );
//
//
//        nar.beliefConfidence(0.9f);
//        nar.goalConfidence(0.9f);
//
//        Param.DEBUG_ANSWERS = Param.DEBUG;
//
////        nar.onTask(t -> {
////            long now = nar.time();
////            if (t.isBeliefOrGoal() && t.occurrence() > 1 + now) {
////                System.err.println("\tFUTURE: " + t + "\t vs. PRESENT: " +
////                        ((BeliefTable)(t.concept(nar).tableFor(t.punc()))).truth(now)
////                        //+ "\n" + t.proof() + "\n"
////                );
////            }
////        });
//

        /*
        nar.onCycle((n)->{
            FloatSummaryReusableStatistics inputPri = new FloatSummaryReusableStatistics();
            FloatSummaryReusableStatistics derivPri = new FloatSummaryReusableStatistics();
            FloatSummaryReusableStatistics otherPri = new FloatSummaryReusableStatistics();
            n.tasks.forEach(t -> {
                float tp = t.pri();
                if (tp != tp)
                    return;
                if (t.isInput()) {
                    inputPri.accept(tp);
                } else if (t instanceof DerivedTask) {
                    derivPri.accept(tp);
                } else {
                    otherPri.accept(tp);
                }
            });

            System.out.println("input=" + inputPri);
            System.out.println("deriv=" + derivPri);
            System.out.println("other=" + otherPri);
            System.out.println();
        });
        */

//
//
//        nar.confMin.setValue(0.02f);
//
//
//        nar.compoundVolumeMax.setValue(maxVol);
//        nar.linkFeedbackRate.setValue(0.15f);
//

//
////        nar.on(new TransformConcept("seq", (c) -> {
////            if (c.size() != 3)
////                return null;
////            Term X = c.term(0);
////            Term Y = c.term(1);
////
////            Integer x = intOrNull(X);
////            Integer y = intOrNull(Y);
////            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
////
////
////            return $.inh($.p(X, Y, Z), $.oper("seq"));
////        }));
////        nar.believe("seq(#1,#2,TRUE)");
////        nar.believe("seq(#1,#2,FALSE)");
//
        //n.log();
//        //nar.logSummaryGT(System.out, 0.1f);
//
////		nar.log(System.err, v -> {
////			if (v instanceof Task) {
////				Task t = (Task)v;
////				if (t instanceof DerivedTask && t.punc() == '!')
////					return true;
////			}
////			return false;
////		});
//
//
////        Abbreviation abbr = new Abbreviation(nar, "the", 3, 12,
////                0.01f, 64);
//
//        new Inperience(nar);
//

        //new VariableCompressor(nar);


//
//            Default m = new Default(512, 16, 2, n.random,
//                    new CaffeineIndex(new DefaultConceptBuilder(), 4096, false, n.exe),
//                    new RealTime.DSHalf().durSeconds(0.2f));

//            float metaLearningRate = 0.75f;
//            m.confMin.setValue(0.01f);
//            m.goalConfidence(metaLearningRate);
//            m.termVolumeMax.setValue(24);


//        MetaAgent metaT = new MetaAgent(a
//                //,m
//
//        );
//        metaT.init();
//        //metaT.trace = true;
//        a.onFrame((z)->metaT.cycle());

        //n.onCycle(metaT.nar::cycle);


        //a.trace = true;

        //metaT.nar.log();
//            m.onTask(t -> {
//                if (t instanceof DerivedTask)
//                    System.out.println("meta: " + t);
//            });
        //window(conceptsTreeChart(m, 64), 800, 600);
        //NAgentX.chart( metaT );

//
//            SoNAR s = null;
//            try {
//                s = new SoNAR(nar);
//                s.samples("/tmp/wav");
//                s.listen(t.happy.term());
//                s.listen($("tetris"));
//                s.listen($("0"));
//                s.listen($("1"));
//
////        s.listen($("a:b"));
////        s.listen($("b:c"));
//                //s.join();
//            } catch (LineUnavailableException e) {
//                e.printStackTrace();
//            }


//        NARController meta = new NARController(nar, loop, t);
//
//        newControlWindow(Lists.newArrayList(
//                newCPanel(nar, 256, () -> meta.rewardValue),
//                newBeliefChart(meta, 200)
//
//        ));


        //NAR.printTasks(meta.nar, true);
        //NAR.printTasks(meta.nar, false);
//        nar.forEachActiveConcept(c -> {
//            if (c.volume() < 12)
//                c.print();
//        });
    }

    public static class OfflineTetris {
        public static void main(String[] args) throws Narsese.NarseseException {
            Param.DEBUG = true;

            NAR n = NARS.tmp();
            n.time.dur(4);
            n.freqResolution.set(0.02f);
            n.confResolution.set(0.02f);
            //new Abbreviation(n, "z", 3, 8, 0.1f, 32);

            new Tetris(n, Tetris.tetris_width, Tetris.tetris_height, 2);
            n.run(200);

            //n.concepts().map(x -> x.toString()).sorted().forEach(c -> {
            n.concepts().forEach(c -> {
                System.out.println(c);
                c.tasks().forEach(t -> {
                    System.out.println("\t" + t.toString(true));
                });
                System.out.println();
            });


            n.stats(System.out);
        }
    }

    //    public static class NARController extends NAgent {
//
//        private final NARLoop loop;
//        private final NAR worker;
//        private final NAgent env;
//        private final FloatSupplier learn;
//        private final RangeNormalizedFloat busy;
//        public float score;
//
//
//        @Override
//        protected float act() {
//            //float avgFramePeriodMS = (float) loop.frameTime.getMean();
//
//            //float mUsage = memory();
//            //float targetMemUsage = 0.75f;
//
//            return this.score = (
//                    (1f + learn.asFloat()) *       //learn
////                    (1f + (1f- busy.asFloat())) *  //avoid busywork
//                    (1f + happysad.asFloat())      //boost for motivation change
//
//                    //env.rewardNormalized.asFloat() +
//
////                    1 / (1f + Math.abs(targetMemUsage - mUsage) / (targetMemUsage)) //maintain % memory utilization TODO cache 'memory()' result
//            );
//        }
//
//
//        public NARController( NAR worker, NARLoop loop, NAgent env) {
//
//            super( new Default(384, 4, 3, 2, new XORShiftRandom(2),
//                    //new CaffeineIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 5*100000, false, exe),
//                    new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 100000, 16384, 4),
//
//                    new FrameClock()) {
//                       @Override
//                       protected void initHigherNAL() {
//                           super.initHigherNAL();
//                           cyclesPerFrame.setValue(16);
//                           //ctl.confMin.setValue(0.01f);
//                           //ctl.truthResolution.setValue(0.01f);
//                           beliefConfidence(0.5f);
//                           goalConfidence(0.5f);
//                       }
//                   });
//
//            this.worker = worker;
//            this.loop = loop;
//            this.env = env;
//
//            busy = new RangeNormalizedFloat(()->(float)worker.emotion.busy.getSum());
//            happysad = new RangeNormalizedFloat(()->(float)worker.emotion.happysad());
//            learn = ()->(float)worker.emotion.learning();
//
//            //nar.log();
//            worker.onFrame(nn -> next());
//
//            init(nar);
//            mission();
//        }
//
//        @Override public void next() {
//            super.next();
//            nar.next();
//        }
//
//
//
//        @Override
//        protected void init(NAR n) {
//
//            float sensorResolution = 0.05f;
//
//            float sensorConf = alpha;
//
//            FloatToObjectFunction<Truth> truther = (v) -> $.t(v, sensorConf);
//
//            sensors.addAll(Lists.newArrayList(
//                    new SensorConcept("(motive)", n,
//                            happysad,
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(busy)", n,
//                            new RangeNormalizedFloat(() -> (float) worker.emotion.busy.getSum()),
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(learn)", n,
//                            learn,
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(memory)", n,
//                            () -> memory(),
//                            truther
//                    ).resolution(sensorResolution)
//            ));
//
//            //final int BASE_PERIOD_MS = 100;
//            final int MAX_CONCEPTS_FIRE_PER_CYCLE = 32;
//            final int MAX_LINKS_PER_CONCEPT = 24;
//
//            actions.addAll(Lists.newArrayList(
//                    //cpu throttle
//                    /*new MotorConcept("(cpu)", nar, (b, d) -> {
//                        int newPeriod = Math.round(((1f - (d.expectation())) * BASE_PERIOD_MS));
//                        loop.setPeriodMS(newPeriod);
//                        //System.err.println("  loop period ms: " + newPeriod);
//                        return d;
//                    }),*/
//
//                    //memory throttle
////                    new MotorConcept("(memoryWeight)", nar, (b, d) -> {
////                        ((CaffeineIndex) worker.index).compounds.policy().eviction().ifPresent(e -> {
////                            float sweep = 0.1f; //% sweep , 0<sweep
////                            e.setMaximum((long) (DEFAULT_INDEX_WEIGHT * (1f + sweep * 2f * (d.freq() - 0.5f))));
////                        });
////                        //System.err.println("  loop period ms: " + newPeriod);
////                        return d;
////                    }),
//
//                    new MotorConcept("(confMin)", nar, (b, d) -> {
//                        float MAX_CONFMIN = 0.1f;
//                        float newConfMin = Math.max(Param.TRUTH_EPSILON, MAX_CONFMIN * d.freq());
//                        worker.confMin.setValue(newConfMin);
//                        return d;
//                    }),
////
////                    new MotorConcept("(inputActivation)", nar, (b, d) -> {
////                        worker.inputActivation.setValue(d.freq());
////                        return d;
////                    }),
////
////                    new MotorConcept("(derivedActivation)", nar, (b, d) -> {
////                        worker.derivedActivation.setValue(d.freq());
////                        return d;
////                    }),
//
//                    new MotorConcept("(conceptsPerFrame)", nar, (b, d) -> {
//                        ((Default) worker).core.conceptsFiredPerCycle.setValue((int) (d.freq() * MAX_CONCEPTS_FIRE_PER_CYCLE));
//                        return d;
//                    }),
//
//                    new MotorConcept("(linksPerConcept)", nar, (b, d) -> {
//                        float l = d.freq() * MAX_LINKS_PER_CONCEPT;
//                        l = Math.max(l, 1f);
//                        int vv = (int) Math.floor((float)Math.sqrt(l));
//
//                        ((Default) worker).core.tasklinksFiredPerFiredConcept.setValue(vv);
//                        ((Default) worker).core.termlinksFiredPerFiredConcept.setValue((int)Math.ceil(l / vv));
//                        return d;
//                    }),
//
//                    new MotorConcept("(envCuriosity)", nar, (b, d) -> {
//                        float exp = d.freq();
//                        env.epsilonProbability = exp;
//                        env.gammaEpsilonFactor = exp*exp;
//                        return d;
//                    })
//            ));
//        }
//
//        public final float memory() {
//            Runtime runtime = Runtime.getRuntime();
//            long total = runtime.totalMemory(); // current heap allocated to the VM process
//            long free = runtime.freeMemory(); // out of the current heap, how much is free
//            long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
//            long usedMemory = total - free; // how much of the current heap the VM is using
//            long availableMemory = max - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
//            float ratio = 1f - ((float)availableMemory) / max;
//            //logger.warn("max={}k, used={}k {}%, free={}k", max/1024, total/1024, Texts.n2(100f * ratio), free/1024);
//            return ratio;
//        }
//
//        final RangeNormalizedFloat happysad;
//
//    }
//
//
//    //    static void addCamera(Tetris t, NAR n, int w, int h) {
////        //n.framesBeforeDecision = GAME_DIVISOR;
////        SwingCamera s = new SwingCamera(t.vis);
////
////        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));
////
////        NARCamera.newWindow(s);
////
////        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
////        s.output(w, h);
////
////        n.onFrame(nn -> {
////            s.update();
////        });
////    }

}
//                AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
//                        tetris_width/2, 7 /* states */,
//                        0.05f);
//                view.autoenc = new MatrixView(ac.W.length, ac.W[0].length, arrayRenderer(ac.W));

//                int totalSize = tetris_width*tetris_height;
//                AutoClassifier bc = new AutoClassifier($.the("row4"), nar, sensors,
//                        tetris_width*4, 16,
//                        0.1f);

//                newControlWindow(
//                        new GridSurface(VERTICAL,
//                                charted.stream().map(c -> new BeliefTableChart(nar, c)).collect(toList())
//                        )
//                );


//window(Vis.concepts(nar, 1024), 500, 500);

//STMView.show(stm, 800, 600);
//
//        window(
//                col(
//
//                        //Vis.concepts(nar, 32),
//                        //Vis.agentActions(t, 1200),
//
//                        Vis.budgetHistogram(nar, 24)
//                        /*Vis.conceptLinePlot(nar,
//                                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)),
//                                600)*/
//                ), 1200, 900);



            /*view.plot2 = new GridSurface(HORIZONTAL,
                //conceptLinePlot(nar, Lists.newArrayList( t.happy, t.joy ), (c) -> nar.conceptPriority(c), 256),

                conceptLinePlot(nar, t.actions, (c) -> {
                    try {
                        return nar.concept(c).goals().truth(nar.time()).freq();
                    } catch (NullPointerException npe) {
                        return 0.5f;
                    }
                }, 256)
            );*/

//                {
//                    List<FloatSupplier> li = new ArrayList();
//                    for (int i = 0; i < sensors.size(); i++) {
//                        li.add(sensors.get(i).getInput());
//                    }
//
//                    List<FloatSupplier> lo = new ArrayList();
//                    RangeNormalizedFloat normReward = new RangeNormalizedFloat(() -> rewardValue);
//                    lo.add(normReward);
////
////
//                    LSTMPredictor lp = new LSTMPredictor(li, lo, 1);
////
//
//                    double[] lpp = new double[2];
//                    nar.onFrame(nn -> {
//                        double[] p = lp.next();
//                        System.arraycopy(p, 0, lpp, 0, p.length);
//                        //System.out.println("LSTM: " + Texts.n4(p) + " , " + normReward.asFloat());
//                    });
//
//                    view.lstm = new Plot2D(plotHistory, Plot2D.Line)
//                            .add("Reward (actual)", () -> normReward.asFloat())
//                            .add("Predicted", () -> lpp[0]);
//
//
//                }


//        nar.onFrame(f -> {
//            //view.lstm.update();
//            try {
//                view.term.term.putLinePre(t.summary());
//            } catch (IOException e1) {
//            }
//        });


//                int window = 32;
//                GridSurface camHistory = new GridSurface(HORIZONTAL,
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window * 2)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window * 2))
//                );
//                newControlWindow(12f,4f, new Object[] { camHistory } );

//newControlWindow(view);


//newControlWindow(2f,4f, new Object[] { new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)) } );

//Vis.newBeliefChartWindow(t, 200);

//window(Vis.budgetHistogram(nar, 30), 500, 300);

//Arkancide.newBeliefChartWindow(nar, 200, nar.inputTask("(&&, ((happy) ==>+0 (joy)), ((joy) ==>+0 (happy)), ((happy) <=>+0 (joy))). :|:").term());

//                BeliefTableChart.newBeliefChart(nar, Lists.newArrayList(
//                        sensors.get(0),
//                        sensors.get(1),
//                        sensors.get(2),
//                        sensors.get(3),
//                        sensors.get(4),
//                        sensors.get(5)
//                ), 200);

//NARSpace.newConceptWindow((Default) nar, 32, 8);


//        Iterable<Termed> cheats = Iterables.concat(
//                numericSensor(() -> t.currentX, nar, 0.3f,
//                        "(cursor_x)")
//                        //"(active,a)","(active,b)","(active,c)","(active,d)","(active,e)","(active,f)","(active,g)","(active,h)")
//                        //"I(a)","I(b)","I(c)","I(d)","I(e)","I(f)","I(g)","I(h)")
//                        //"(active,x)")
//                        .resolution(0.5f / t.width),
//                numericSensor(() -> t.currentY, nar, 0.3f,
//                        "(cursor_y)")
//                        //"active:(y,t)", "active:(y,b)")
//                        //"(active,y)")
//                        .resolution(0.5f / t.height)
//        );

//        NAgent n = new NAgent(nar) {
//            @Override
//            public void start(int inputs, int actions) {
//                super.start(inputs, actions);
//
//                List<Termed> charted = new ArrayList(super.actions);
//
//                charted.add(sad);
//                charted.add(happy);
//                Iterables.addAll(charted, cheats);
//
//                if (nar instanceof Default) {
//
//                    new BeliefTableChart(nar, charted).show(600, 900);
//
//                    //BagChart.show((Default) nar, 128);
//
//                    //STMView.show(stm, 800, 600);
//
//
//                    NARSpace.newConceptWindow((Default) nar, 128, 8);
//                }
//
//
//            }
//
//            @Override
//            protected Collection<Task> perceive(Set<Task> inputs) {
//                return super.perceive(inputs);
//            }
//        };


//addCamera(t, nar, 8, 8);


//t.run(runFrames);
//t.run(20000);
