/**
 * Copyright (C) 1997-2010 Junyang Gu <mikejyg@gmail.com>
 * <p>
 * This file is part of javaiPacman.
 * <p>
 * javaiPacman is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * javaiPacman is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with javaiPacman.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.experiment.pacman0;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.time.FrameTime;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.layout.Grid.col;
import static spacegraph.obj.layout.Grid.grid;

/**
 * the java application class of pacman 
 */
public class Pacman extends NAgent {

    final cpcman pacman;
    public static final int cyclesPerFrame = 8;

    final int visionRadius;
    final int itemTypes = 3;
    final static int runCycles = 500;


    final int inputs;

    private final int pacmanCyclesPerFrame = 1;
    int pacMovesPerCycle = 6;

    float bias = -0.05f; //pain of boredom, should be non-zero for the way it's used below
    public float scoretoReward = 1f;

    public Pacman(NAR nar, int ghosts, int visionRadius) {
        this(nar, ghosts, visionRadius, true);
    }
    public Pacman(NAR nar, int ghosts, int visionRadius, boolean window) {
        super(nar);
        pacman = new cpcman(ghosts, window) {
            @Override
            public void killedByGhost() {
                super.killedByGhost();
                deathPain -= 1f;
            }
        };
        this.visionRadius = visionRadius;
        this.inputs = (int) Math.pow(visionRadius * 2 + 1, 2) * itemTypes;

        FloatToObjectFunction<Truth> truther = (f) -> $.t(f, alpha);

        for (int ii = -visionRadius; ii <= +visionRadius; ii++) {
            int i = ii;
            for (int jj = -visionRadius; jj <= +visionRadius; jj++) {
                int j = jj;

                sensors.add(new SensorConcept( $.inh($.p(i, j), $.the("wall")), nar,
                        () -> {
                            return at(i, j)==cmaze.WALL ? 1f : 0;
                        },
                        truther));

                sensors.add(new SensorConcept( $.inh($.p(i, j), $.the("dot")), nar,
                        () -> {
                            int v = at(i, j);
                            float dotness = 0;
                            switch (v) {
                                case cmaze.DOT:
                                    dotness = 0.85f;
                                    break;
                                case cmaze.POWER_DOT:
                                    dotness = 1f;
                                    break;
                            }
                            return dotness;
                        },
                        truther));

                sensors.add(new SensorConcept( $.inh($.p(i, j), $.the("ghost")), nar,
                        () -> {
                            int pix = Math.round(pacman.pac.iX / 16f);
                            int piy = Math.round(pacman.pac.iY / 16f);
                            int px = i+pix;
                            int py = j+piy;

                            for (int i1 = 0, ghostsLength = pacman.ghosts.length; i1 < ghostsLength; i1++) {
                                cghost g = pacman.ghosts[i1];
//							int ix = g.iX / 16;
//							int iy = g.iY / 16;
                                int ix = Math.round(g.iX / 16f);
                                int iy = Math.round(g.iY / 16f);
                                if (ix == px && iy == py) {
                                    return 1;
                                }
                            }
                            return 0;
                        },
                        truther));

            }


        }

        @Nullable Truth zero = $.t(0.5f, alpha);

        actions.add(new ActionConcept("(leftright)",nar,(b, d)->{
            if (d!=null) {

                float f =
                        //d.expectation();
                        d.freq();
                int sc = Math.round(2f * (f -0.5f) * pacMovesPerCycle);
                if (sc < 0) {
                    if (!pacman.pac.move(ctables.LEFT, -sc, pacman))
                        return zero;
                } else if (sc > 0) {
                    if (!pacman.pac.move(ctables.RIGHT, sc, pacman))
                        return zero;
                } else {
                    return zero;
                }
                //return d;
                return d.withConf(alpha);
            }
            return null;
        }));

        actions.add(new ActionConcept("(updown)",nar,(b, d)->{
            if (d!=null) {
                float f =
                        //d.expectation();
                        d.freq();

                int sc = Math.round(2f * (f-0.5f) * pacMovesPerCycle);
                if (sc < 0) {
                    if (!pacman.pac.move(ctables.UP, -sc, pacman))
                        return zero;
                } else if (sc > 0) {
                    if (!pacman.pac.move(ctables.DOWN, sc, pacman))
                        return zero;
                } else {
                    return zero;
                }
                //return d;
                return d.withConf(alpha);
            }
            return null;
        }));


        //PAC-GPS
        //new NObj("pcpman", pacman, nar).read("pac.iX", "pac.iY").into(this);
    }


    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        //Param.CONCURRENCY_DEFAULT = 2;

        //Multi nar = new Multi(3,512,

        Executioner e =
                new MultiThreadExecutioner(2, 8192);
                //new SingleThreadExecutioner();

        Default nar = new Default(1024,
                16, 2, 3, rng,
                new CaffeineIndex(new DefaultConceptBuilder(), 25 * 100000, false, e),
                //new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(rng), 150000, 8192, 2),
                new FrameTime(), e

        );
        //Object queryIntroducer = queryVariableIntroduction(nar, 100, 0.1f, 0.25f);

        //new MemoryManager(nar);

        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.7f);

        float pMult = 0.01f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * pMult;
        nar.DEFAULT_GOAL_PRIORITY = 1f * pMult;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f * pMult;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * pMult;
        nar.linkFeedbackRate.setValue(0.1f);

        nar.confMin.setValue(0.04f);
        nar.termVolumeMax.set(32);

        //nar.truthResolution.setValue(0.02f);

        //nar.inputAt(100,"$1.0;0.8;1.0$ ( ( ((#x,?r)-->#a) && ((#x,?s)-->#b) ) ==> col:(#x,#a,#b) ). %1.0;1.0%");
        //nar.inputAt(100,"$1.0;0.8;1.0$ col:(?c,?x,?y)?");

        //nar.inputAt(20,"$1.0;0.8;1.0$ rightOf:((0,#x),(1,#x)). %1.0;1.0%");
        //nar.inputAt(20,"$1.0;0.8;1.0$ rightOf:((1,#x),(2,#x)). %1.0;1.0%");
        //nar.inputAt(20,"$1.0;0.8;1.0$ ( ( (($x,$y)-->$a) && (($x,$y)-->$b) ) ==> samePlace:(($x,$y),$a,$b) ). %1.0;1.0%");
        //nar.inputAt(100,"$1.0;0.8;1.0$ samePlace:(#x,#y,#a,#b)?");

        //nar.log();
        //nar.logSummaryGT(System.out, 0.1f);

//		nar.log(System.err, v -> {
//			if (v instanceof Task) {
//				Task t = (Task)v;
//				if (t instanceof DerivedTask && t.punc() == '!')
//					return true;
//			}
//			return false;
//		});

        //Param.DEBUG = true;

        //new Abbreviation2(nar, "_");
        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3);
        MySTMClustered stmGoal = new MySTMClustered(nar, 64, '!', 2);


        Pacman p = new Pacman(nar, 7 /* ghosts  */, 6 /* visionRadius */);
        p.trace = true;



//				charted.add(nar.activate($.$("[pill]"), UnitBudget.Zero));
//				charted.add(nar.activate($.$("[ghost]"), UnitBudget.Zero));

        //PAC GPS global positioining
//        Iterable<Termed> cheats = Iterables.concat(
//                numericSensor(() -> pacman.pac.iX, nar, 0.3f,
//                        "(pacX)").resolution(0.1f),
//                numericSensor(() -> pacman.pac.iY, nar, 0.3f,
//                        "(pacY)").resolution(0.1f)
//        );
        //Iterables.addAll(charted, cheats);

        //charted.add(nar.ask($.$("(a:?1 ==> (I-->happy))")).term());
        //charted.add(nar.ask($.$("((I-->be_happy) <=> (I-->happy))")).term());

//				charted.add(nar.ask($.$("((a0) &&+2 (happy))")).term());
//				charted.add(nar.ask($.$("((a1) &&+2 (happy))")).term());
//				charted.add(nar.ask($.$("((a2) &&+2 (happy))")).term());
//				charted.add(nar.ask($.$("((a3) &&+2 (happy))")).term());
//				charted.add(nar.ask($.$("((a0) &&+2 (sad))")).term());
//				charted.add(nar.ask($.$("((a1) &&+2 (sad))")).term());
//				charted.add(nar.ask($.$("((a2) &&+2 (sad))")).term());
//				charted.add(nar.ask($.$("((a3) &&+2 (sad))")).term());


        //NAL9 emotion feedback loop
//				Iterables.addAll(charted,
//					bipolarNumericSensor(nar.self.toString(),
//						"be_sad", "be_neutral", "be_happy", nar, ()->(float)(Util.sigmoid(nar.emotion.happy())-0.5f)*2f, 0.5f).resolution(0.1f));

//				Iterables.addAll(charted,
//						numericSensor(nar.self.toString(),
//								"unmotivationed", "motivated", nar, ()->(float)nar.emotion.motivation.getSum(), 0.5f).resolution(0.1f));

////				{
//					List<FloatSupplier> li = new ArrayList();
//					for (int i = 0; i < this.inputs.size(); i++) {
//						li.add( this.inputs.get(i).getInput() );
//					}
////					for (int i = 0; i < cheats.size(); i++) {
////						FloatSupplier ci = cheats.get(i).getInput();
////						//li.add( new DelayedFloat(ci, 2) );
////						//li.add( new DelayedFloat(new RangeNormalizedFloat(()->reward), 1) );
////						li.add( ci );
////					}
//					List<FloatSupplier> lo = new ArrayList();
//					RangeNormalizedFloat normReward = new RangeNormalizedFloat(() -> reward);
//					lo.add(normReward);
////
////
//					LSTMPredictor lp = new LSTMPredictor(li, lo, 1);
////
//					nar.onFrame(nn->{
//						double[] p = lp.next();
//						System.out.println(Texts.n4(p) + " , " + normReward.asFloat() );
//					});
////				}


//        if (nar instanceof Default) {
//
//          //new BeliefTableChart(nar, charted).show(700, 900);


        int history = 2000;
        window(
            grid(
                    //new CameraSensorView(pacman.pixels, nar),
                    Vis.agentActions(p, history),
                    Vis.conceptsTreeChart(nar, 64),
                    col(
                            Vis.budgetHistogram(nar, 50),
                            Vis.conceptLinePlot(nar,
                                    Iterables.concat(p.actions, Lists.newArrayList(p.happy, p.joy)),
                                    200, nar::priority)
                    )
            ), 500, 500);

////
        //BagChart.show((Default) nar, 512);
////
//            //STMView.show(stm, 500, 500);
//
//            //TimeSpace.newTimeWindow((Default)nar, 128);
//            //NARSpace.newConceptWindow((Default) nar, 128, 6);
//
//
//        }


//		SwingCamera swingCam = new SwingCamera(pacman);
//		NARCamera camera = new NARCamera("pacmap", nar, swingCam, (x, y) -> {
//			return $.p($.the(x), $.the(y));
//		});
//
//		swingCam.input(200,200, 32, 32);
//		swingCam.output(64,64);
//
//		NARCamera.newWindow(camera);

//        nar.onFrame(nn -> {
//
//            //camera.center(pacman.pac.iX, pacman.pac.iY); //center on nars
//
//
////			camera.updateMono((x,y,t,w) -> {
////				//nar.believe(t, w, 0.9f);
////			});
//
//        });


//        Param.DEBUG = true;
//        nar.onTask(t->{
//           if (t.op()==PROD && t.term().size()==2) {
//               System.out.println(t);
//           }
//        });
        p.run(runCycles);

//		pacman.run(
//				//new DQN(),
//				//new DPG(),
//				//new HaiQAgent(),
//				n,
//				runCycles,
//				runDelay);

        NAR.printActiveTasks(nar, true);
        NAR.printActiveTasks(nar, false);
        //n.printActions();
        //nar.forEachActiveConcept(System.out::println);

        //Derive.printStats(nar);

        //nar.index.print(System.out);



//		nar.index.forEach(t -> {
//			if (t instanceof Concept) {
//				Concept c = (Concept)t;
//				if (c.hasQuestions()) {
//					System.out.println(c.questions().iterator().next());
//				}
//			}
//		});
    }


    protected int at(int i, int j) {
        int pix = Math.round(pacman.pac.iX / 16f);
        int piy = Math.round(pacman.pac.iY / 16f);
        int px = pix + i;
        int py = piy + j;

        int[][] m = pacman.maze.iMaze;
        if (amazed(m, px, py)) {
            return m[py][px];
        } else {
            return cmaze.WALL;
        }
    }
    private boolean amazed(int[][] m, int px, int py) {
        return px >= 0 && py >= 0 && py < m.length && px < m[0].length;
    }

    float lastScore;


//    final Atom PILL = $.the("pill");
//    final Atom WALL = $.the("wall");
//    final Atom GHOST = $.the("ghost");


//    @Override
//    public void preStart(Agent a) {
//        if (a instanceof NAgent) {
//            //provide custom sensor input names for the nars agent
//
//            int visionDiameter = 2 * visionRadius + 1;
//
//            NAgent nar = (NAgent) a;
//
//
//            nar.setSensorNamer((i) -> {
//                int cell = i / 3;
//                int type = i % 3;
//                Atom typeTerm;
//                switch (type) {
//                    case 0:
//                        typeTerm = WALL;
//                        break;
//                    case 1:
//                        typeTerm = PILL;
//                        break;
//                    case 2:
//                        typeTerm = GHOST;
//                        break;
//                    default:
//                        throw new RuntimeException();
//                }
//
//                int ax = cell % visionDiameter;
//                int ay = cell / visionDiameter;
//
//                //Term squareTerm = $.p($.the(ax), $.the(ay));
//
//                int dx = (visionRadius) - ax;
//                int dy = (visionRadius) - ay;
//
//                Term squareTerm = $.p($.the(dx), $.the(dy));
//
////				Atom dirX, dirY;
////				if (dx == 0) dirX = $.the("v"); //vertical
////				else if (dx > 0) dirX = $.the("r"); //right
////				else /*if (dx < 0)*/ dirX = $.the("l"); //left
////				if (dy == 0) dirY = $.the("h"); //horizontal
////				else if (dy > 0) dirY = $.the("u"); //up
////				else /*if (dy < 0)*/ dirY = $.the("d"); //down
////				Term squareTerm = $.p(
////						//$.p(dirX, $.the(Math.abs(dx))),
////						$.p(new IntTerm(Math.abs(dx)), dirX),
////						//$.p(dirY, $.the(Math.abs(dy)))
////						$.p(new IntTerm(Math.abs(dy)), dirY)
////				);
//                //System.out.println(dx + " " + dy + " " + squareTerm);
//
//                //return $.p(squareTerm, typeTerm);
//                return $.inst(squareTerm, typeTerm);
//                //return (Compound)$.inh($.the(square), typeTerm);
//            });
//        }
//    }


    @Override
    protected float act() {

        //update maze limits by not moving
        pacman.pac.move(ctables.LEFT, 0, pacman);


        //delta score from pacman game
        float ds = (pacman.score - lastScore) * scoretoReward;
        this.lastScore = pacman.score;


        //ds/=2f;

        ds += bias;

        ds += deathPain;
        deathPain *= 0.95f;

        if (ds > 1f) ds = 1f;
        if (ds < -1f) ds = -1f;

//
//        if (deathPain < bias * 2f) {
//            //too much pain
//            pacman.pacKeyDir = -1;
//        }
        pacman.cycle(pacmanCyclesPerFrame);


        return ds;

    }

//    @Override
//    public void post(int t, int action, float[] ins, Agent a) {
//
//
//        switch (action) {
//            case 0:
//                pacKeyDir = ctables.RIGHT;
//                break;
//            case 1:
//                pacKeyDir = ctables.UP;
//                break;
//            case 2:
//                pacKeyDir = ctables.LEFT;
//                break;
//            case 3:
//                pacKeyDir = ctables.DOWN;
//                break;
//        }
//
//
//
//        if (trace)
//            System.out.println(a.summary());
//
//		/*static final int BLANK=0;
//		static final int WALL=1;
//		static final int DOOR=2;
//		static final int DOT=4;
//		static final int POWER_DOT=8;*/
//
//    }

    float deathPain = -1; //start dead



//    private static class MemoryManager implements Consumer<NAR> {
//
//        private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);
//        private final Default nar;
//        private final CaffeineIndex index;
//
//        float linkMinLow = 4, linkMaxLow = 8, linkMinHigh = 16, linkMaxHigh = 32;
//        float confMinMin = 0.15f, confMinMax = 0.05f;
//        float durMinMin = 0.1f, durMinMax = Param.BUDGET_EPSILON * 4f;
//
//        public MemoryManager(Default nar) {
//
//            this.nar = nar;
//
//            this.index = ((CaffeineIndex) nar.index);
//
//            nar.onFrame(this);
//        }
//
//        @Override
//        public void accept(NAR nar) {
//            update();
//        }
//
//        protected void update() {
//            Runtime runtime = Runtime.getRuntime();
//            long total = runtime.totalMemory(); // current heap allocated to the VM process
//            long free = runtime.freeMemory(); // out of the current heap, how much is free
//            long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
//            long usedMemory = total - free; // how much of the current heap the VM is using
//            long availableMemory = max - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
//            float ratio = 1f - ((float) availableMemory) / max;
//
//
//            logger.warn("max={}k, used={}k {}%, free={}k", max / 1024, total / 1024, Texts.n2(100f * ratio), free / 1024);
//
////			nar.conceptCold.termlinksCapacityMin.set(
////					(int)Util.lerp(linkMinLow, linkMinHigh, ratio)/2
////			);
////			nar.conceptCold.termlinksCapacityMax.set(
////					(int)Util.lerp(linkMaxLow, linkMaxHigh, ratio)/2
////			);
////			nar.conceptWarm.termlinksCapacityMin.set(
////					(int)Util.lerp(linkMinLow, linkMinHigh, ratio)
////			);
////			nar.conceptWarm.termlinksCapacityMax.set(
////					(int)Util.lerp(linkMaxLow, linkMaxHigh, ratio)
////			);
//            nar.confMin.setValue(
//                    Util.lerp(confMinMin, confMinMax, ratio)
//            );
//            nar.durMin.setValue(
//                    Util.lerp(durMinMin, durMinMax, ratio)
//            );
//            nar.cyclesPerFrame.setValue(
//                    (int) Util.lerp(24, 32, ratio * ratio)
//            );
////			index.setWeightFactor(
////					Util.lerp(1, 16, ratio*ratio)
////			);
//
//            //int targetSize = 8 * 1024;
//
//            //int m = (int) Util.lerp(2, 100, ratio) * targetSize;
//
//            Consumer<Policy.Eviction> evictionConsumer = e -> {
//                float warningRatio = 0.75f;
//                if (ratio > warningRatio) {
//                    float over = ratio - warningRatio;
//                    e.setMaximum((long) (e.weightedSize().getAsLong() * (1f - over / 2f))); //shrink
//                } else {
//                    e.setMaximum((long) (Math.max(e.getMaximum(), e.weightedSize().getAsLong()) * 1.05f)); //grow
//                }
//            };
//            index.compounds.policy().eviction().ifPresent(evictionConsumer);
//
//            if (ratio > 0.75f) {
//
//                //index.data.cleanUp();
//
//                //logger.error("{}", index.data.stats());
//
//            }
//
//            if (ratio > 0.95f) {
//                logger.error("memory alert");
//                //System.gc();
//            }
//        }
//    }
}


