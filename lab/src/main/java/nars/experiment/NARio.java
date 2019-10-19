package nars.experiment;

import jcog.math.FloatRange;
import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.attention.PriNode;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.MarioComponent;
import nars.experiment.mario.Scene;
import nars.experiment.mario.level.Level;
import nars.experiment.mario.sprites.Mario;
import nars.game.Reward;
import nars.game.action.AbstractGoalActionConcept;
import nars.game.action.ActionSignal;
import nars.game.action.BiPolarAction;
import nars.game.action.GoalActionConcept;
import nars.game.sensor.DigitizedScalar;
import nars.game.sensor.SelectorSensor;
import nars.gui.NARui;
import nars.sensor.PixelBag;
import nars.video.AutoclassifiedBitmap;

import javax.swing.*;
import java.util.List;
import java.util.stream.IntStream;

import static nars.$.$$;
import static nars.experiment.mario.level.Level.*;
import static nars.game.GameTime.fps;
import static spacegraph.SpaceGraph.window;

public class NARio extends GameX {

	static final float fps = 24;
//    private final AbstractSensor cam;
	static final int tileTypes = 3; //0..2
	//    public final FloatRange MoveRight = new FloatRange(0.75f, 0f, 1f);
	public final FloatRange EarnCoin = new FloatRange(0.95f, 0f, 1f);
	private final MarioComponent game;
	int lastCoins;
	float lastX;
	private Mario theMario = null;

	public NARio(NAR nar) {
		super($$("nario"), fps(fps), nar);


		game = new MarioComponent(
			640, 480
		);
		JFrame frame = new JFrame("Infinite NARio");
		frame.setIgnoreRepaint(true);

		frame.setContentPane(game);

		frame.pack();
		frame.setResizable(false);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocation(0, 0);


		frame.setVisible(true);


		PixelBag cc = new PixelBag(new MonoBufImgBitmap2D(() -> game.image), 32, 24) {
			{
				panRate = 1;
				zoomRate = 1;
			}

			@Override
			protected float missing() {
				return 0f;
			}
		};

		cc.addActions(id, this, false, false, true);
		//addCamera(new Bitmap2DSensor(id, cc, nar));

		for (ActionSignal a : cc.actions) {
			a.resolution(0.5f);
		}


//        Bitmap2DSensor ccb;
//        addCamera(ccb = new Bitmap2DSensor(id, cc, this.nar)).resolution(0.02f);

//        AutoConceptualizer ac;
//        addSensor(ac = new AutoConceptualizer(new FasterList(ccb.concepts), true, 8 , this.nar));
//        nar.runLater(()->{
//            SpaceGraph.window(new BitmapMatrixView(ac.ae.W) {
//                {
//                    onFrame(this::update);
//                }
//            }, 500, 500);
//        });


		int nx = 4;
		AutoclassifiedBitmap camAE = new AutoclassifiedBitmap($.p(id,"cam"), cc, nx, nx, (subX, subY) -> new float[]{/*cc.X, cc.Y, */cc.Z}, 12, this);
		camAE.confResolution.set(0.1f);
		camAE.resolution(0.1f);
		camAE.alpha(0.03f);
		camAE.noise.set(0.02f);


//        Atomic LEFT = $.the("left");
//        Atomic RIGHT = $.the("right");
//        Atomic UP = $.the("up");
//        Atomic DOWN = $.the("down");
		List<SelectorSensor> tileSensors = List.of(
			tileSwitch(-1, 0),
			tileSwitch(+1, 0),
			tileSwitch(0, -1),
			tileSwitch(0, +1),
			tileSwitch(-1, -1),
			tileSwitch(+1, -1),
			tileSwitch(-1, +1),
			tileSwitch(+1, +1)
		);

		nar.runLater(() -> {//HACK
			PriNode tileAttnGroup = new PriNode(tileSensors);
			nar.control.input(tileAttnGroup, sensorPri);

			for (SelectorSensor s : tileSensors)
				nar.control.input(s.pri, tileAttnGroup);
		});

		window(camAE.newChart(), 500, 500);

//        SpaceGraph.window(new LabeledPane("Tile types",
//                new Gridding(tileSensors.stream().map(z -> new VectorSensorView(z, nar).withControls()).collect(toList()))), 100, 100);


		onFrame((z) -> {

			Scene scene1 = game.scene;

			if (scene1 instanceof LevelScene) {
				LevelScene level = (LevelScene) game.scene;
				theMario = level.mario;
				float xCam = level.xCam;
				float yCam = level.yCam;
				Mario M = level.mario;
				float x = (M.x - xCam) / 320f;
				float y = (M.y - yCam) / 240f;
				cc.setXRelative(x);
				cc.setYRelative(y);
				cc.setMinZoom(1);
			} else {
				theMario = null;
			}

		});


		initButton();
		//initBipolar();


		DigitizedScalar vx = senseNumberDifferenceBi($.p(id, $.p("v", "x")), 8, () -> theMario != null ? theMario.x : 0)
			.resolution(0.25f);
		DigitizedScalar vy = senseNumberDifferenceBi($.p(id, $.p("v", "y")), 8, () -> theMario != null ? theMario.y : 0)
			.resolution(0.25f);

//        window(NARui.beliefCharts(this.nar, Stream.of(vx, vy).flatMap(x->x.sensors.stream()).collect(toList())), 400, 300);


		Reward right = reward("right", 1f, () -> {

			float reward;
			float curX = theMario != null && theMario.deathTime <= 0 ? theMario.x : Float.NaN;
			int thresh = 1;
			if ((curX == curX && lastX == lastX) && lastX < curX - thresh) {
				reward = //unitize(Math.max(0, (curX - lastX)) / 16f * MoveRight.floatValue());
					1;
			} else {
				reward =
					0;
				//-1;
				//Float.NaN;
			}
			lastX = curX;

			return reward;
		});
		//right.setDefault($.t(0, 0.75f));

		Reward getCoins = rewardNormalized("money", 1f, 0, +1, () -> {
			int coins = Mario.coins;
			int deltaCoin = coins - lastCoins;
			if (deltaCoin <= 0)
				return 0;

			float reward = deltaCoin * EarnCoin.floatValue();
			lastCoins = coins;
			return reward;
		});
		//getCoins.setDefault($.t(0, 0.75f));

		Reward alive = rewardNormalized("alive", 1f, -1, +1, () -> {
//            if (dead)
//                return -1;
//
			if (theMario == null) {
				return Float.NaN;
			}

			float t = theMario.deathTime > 0 ? -1 : /*Float.NaN*/ +1;
//            if (t == -1) {
//                System.out.println("Dead");
//                theMario.deathTime = 0;
//                dead = true;
			//mario.levelFailed(); //restart level
//                nar.runAt(nar.time() + theMario.AFTERLIFE_TIME, ()->{
//                    dead = false;
//                });
//            }
			return t;
		});
		//alive.setDefault($.t(1, 0.75f));

		game.paused = false;
		game.thread.start();
	}

	public static void main(String[] args) {


		runRT(n -> {


            NARio x = new NARio(n);
			n.add(x);
//            n.freqResolution.setAt(0.02f);
//            n.confResolution.setAt(0.01f);


			return x;


		}, 2 * fps);


	}

	@Override
	protected void starting(NAR nar) {

		super.starting(nar);

		if (game != null)
			game.paused = false;
	}

	@Override
	protected void stopping(NAR nar) {
		game.paused = true;
		super.stopping(nar);
	}

	private SelectorSensor tileSwitch(int dx, int dy) {
		return senseSwitch(tileTypes, () -> tile(dx, dy), i -> $.inh($.p(dx, dy), $.p($.the("tile"), $.the(i)))
		);
	}

	int tile(int dx, int dy) {
		if (this.game.scene instanceof LevelScene) {
			LevelScene s = (LevelScene) game.scene;
			Level ll = s.level;
			if (ll != null) {
				//System.out.println(s.mario.x + " " + s.mario.y);
				byte block = ll.getBlock(Math.round((s.mario.x - 8) / 16f) + dx, Math.round((s.mario.y - 8) / 16f) + dy);
				byte t = Level.TILE_BEHAVIORS[block & 0xff];
				boolean breakable = IntStream.of(BIT_BREAKABLE, BIT_PICKUPABLE, BIT_BUMPABLE).anyMatch(i -> ((t & i) != 0));
				if (breakable)
					return 2;
				boolean blocking = ((t & BIT_BLOCK_ALL) != 0);
				if (blocking)
					return 1;
			}

		}
		return 0;
	}

	private void initButton() {

		actionPushButtonMutex(
			$.inh(id, $$("L")),
			$.inh(id, $$("R")),
				n -> {
				Scene s = game.scene;
				boolean was = s != null && Scene.key(Mario.KEY_LEFT, n);
				return n;
			},
				n -> {
				Scene s = game.scene;
				boolean was = s != null && Scene.key(Mario.KEY_RIGHT, n);
				return n;
			});

		GoalActionConcept j = actionPushButton($.inh(id, $$("jump")),
			n -> {

//                    Scene s = game.scene;
//                    int jumpTime = s instanceof LevelScene ? ((LevelScene) s).mario.jumpTime : 0;
//                    //System.out.println(jumpTime);
//                    boolean jumping = jumpTime > 0;
//                    boolean wasPressed = game.scene.key(Mario.KEY_JUMP);
//
//                    boolean press;
//                    if (!n) {
//                        press = wasPressed || (!wasPressed && jumping);
//                    } else {
//
//
//
//
////                        //System.out.println(jumping + " " + (s instanceof LevelScene ? ((LevelScene) s).mario.jumpTime : 0));
////                        if (wasPressed && !jumping) {
////                            press = false;
////                        } else {
////                            press = (!wasPressed) || jumping;
////                        }
//                        if (!wasPressed || (wasPressed && jumping))
//                            press = true;
//                        else
//                            press = false;
//                    }
				Scene s = game.scene;
				if (s != null)
					Scene.key(Mario.KEY_JUMP, n);
				return n;
			});

		//j.actionDur(1);


//        actionPushButton($$("down"),
//                n -> { game.scene.key(Mario.KEY_DOWN, n); return n; } );

		AbstractGoalActionConcept ss = actionPushButton($.inh(id, $$("speed")),
			n -> {
				Scene s = game.scene;
				if (s != null)
					Scene.key(Mario.KEY_SPEED, n);
				return n;
			});
		//s.actionDur(1);

		//bias
//        j.goalDefault($.t(0, 0.01f), nar);
//        ss.goalDefault($.t(0, 0.01f), nar);
	}

	void initTriState() {
		actionTriState($.inh($.the("x"), id), i -> {
			boolean n, p;
			switch (i) {
				case -1:
					p = false;
					n = true;
					break;
				case +1:
					p = true;
					n = false;
					break;
				case 0:
					p = false;
					n = false;
					break;
				default:
					throw new RuntimeException();
			}
			Scene.key(Mario.KEY_LEFT, n);
			Scene.key(Mario.KEY_RIGHT, p);
			return true;
		});
		actionTriState($.inh($.the("y"), id), i -> {
			boolean n, p;
			switch (i) {
				case -1:
					p = false;
					n = true;
					break;
				case +1:
					p = true;
					n = false;
					break;
				case 0:
					p = false;
					n = false;
					break;
				default:
					throw new RuntimeException();
			}
			Scene.key(Mario.KEY_DOWN, n);

			Scene.key(Mario.KEY_JUMP, p);
			return true;
		});


	}

	public void initBipolar() {
		float thresh = 0.25f;


		BiPolarAction X = actionBipolarFrequencyDifferential($.p(id, $.the("x")), false, (x) -> {
			if (game == null || game.scene == null) return Float.NaN; //HACK

			float boostThresh = 0.75f;
			if (x <= -thresh) {
				Scene.key(Mario.KEY_LEFT, true);
				Scene.key(Mario.KEY_RIGHT, false);
				Scene.key(Mario.KEY_SPEED, x <= -boostThresh);

				return x <= -boostThresh ? -1 : -boostThresh;
			} else if (x >= +thresh) {
				Scene.key(Mario.KEY_RIGHT, true);
				Scene.key(Mario.KEY_LEFT, false);
				Scene.key(Mario.KEY_SPEED, x >= +boostThresh);

				return x >= +boostThresh ? +1 : +boostThresh;
			} else {
				Scene.key(Mario.KEY_LEFT, false);
				Scene.key(Mario.KEY_RIGHT, false);
				Scene.key(Mario.KEY_SPEED, false);


				return 0f;

			}
		});
		BiPolarAction Y = actionBipolarFrequencyDifferential($.p(id, $.the("y")), false, (y) -> {
			if (game == null || game.scene == null) return Float.NaN; //HACK

			if (y <= -thresh) {
				Scene.key(Mario.KEY_DOWN, true);
				Scene.key(Mario.KEY_JUMP, false);
				return -1f;

			} else if (y >= +thresh) {
				Scene.key(Mario.KEY_JUMP, true);
				Scene.key(Mario.KEY_DOWN, false);
				return +1f;

			} else {
				Scene.key(Mario.KEY_JUMP, false);
				Scene.key(Mario.KEY_DOWN, false);

				return 0f;

			}
		});/*.forEach(g -> {
            g.resolution(0.1f);
        });*/

		window(NARui.beliefCharts(nar, List.of(X.pos, X.neg, Y.pos, Y.neg)), 700, 700);
	}

}

/*
public class NARio {
    public static void main(String[] args)
    {
        
        MarioComponent mario = new MarioComponent(
                
                800, 600
        );
        JFrame frame = new JFrame("Infinite NARio");
        frame.setIgnoreRepaint(true);

        frame.setContentPane(mario);
        
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 0);

        

        frame.setVisible(true);

        mario.start();


    }
}
 */