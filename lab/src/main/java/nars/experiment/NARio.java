package nars.experiment;

import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.action.BiPolarAction;
import nars.concept.action.GoalActionConcept;
import nars.concept.sensor.Signal;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.MarioComponent;
import nars.experiment.mario.Scene;
import nars.experiment.mario.level.Level;
import nars.experiment.mario.sprites.Mario;
import nars.gui.NARui;
import nars.sensor.Bitmap2DSensor;
import nars.video.AutoclassifiedBitmap;
import nars.video.PixelBag;
import spacegraph.SpaceGraph;

import javax.swing.*;

import static nars.$.$$;
import static nars.agent.FrameTrigger.fps;
import static nars.experiment.mario.level.Level.*;
import static spacegraph.SpaceGraph.window;

public class NARio extends NAgentX {

    private final MarioComponent game;
//    private final AbstractSensor cam;

    static final float fps = 24;
    private Mario theMario = null;

    public NARio(NAR nar) {
        super("nario", fps(fps), nar);


//        nar.freqResolution.set(0.1f);


        game = new MarioComponent(

                640, 480
        );
        JFrame frame = new JFrame("Infinite NARio");
        frame.setIgnoreRepaint(true);

        frame.setContentPane(game);

        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 0);


        frame.setVisible(true);


        game.start();

        PixelBag cc = PixelBag.of(() -> game.image, 32, 24);
        cc.addActions(id, this, false, false, true);
        cc.actions.forEach(a -> a.resolution(0.5f));


        Bitmap2DSensor ccb;
        addCamera(ccb = new Bitmap2DSensor(id, cc, this.nar)).resolution(0.02f);

//        AutoConceptualizer ac;
//        addSensor(ac = new AutoConceptualizer(new FasterList(ccb.concepts), true, 8 , this.nar));
//        nar.runLater(()->{
//            SpaceGraph.window(new BitmapMatrixView(ac.ae.W) {
//                {
//                    onFrame(this::update);
//                }
//            }, 500, 500);
//        });


        onFrame(cc::update);
        int nx = 4;
        AutoclassifiedBitmap camAE = new AutoclassifiedBitmap($.inh("cae", id), cc.pixels, nx, nx, (subX, subY) -> {
            return new float[]{/*cc.X, cc.Y, */cc.Z};
        }, 12, this);
        camAE.alpha(0.15f);
        SpaceGraph.window(camAE.newChart(), 500, 500);

        try {
            final int tileMax = 3; //0..4
            senseSwitch($$("tile(nario,right)"), () -> {
                int b = tile(1, 0);
                //System.out.println("right: " + b);
                return b;
            }, 0, tileMax);
            senseSwitch($$("tile(nario,below)"), () -> {
                int b = tile(0, 1);
                //System.out.println("below: " + b);
                return b;
            }, 0, tileMax);
            senseSwitch($$("tile(nario,left)"), () -> {
                int b = tile(-1, 0);
                //System.out.println("left: " + b);
                return b;
            }, 0, tileMax);
            senseSwitch($$("tile(nario,above)"), () -> {
                int b = tile(0, -1);
                //System.out.println("above: " + b);
                return b;
            }, 0, tileMax);
        } catch (Exception e) {
            e.printStackTrace();
        }



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
            } else {
                theMario = null;
            }

        });


        initButton();
        //initBipolar();


        Signal dvx = senseNumberDifference($$("vx(nario)"), () -> theMario!=null ? theMario.x : 0).resolution(0.02f);
        Signal dvy = senseNumberDifference($$("vy(nario)"), () -> theMario!=null ? theMario.y : 0).resolution(0.02f);


        rewardNormalized("goRight", -1, +1, () -> {

            float reward;
            float curX = theMario!=null ? theMario.x : Float.NaN;
            if (lastX == lastX && lastX < curX) {
                reward = //unitize(Math.max(0, (curX - lastX)) / 16f * MoveRight.floatValue());
                         1;
            } else {
                reward = -1;
            }
            lastX = curX;

            return reward;
        });
        rewardNormalized("getCoins", -1, +1, () -> {
            int coins = Mario.coins;
            int deltaCoin = coins - lastCoins;
            if (deltaCoin <= 0)
                return -1;
                //return Float.NaN;

            float reward = deltaCoin * EarnCoin.floatValue();
            lastCoins = coins;
            return Math.max(0,reward);
        });
        rewardNormalized("alive", -1, +1, () -> {
//            if (dead)
//                return -1;
//
            if (theMario ==null) {
                return 0f;
            }

            int t = theMario.deathTime > 0  ? -1 : +1;
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
    }



    int tile(int dx, int dy) {
        if (this.game.scene instanceof LevelScene) {
            LevelScene s = (LevelScene) game.scene;
            Level ll = s.level;
            if (ll != null) {
                //System.out.println(s.mario.x + " " + s.mario.y);
                byte block = ll.getBlock(Math.round((s.mario.x - 8) / 16f) + dx, Math.round((s.mario.y - 8) / 16f) + dy);
                byte t = Level.TILE_BEHAVIORS[block & 0xff];
                boolean breakable = ((t & BIT_BREAKABLE) != 0) || ((t & BIT_PICKUPABLE) != 0) || (t & BIT_BUMPABLE) != 0;
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

        for (GoalActionConcept c : actionPushButtonMutex(
                $$("left(nario)"),
                $$("right(nario)"),
                n -> game.scene.key(Mario.KEY_LEFT, n),
                n -> game.scene.key(Mario.KEY_RIGHT, n))) {
            c.actionDur(1);
        }

        GoalActionConcept j = actionPushButton($$("jump(nario)"),
                n -> {

                    Scene s = game.scene;
                    int jumpTime = s instanceof LevelScene ? ((LevelScene) s).mario.jumpTime : 0;
                    //System.out.println(jumpTime);
                    boolean jumping = jumpTime > 0;
                    boolean wasPressed = game.scene.key(Mario.KEY_JUMP);

                    boolean press;
                    if (!n) {
                        press = wasPressed || (!wasPressed && jumping);
                    } else {




//                        //System.out.println(jumping + " " + (s instanceof LevelScene ? ((LevelScene) s).mario.jumpTime : 0));
//                        if (wasPressed && !jumping) {
//                            press = false;
//                        } else {
//                            press = (!wasPressed) || jumping;
//                        }
                        if (!wasPressed || (wasPressed && jumping))
                            press = true;
                        else
                            press = false;
                    }
                    game.scene.key(Mario.KEY_JUMP, press);
                    return press;
                });


        actionToggle($$("down(nario)"),
                n -> game.scene.key(Mario.KEY_DOWN, n));
        actionToggle($$("speed(nario)"),
                n -> game.scene.key(Mario.KEY_SPEED, n));


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
            game.scene.key(Mario.KEY_LEFT, n);
            game.scene.key(Mario.KEY_RIGHT, p);
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
            game.scene.key(Mario.KEY_DOWN, n);

            game.scene.key(Mario.KEY_JUMP, p);
            return true;
        });


    }

    public void initBipolar() {
        float thresh = 0.25f;


        BiPolarAction X = actionBipolarFrequencyDifferential($.p(id, $.the("x")), false, (x) -> {

            float boostThresh = 0.75f;
            if (x <= -thresh) {
                game.scene.key(Mario.KEY_LEFT, true);
                game.scene.key(Mario.KEY_RIGHT, false);
                game.scene.key(Mario.KEY_SPEED, x <= -boostThresh);

                return x <= -boostThresh ? -1 : -boostThresh;
            } else if (x >= +thresh) {
                game.scene.key(Mario.KEY_RIGHT, true);
                game.scene.key(Mario.KEY_LEFT, false);
                game.scene.key(Mario.KEY_SPEED, x >= +boostThresh);

                return x >= +boostThresh ? +1 : +boostThresh;
            } else {
                game.scene.key(Mario.KEY_LEFT, false);
                game.scene.key(Mario.KEY_RIGHT, false);
                game.scene.key(Mario.KEY_SPEED, false);


                return 0f;

            }
        });
        BiPolarAction Y = actionBipolarFrequencyDifferential($.p(id, $.the("y")), false, (y) -> {

            if (y <= -thresh) {
                game.scene.key(Mario.KEY_DOWN, true);
                game.scene.key(Mario.KEY_JUMP, false);
                return -1f;

            } else if (y >= +thresh) {
                game.scene.key(Mario.KEY_JUMP, true);
                game.scene.key(Mario.KEY_DOWN, false);
                return +1f;

            } else {
                game.scene.key(Mario.KEY_JUMP, false);
                game.scene.key(Mario.KEY_DOWN, false);

                return 0f;

            }
        });/*.forEach(g -> {
            g.resolution(0.1f);
        });*/

        window(NARui.beliefCharts(nar, X.pos, X.neg, Y.pos, Y.neg), 700, 700);
    }

    int lastCoins;

    public final FloatRange MoveRight = new FloatRange(0.75f, 0f, 1f);
    public final FloatRange EarnCoin = new FloatRange(0.95f, 0f, 1f);

    float lastX;


    public static void main(String[] args) {


        runRT((NAR n) -> {


            NARio x;
            x = new NARio(n);
            n.freqResolution.set(0.02f);
            n.confResolution.set(0.01f);

            return x;


        }, fps);


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