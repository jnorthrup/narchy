package nars.experiment;

import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.sensor.Signal;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.MarioComponent;
import nars.experiment.mario.Scene;
import nars.experiment.mario.level.Level;
import nars.experiment.mario.sprites.Mario;
import nars.sensor.Bitmap2DSensor;
import nars.video.PixelBag;

import javax.swing.*;

import static jcog.Util.unitize;
import static nars.$.$$;
import static nars.agent.FrameTrigger.fps;
import static nars.experiment.mario.level.Level.*;

public class NARio extends NAgentX {

    private final MarioComponent mario;
//    private final AbstractSensor cam;

    static final float fps = 24;

    public NARio(NAR nar) {
        super("nario", fps(fps), nar);


//        nar.freqResolution.set(0.1f);


        mario = new MarioComponent(

                640, 480
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


        PixelBag cc = PixelBag.of(() -> mario.image, 32, 24);
        cc.addActions(id, this, false, false, true);
        cc.actions.forEach(a -> a.resolution(0.5f));


        Bitmap2DSensor ccb;
        addCamera(ccb = new Bitmap2DSensor(id, cc, this.nar)).resolution(0.02f);
//
//        AutoConceptualizer ac;
//        addSensor(ac = new AutoConceptualizer(new FasterList(ccb.concepts), true, 8 , this.nar));
//        nar.runLater(()->{
//            SpaceGraph.window(            new BitmapMatrixView(ac.ae.W) {
//                {
//                    onFrame(()-> update());
//                }
//            }, 500, 500);
//        });


//        onFrame(()->cc.update());
//        int nx = 4;
//        AutoclassifiedBitmap camAE = new AutoclassifiedBitmap($.inh("cae", id), cc.pixels, nx, nx, (subX, subY) -> {
//            return new float[]{/*cc.X, cc.Y, */cc.Z};
//        }, 24, this);
//        camAE.alpha(0.05f);
//        SpaceGraph.window(camAE.newChart(), 500, 500);//

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


            Scene scene1 = mario.scene;

            if (scene1 instanceof LevelScene) {
                LevelScene scene = (LevelScene) scene1;
                float xCam = scene.xCam;
                float yCam = scene.yCam;
                Mario M = ((LevelScene) this.mario.scene).mario;
                float x = (M.x - xCam) / 320f;
                float y = (M.y - yCam) / 240f;
                cc.setXRelative(x);
                cc.setYRelative(y);

            }

        });


        initButton();


        Signal dvx = senseNumberDifference($$("vx(nario)"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).
                mario.x : 0).resolution(0.02f);
        Signal dvy = senseNumberDifference($$("vy(nario)"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).
                mario.y : 0).resolution(0.02f);


        reward("goRight", () -> {
            float reward = 0;

            float curX = mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.x : Float.NaN;
            if (lastX == lastX && lastX < curX) {
                reward += unitize(Math.max(0, (curX - lastX)) / 16f * MoveRight.floatValue());
            }
            lastX = curX;

            return reward;
        });
        reward("getCoins", () -> {
            int coins = Mario.coins;
            float reward = (coins - lastCoins) * EarnCoin.floatValue();
            lastCoins = coins;
            return Math.max(0,reward);
        });
        reward("alive", () -> {
            int t = mario.scene instanceof LevelScene ? (((LevelScene) mario.scene).mario.deathTime > 0 ? -1 : +1) : 0;
            if (t == -1)
                System.out.println("Dead");
            return t;
        });
    }


    public int tile(int dx, int dy) {
        if (this.mario.scene instanceof LevelScene) {
            LevelScene s = (LevelScene) mario.scene;
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

        actionPushButtonMutex(
                $$("left(nario)"),
                $$("right(nario)"),
                n -> mario.scene.key(Mario.KEY_LEFT, n),
                n -> mario.scene.key(Mario.KEY_RIGHT, n));

        actionToggle($$("jump"),
                n -> mario.scene.key(Mario.KEY_JUMP, n));
        actionToggle($$("down"),
                n -> mario.scene.key(Mario.KEY_DOWN, n));
        actionToggle($$("speed"),
                n -> mario.scene.key(Mario.KEY_SPEED, n));


    }

    public void initTriState() {
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
            mario.scene.key(Mario.KEY_LEFT, n);
            mario.scene.key(Mario.KEY_RIGHT, p);
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
            mario.scene.key(Mario.KEY_DOWN, n);

            mario.scene.key(Mario.KEY_JUMP, p);
            return true;
        });


    }

    public void initBipolar() {
        float thresh = 0.33f;


        actionBipolarFrequencyDifferential($.the("x"), false, true, (x) -> {

            float boostThresh = 0.66f;
            if (x <= -thresh) {
                mario.scene.key(Mario.KEY_LEFT, true);
                mario.scene.key(Mario.KEY_RIGHT, false);
                mario.scene.key(Mario.KEY_SPEED, x <= -boostThresh);

                return x <= -boostThresh ? -1 : -boostThresh;
            } else if (x >= +thresh) {
                mario.scene.key(Mario.KEY_RIGHT, true);
                mario.scene.key(Mario.KEY_LEFT, false);
                mario.scene.key(Mario.KEY_SPEED, x >= +boostThresh);

                return x >= +boostThresh ? +1 : +boostThresh;
            } else {
                mario.scene.key(Mario.KEY_LEFT, false);
                mario.scene.key(Mario.KEY_RIGHT, false);
                mario.scene.key(Mario.KEY_SPEED, false);


                return 0f;

            }
        });
        actionBipolarFrequencyDifferential($.the("y"), false, true, (y) -> {

            if (y <= -thresh) {
                mario.scene.key(Mario.KEY_DOWN, true);
                mario.scene.key(Mario.KEY_JUMP, false);
                return -1f;

            } else if (y >= +thresh) {
                mario.scene.key(Mario.KEY_JUMP, true);
                mario.scene.key(Mario.KEY_DOWN, false);
                return +1f;

            } else {
                mario.scene.key(Mario.KEY_JUMP, false);
                mario.scene.key(Mario.KEY_DOWN, false);

                return 0f;

            }
        });/*.forEach(g -> {
            g.resolution(0.1f);
        });*/
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