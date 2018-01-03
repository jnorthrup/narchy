package nars.experiment;

import jcog.Util;
import jcog.math.FloatParam;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.concept.SensorConcept;
import nars.experiment.mario.LevelScene;
import nars.experiment.mario.MarioComponent;
import nars.experiment.mario.Scene;
import nars.experiment.mario.sprites.Mario;
import nars.op.video.PixelBag;
import nars.util.signal.CameraSensor;

import javax.swing.*;

import static jcog.Util.unitize;
import static nars.$.$;
import static nars.$.p;

public class NARio extends NAgentX {

    private final MarioComponent mario;

//    private final SensorConcept vx;

    public NARio(NAR nar) throws Narsese.NarseseException {
        super("nario", nar);
        //super(nar, HaiQAgent::new);

        //Param.ANSWER_REPORTING = false;
        //Param.DEBUG = true;

        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        mario = new MarioComponent(
                //screenSize.width, screenSize.height
                640, 480
        );
        JFrame frame = new JFrame("Infinite NARio");
        frame.setIgnoreRepaint(true);

        frame.setContentPane(mario);
        //frame.setUndecorated(true);
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 0);

        //frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);

        frame.setVisible(true);

        mario.start();


        PixelBag cc = PixelBag.of(() -> mario.image, 36, 28);
        cc.addActions(id, this, false, false, true);
        cc.actions.forEach(a -> a.resolution = () -> (0.25f));
        //cc.setClarity(0.8f, 0.95f);
        CameraSensor<PixelBag> sc = addCamera(new CameraSensor<>(id, cc, this));

        //new ShapeSensor($.the("shape"), new BufferedImageBitmap2D(()->mario.image),this);


//        try {
//            csvPriority(nar, "/tmp/x.csv");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        onFrame((z) -> {
            //nar.onCycle(() -> {

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
                //cc.setZoom(0.4f);
            }
            //cc.setXRelative( mario.)
        });

        //sc.pri(0.1f);

//        CameraSensor ccAe = senseCameraReduced($.the("narioAE"), cc, 16)
//            .resolution(0.1f);
        //ccAe.pri(0.1f);


//        //new CameraGasNet($.the("camF"), cc, this, 64);
//        senseCameraRetina("narioGlobal", ()->mario.image, 16, 16, (v) -> t(v, alpha()));//.setResolution(0.1f);
//        sc.setResolution(0.1f);

//        nar.believe("nario:{narioLocal, narioGlobal}");


        initBipolar();
        //initToggle();


        SensorConcept dvx = senseNumberDifference($("vx"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).
                mario.x : 0).resolution(0.04f);
        SensorConcept dvy = senseNumberDifference($("vy"), () -> mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).
                mario.y : 0).resolution(0.04f);

//        window(Vis.beliefCharts(64, concat(dvx,dvy), nar), 300, 300);
//
//        new BeliefPredict(
//                Iterables.concat(actions.keySet(),  Iterables.concat(dvx, dvy)),
//                8,
//                12,
//                Iterables.concat(actions.keySet(),  dvx, dvy),
//                //new LivePredictor.LSTMPredictor(0.1f, 2),
//                new LivePredictor.MLPPredictor(),
//                nar
//        );



//        frame.addKeyListener(mario);
//        frame.addFocusListener(mario);
    }

    private void initToggle() {
        actionToggle($.$safe("nario:left"), (n) -> mario.scene.key(Mario.KEY_LEFT, n));
        actionToggle($.$safe("nario:right"), (n) -> mario.scene.key(Mario.KEY_RIGHT, n));
        actionToggle($.$safe("nario:jump"), (n) -> mario.scene.key(Mario.KEY_JUMP, n));
        actionToggle($.$safe("nario:down"), (n) -> mario.scene.key(Mario.KEY_DOWN, n));
        actionToggle($.$safe("nario:speed"), (b) -> mario.scene.key(Mario.KEY_SPEED, b));
//        actionTriState($("x"), i -> {
//            boolean n, p;
//            switch (i) {
//                case -1:
//                    p = false;
//                    n = true;
//                    break;
//                case +1:
//                    p = true;
//                    n = false;
//                    break;
//                case 0:
//                    p = false;
//                    n = false;
//                    break;
//                default:
//                    throw new RuntimeException();
//            }
//            mario.scene.key(Mario.KEY_LEFT, n);
//            mario.scene.key(Mario.KEY_RIGHT, p);
//            return true;
//        });
//        actionTriState($("y"), i -> {
//            boolean n, p;
//            switch (i) {
//                case -1:
//                    p = false;
//                    n = true;
//                    break;
//                case +1:
//                    p = true;
//                    n = false;
//                    break;
//                case 0:
//                    p = false;
//                    n = false;
//                    break;
//                default:
//                    throw new RuntimeException();
//            }
//            mario.scene.key(Mario.KEY_DOWN, n);
//            //mario.scene.key(Mario.KEY_UP, p);
//            mario.scene.key(Mario.KEY_JUMP, p);
//            return true;
//        });
//

    }

    public void initBipolar() {
        //actionBipolar($.inh($.the("x"), id), (x) -> {
        actionBipolar($.the("x"), (x) -> {
            float thresh = 0.2f;
            float boostThresh = 0.9f;
            if (x <= -thresh) {
                mario.scene.key(Mario.KEY_LEFT, true);
                mario.scene.key(Mario.KEY_RIGHT, false);
                mario.scene.key(Mario.KEY_SPEED, x <= -boostThresh);
                //return -1;
                return x <= -boostThresh ? -1 : -boostThresh;
            } else if (x >= +thresh) {
                mario.scene.key(Mario.KEY_RIGHT, true);
                mario.scene.key(Mario.KEY_LEFT, false);
                mario.scene.key(Mario.KEY_SPEED, x >= +boostThresh);
                //return +1;
                return x >= +boostThresh ? +1 : +boostThresh;
            } else {
                mario.scene.key(Mario.KEY_LEFT, false);
                mario.scene.key(Mario.KEY_RIGHT, false);
                mario.scene.key(Mario.KEY_SPEED, false);
                //return 0f;
                //return x;
                //return 0;
                return 0f;
                //return Float.NaN;
            }
        });
        //actionBipolar($.inh($.the("y"), id), (y) -> {
        actionBipolar($.the("y"), (y) -> {
            float thresh = 0.2f;
            if (y <= -thresh) {
                mario.scene.key(Mario.KEY_DOWN, true);
                mario.scene.key(Mario.KEY_JUMP, false);
                return -1f;
                //return y;
            } else if (y >= +thresh) {
                mario.scene.key(Mario.KEY_JUMP, true);
                mario.scene.key(Mario.KEY_DOWN, false);
                return +1f;
                //return y;
            } else {
                mario.scene.key(Mario.KEY_JUMP, false);
                mario.scene.key(Mario.KEY_DOWN, false);
                //return 0f;
                return 0f;
                //return Float.NaN;
            }
        });/*.forEach(g -> {
            g.resolution(0.1f);
        });*/
    }

    int lastCoins;

    public final FloatParam Depress = new FloatParam(0f, 0f, 1f);
    public final FloatParam MoveRight = new FloatParam(0.25f, 0f, 1f);
    public final FloatParam EarnCoin = new FloatParam(0.95f, 0f, 1f);

    float lastX;

    @Override
    protected float act() {
        int coins = Mario.coins;
        float reward = (coins - lastCoins) * EarnCoin.floatValue();
        lastCoins = coins;

//        float vx = this.vx.asFloat();

        float curX = mario.scene instanceof LevelScene ? ((LevelScene) mario.scene).mario.x : Float.NaN;
        if (lastX == lastX && lastX < curX) {
            reward += unitize(Math.max(0, (curX - lastX))/16f * MoveRight.floatValue());
        }
        lastX = curX;

        reward -= Depress.floatValue();

        float r = Util.clamp(reward, -1, +1);
//        if (r == 0)
//            return Float.NaN;
        return r;// + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {


        //Param.DEBUG = true;

        NAR nar = runRT((NAR n) -> {


            NARio x;
            try {
                x = new NARio(n);
                n.freqResolution.set(0.05f);
                n.confResolution.set(0.02f);

                //new Implier(x, 1);
                //x.durations.setValue(2f);
                x.trace = true;

                n.onTask(t -> {
                    if (t.isEternal() && !t.isInput() && !t.isQuestOrQuestion()) {
                        System.err.println(t.proof());
                    }
//                    if (t.isGoal() && !t.isInput()) {
//                        System.err.println(t.proof());
//                    }
//                    if (t.isGoal() && t.term().equals(x.happy.term())) {
//                        System.err.println(t.proof());
//                    }
                });
                return x;
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }


            //n.termVolumeMax.setValue(60);

//            try {
//                ImmutableTask r = (ImmutableTask) n.ask($.$("(?x ==> happy(nario))"), ETERNAL, (q, a) -> {
//                    System.err.println(a);
//                });
//                n.onCycle((nn) -> {
//                    r.budgetSafe(1f, 0.9f);
//                    nn.input(r);
//                });

//                n.onTask(tt -> {
//                   if (tt.isBelief() && tt.op() == IMPL)
//                       System.err.println("\t" + tt);
//                });

//            } catch (Narsese.NarseseException e) {
//                e.printStackTrace();
//            }


        }, 24);


//        ArrayList<PLink<Concept>> x = Lists.newArrayList(nar.conceptsActive());
//        x.sort((a,b)->{
//            int z = Float.compare(a.pri(), b.pri());
//            if (z == 0)
//                return Integer.compare(a.get().hashCode(), b.get().hashCode());
//            return z;
//        });
//        for (PLink y : x)
//            System.out.println(y);

    }

}

/*
public class NARio {
    public static void main(String[] args)
    {
        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        MarioComponent mario = new MarioComponent(
                //screenSize.width, screenSize.height
                800, 600
        );
        JFrame frame = new JFrame("Infinite NARio");
        frame.setIgnoreRepaint(true);

        frame.setContentPane(mario);
        //frame.setUndecorated(true);
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 0);

        //frame.setLocation((screenSize.width-frame.getWidth())/2, (screenSize.height-frame.getHeight())/2);

        frame.setVisible(true);

        mario.start();
//        frame.addKeyListener(mario);
//        frame.addFocusListener(mario);
    }
}
 */