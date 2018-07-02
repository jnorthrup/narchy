package nars.experiment;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.signal.Signal;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.MarioComponent;
import nars.experiment.mario.Scene;
import nars.experiment.mario.sprites.Mario;
import nars.sensor.Bitmap2DSensor;
import nars.util.TimeAware;
import nars.video.PixelBag;

import javax.swing.*;

import static jcog.Util.unitize;
import static nars.$.$$;

public class NARio extends NAgentX {

    private final MarioComponent mario;

    public NARio(NAR nar) {
        super("nario", nar);
        

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
        cc.actions.forEach(a -> a.resolution.set(0.25f));
        

        

        Bitmap2DSensor ccb;
        addCamera(ccb = new Bitmap2DSensor(id, cc, this.nar)).resolution(0.03f);









        








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
        


        Signal dvx = senseNumberDifference($$("(v,x)"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).
                mario.x : 0).resolution(0.02f);
        Signal dvy = senseNumberDifference($$("(v,y)"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).
                mario.y : 0).resolution(0.02f);

















    }

    private void initButton() {

        actionToggle($$("left"),
                n -> mario.scene.key(Mario.KEY_LEFT, n));
        actionToggle($$("right"),
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

    public final FloatRange MoveRight = new FloatRange(0.25f, 0f, 1f);
    public final FloatRange EarnCoin = new FloatRange(0.95f, 0f, 1f);

    float lastX;

    @Override
    protected float act() {
        int coins = Mario.coins;
        float reward = (coins - lastCoins) * EarnCoin.floatValue();
        lastCoins = coins;



        float curX = mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.x : Float.NaN;
        if (lastX == lastX && lastX < curX) {
            reward += unitize(Math.max(0, (curX - lastX))/16f * MoveRight.floatValue());
        }
        lastX = curX;


        float r = Util.clamp(reward, -1, +1);


        return r;
    }

    public static void main(String[] args) {


        

        TimeAware timeAware = runRT((NAR n) -> {


            NARio x;
                x = new NARio(n);
                n.freqResolution.set(0.02f);
                n.confResolution.set(0.01f);
                

                
                
                x.trace = true;












                return x;


            




















        }, 24);












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