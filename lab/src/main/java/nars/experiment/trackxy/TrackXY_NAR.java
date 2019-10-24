package nars.experiment.trackxy;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.exe.Exe;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.ArrayBitmap2D;
import jcog.test.control.TrackXY;
import jcog.tree.rtree.rect.RectFloat;
import nars.*;
import nars.attention.TaskLinkWhat;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.game.GameTime;
import nars.gui.NARui;
import nars.gui.sensor.VectorSensorChart;
import nars.memory.CaffeineMemory;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.clock.CycleTime;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.jetbrains.annotations.NotNull;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.Texts.n2;
import static nars.$.$$;
import static nars.Op.GOAL;
import static spacegraph.SpaceGraph.window;

public class TrackXY_NAR extends GameX {

	static boolean
		sourceNumerics = true;
	static boolean targetNumerics = false;
	static boolean targetCam = !targetNumerics;
	static boolean gui = true;

	//W = 3, H = 1;
	//W = 5, H = 1;
//    public static final int derivationStrength = 2;

//    static float fps = 16;
//    static int durMS = Math.round(1000/(fps));

	static int dur = 1;

	static float camResolution = 0.1f;
	public final AtomicBoolean trainer = new AtomicBoolean(false);
	public final AtomicBoolean log = new AtomicBoolean(true);
	final Bitmap2DSensor cam;
	private final TrackXY track;

	protected TrackXY_NAR(NAR nar, TrackXY xy) {
		super($$("trackXY"),
			//FrameTrigger.cycles(W*H*2),
			GameTime.durs(1),
			//GameTime.fps(1),
			nar
		);

		int W = xy.W;
		int H = xy.H;
		this.track = new TrackXY(W, H);

		assert (sourceNumerics | targetNumerics | targetCam);

		if (sourceNumerics) {
			senseNumberBi($.inh(id, "sx"), new FloatNormalized(() -> track.cx, 0, W - 1));
			if (H > 1)
				senseNumberBi($.inh(id, "sy"), new FloatNormalized(() -> track.cy, 0, H - 1));
		}


		if (targetNumerics) {
			senseNumberBi($.inh(id, "tx"), new FloatNormalized(() -> track.tx, 0, W));
			if (H > 1)
				senseNumberBi($.inh(id, "ty"), new FloatNormalized(() -> track.ty, 0, H));
		}

		if (targetCam) {
			Bitmap2DSensor<ArrayBitmap2D> c = new Bitmap2DSensor<>(id, track.grid, nar);
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

		//actionAccelerate();

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

		FloatSupplier nearness = () -> Util.sqr(1f - (track.dist() / track.distMax()));
		reward("near", nearness);


//        FloatSupplier notLeft  = () -> ( 1f - Util.max(0,track.tx - track.cx) / track.W );
//        FloatSupplier notRight = () -> ( 1f - Util.max(0,track.cx - track.tx) / track.W );
//        reward("notLeft", notLeft).resolution().set(0.1f);
//        reward("notRight", notRight).resolution().set(0.1f);
//
//        if (track.H > 1) {
//            FloatSupplier notBelow  = () -> ( 1f - Util.max(0,track.ty - track.cy) / track.H );
//            FloatSupplier notAbove = () -> ( 1f - Util.max(0,track.cy - track.ty) / track.H );
//            reward("notBelow", notBelow).resolution().set(0.1f);
//            reward("notAbove", notAbove).resolution().set(0.1f);
//        }


		//rewardNormalized($.the("better"), -0.1f,  +0.1f, new FloatFirstOrderDifference(nar::time, nearness) );

//        }

		track.randomize();

		onFrame(track::act);

		onFrame(() -> {

			if (trainer.getOpaque()) {
				long now = nar.time();
				int durMS = Math.round(dur());
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
		//n.runLater(() -> {

		NAL.DEBUG = true;
		//NAL.TRACE = true;

		GraphEdit2D g = new GraphEdit2D();
		g.resize(800, 800);
		window(g, 1000, 1000);
		g.windoSizeMinRel(0.02f, 0.02f);

		NARS nb = new NARS.DefaultNAR(0, true)
			.time(new CycleTime().dur(dur))
			.index(
				new CaffeineMemory(2 * 1024 * 10)
				//new HijackConceptIndex(4 * 1024, 4)
			);


		NAR n = nb.get();

//        Param.DEBUG = true;
//        n.run(10); //skip:

//        n.beliefConfDefault.set(0.5f);
//        n.goalConfDefault.set(0.5f);

//        n.attn.links.capacity(1024);

        //n.beliefPriDefault.pri(0.01f);
//        n.beliefPriDefault.amp(0.1f);
//        n.questionPriDefault.amp(0.03f);
//        n.questPriDefault.amp(0.05f);


		n.freqResolution.set(0.1f);
		n.confMin.evi(0);
//        n.confResolution.set(0.05f);


		//n.freqResolution.set(0.04f);

		n.termVolMax.set(16);
		//n.dtDither.set(Math.max(1, durMS));


		Deriver d = new Deriver(Derivers.nal(n,
			//6, 8
			//"induction.goal.nal"
			1, 8
			//2, 8
			//, "motivation.nal"
		));
////		.add(new STMLinker(1))
//		.add(new ConjClustering(n, BELIEF,
//			//x -> true,
//			2, 4, Task::isInput
//		)));

		//{
//                    @Override
//                    public float puncFactor(byte conclusion) {
//                        return conclusion == GOAL ? 1 : 0.01f; //super.puncFactor(conclusion);
//                    }
		//};
//        d.timing = new ActionTiming();


//        ((BatchDeriver) d).premisesPerIteration.set(derivationStrength);


//        new STMLinkage(n, 1) {
////                @Override
////                public boolean keep(Task newEvent) {
////                    return newEvent.isGoal();
////                }
//        };


//        ConjClustering cjB = new ConjClustering(n, BELIEF,
//                //x -> true,
//                2, 4, Task::isInput
//        );


//            window(new Gridding(
//                    new AutoSurface(d),
//                    new AutoSurface(cjB)
//
//            ), 400, 300);

		final int W = 4, H = 1;
		//final int W = 3, H = 1;
		//final int W = 3, H = 3;

		TrackXY_NAR a = new TrackXY_NAR(n, new TrackXY(W, H));

//		Table t = DataTable.create(DoubleColumn.create("tx"), DoubleColumn.create("cx"));
//		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//			try {
//				File f = new File("/tmp/x.csv");
//				System.out.println("writing perf metrics: " + f);
//				t.write().csv(f);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}));
//		a.onFrame(f -> {
//			synchronized (t) {
//				((DoubleColumn) t.column(0)).append((double) a.track.tx);
//				((DoubleColumn) t.column(1)).append((double) a.track.cx);
//			}
//		});

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
		n.add(a);
		((TaskLinkWhat) a.what()).links.linksMax.set(256);


		n.synch();

		Exe.runLater(() -> {
			window(NARui.game(a), 800, 600);
			g.add(NARui.top(n)).posRel(0.5f, 0.5f, 0.2f, 0.1f);
			g.add(NARui.attentionUI(n)).sizeRel(0.1f, 0.1f);

			if (a.cam != null) {
				Splitting<?, ?> vv = Splitting.column(new VectorSensorChart(a.cam, a) {
					@Override
					protected void paint(GL2 gl, ReSurface reSurface) {
						super.paint(gl, reSurface);
						RectFloat at = cellRect(a.track.cx, a.track.cy, 0.5f, 0.5f);
						gl.glColor4f(1, 0, 0, 0.9f);
						Draw.rect(at.move(x(), y(), 0.01f), gl);
					}
				}.withControls(), 0.1f, new ObjectSurface(a.track));
				window(vv, 640,480);
				//g.add(vv).posRel(1f, 1f, 0.1f, 0.1f);

				//});
			}

//        new Impiler(n);

			Term L = a.actions.get(0).term();
			Term R = a.actions.get(1).term();
//			a.what().eventTask.on(tt -> {
//				int stampLen = tt.stamp().length;
//				//if (stampLen > 1)
//					System.out.println(stampLen + " " + tt);
//			});
			a.what().eventTask.on(tt -> {


				if (!tt.isEternal()) {
					{
						Term ttt = tt.term();
						boolean l = ttt.equals(L);
						boolean r = ttt.equals(R);
						//System.out.println(tt);
						if (l || r) {


							//if (n.concept(tt) instanceof ActionConcept)
							long window = 5;
							float dur = n.dur();
							long now = n.time();
							if (tt.intersects(Math.round(now - window / 2 * dur), Math.round(now + window / 2 * dur))) {

								float wantsDir = (l ? -1 : +1) * (tt.freq() < 0.5f ? -1 : +1);
								float needsDir = a.track.tx - a.track.cx;


								boolean ok = Math.signum(wantsDir) == Math.signum(needsDir);
								if (!ok) {
									String summary = ok ? "OK" : "WRONG";
									System.out.println();
									System.out.println(ttt + " " + n2(wantsDir) + " ? " + n2(needsDir) + " " + summary);
									System.out.println(tt.proof());
									System.out.println();

//									((TaskLinkWhat)a.what()).links.links.print();
//									System.out.println();
									//n.proofPrint(tt);
								}

								//System.out.println(NAR.proof(tt, n));

							}


						}

					}
				}
			}, GOAL);


		});
		//n.run(experimentTime);
		n.startFPS(10);


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





		//printGoals(n);
		//printImpls(n);

		//n.stats(System.out);
		//n.conceptsActive().forEach(System.out::println);

		//n.tasks().forEach(System.out::println);
	}

//    public static void printGoals(NAR n) {
//        float dur = n.dur();
//        long now = n.time();
//        List<Task> l = n.tasks(false, false, true, false)
//                .sorted(Comparators.byFloatFunction(tt -> (float) -tt.evi(now, dur)))
//                .collect(toList());
//        l.forEach(System.out::println);
//        System.out.println();
//    }


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
		actionUnipolar($.inh(id, $.the("speed")), a -> {
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

	@SuppressWarnings("NonAtomicOperationOnVolatileField")
	private void actionTriState() {

		if (track.grid.height() > 1) {
			actionTriState($.p($.the("Y"), id), (dy) -> {
				float py = track.cy;
				//noinspection NonAtomicOperationOnVolatileField
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

		actionPushButton($.inh(id, "right"), movement(+1, 0));
		actionPushButton($.inh(id, "left"), movement(-1, 0));

		if (track.grid.height() > 1) {
			actionPushButton($.inh(id, "up"), movement(0, +1));
			actionPushButton($.inh(id, "down"), movement(0, -1));
		}
	}

	private @NotNull BooleanPredicate movement(float dx, float dy) {
		return (b) -> {
			if (b) {
				float speed = track.controlSpeed.floatValue();
				float nextCX = Util.clamp(track.cx + dx * speed, 0, track.grid.width() - 1);
				float nextCY = Util.clamp(track.cy + dy * speed, 0, track.grid.height() - 1);
				if (track.cx != nextCX || track.cy!=nextCY) {
					track.cx = nextCX;
					track.cy = nextCY;
					return true;
				}
			}
			return false;
		};
	}

	private void actionPushButtonMutex() {

		if (track.grid.height() > 1) {
			actionPushButtonMutex($.inh(id, $.the("up")), $.inh(id, $.the("down")), (b) -> {
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

		actionPushButtonMutex($.inh(id, $.the("left")), $.inh(id, $.the("right")), (b) -> {
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
