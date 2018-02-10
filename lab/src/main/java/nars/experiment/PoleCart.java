package nars.experiment;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.math.FloatPolarNormalized;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.NAgentX;
import nars.concept.ScalarConcepts;
import nars.concept.SensorConcept;
import nars.gui.ConceptSurface;
import nars.gui.Vis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.Texts.n2;
import static spacegraph.SpaceGraph.window;

/**
 * adapted from:
 *  http://www.cs.colostate.edu/~anderson/code/
 *  https://github.com/B00075594/CI_Lab2_CartAndPole/blob/master/src/pole.java
 *
 * see also: https://github.com/rihasrdsk/continuous-action-cartpole-java/blob/master/src/org/rlcommunity/environments/cartpole/CartPole.java
 */
public class PoleCart extends NAgentX {


    private final ScalarConcepts xVel;
    private final ScalarConcepts x;
    private final AtomicBoolean drawFinished = new AtomicBoolean(true);
    private final ScalarConcepts dAngVel;

    public static void main(String[] arg) {

        runRT((n) -> {

            try {
                NAgent a = new PoleCart(n);
                a.nar.freqResolution.set(0.05f);
                a.nar.confResolution.set(0.02f);


                window(new ConceptSurface(a.happy, n), 500, 500);


                //a.durations.setValue(1f);
                //n.goalConfidence(0.75f);
                return a;
            } catch (Exception e) {

                e.printStackTrace();
                return null;
            }
        }, 25);
    }

    private final JPanel panel;

    //next three are for double-buffering
    Dimension offDimension;
    Image offImage;
    Graphics offGraphics;

    // Vars to store current pole and cart position and previous positions
    double pos, posDot, angle, angleDot;

    float posMin = -2f, posMax = +2f;
    float velMax = 10;
    boolean manualOverride;

    // Constants used for physics
    static final double cartMass = 1.; //kg
    static final double poleMass = 0.1; //kg
    static final double poleLength = 1f; //m
    static final double gravity = 9.8; //meters/(sec*sec)
    static final double forceMag = 10.;
    static final double tau = 0.01;
    static final double fricCart = 0.00005;
    static final double fricPole = 0.005;
    static final double totalMass = cartMass + poleMass;
    static final double halfPole = 0.5 * poleLength;
    static final double poleMassLength = halfPole * poleMass;
    static final double fourthirds = 4. / 3.;


    // Define the Engine
    // Define InputVariable1 Theta(t) {angle with perpendicular}
    ScalarConcepts angX;
    ScalarConcepts angY;
    // Define InputVariable1 x(t) {angular velocity}
    ScalarConcepts angVel;
    // OutputVariable {force to be applied}

    // Define the RuleBlock
    double action;

    public PoleCart(NAR nar) {
        //super(nar, HaiQAgent::new);
        super("cart", nar);

//        this.inputVariable1 = senseNumber("(ang)",
//                () -> MathUtils.normalizeAngle(angle, 0)).resolution(0.1f);

        // Initialise pole state.
        pos = 0.;
        posDot = 0.;
        angle = 0.2; // Pole starts off at an angle
        angleDot = 0.;
        action = 0;

        //reward.resolution(0.05f);

        /**
         returnObs.doubleArray[0] = theState.getX();
         returnObs.doubleArray[1] = theState.getXDot();
         returnObs.doubleArray[2] = theState.getTheta();
         returnObs.doubleArray[3] = theState.getThetaDot();
         */
        //TODO extract 'senseAngle()' for NSense interface

        this.x = senseNumberBi($.inh("x", id),
                new FloatPolarNormalized(() -> (float) pos)).resolution(0.04f);
        this.xVel = senseNumberBi($.inh("dx", id),
                //() -> Util.sigmoid((float) posDot)
                new FloatPolarNormalized(() -> (float) posDot)
        ).resolution(0.04f);

        //angle

        this.angX = senseNumberBi($.inh("angX", id),
                () -> (float) (0.5f + 0.5f * (Math.sin(angle))))
                .resolution(0.04f);
        this.angY = senseNumberBi($.inh("angY", id),
                () -> (float) (0.5f + 0.5f * (Math.cos(angle))))
                .resolution(0.04f);

        //angular velocity
        this.angVel = senseNumberBi($.inh("angVel", id),
                //() -> Util.sigmoid(angleDot / 4f)
                new FloatPolarNormalized(() -> (float) angleDot)
        ).resolution(0.04f);

        this.dAngVel = senseNumberDifferenceBi($.inh("dAngVel", id),
                ()->(float) angleDot
        ).resolution(0.04f);

        {
            actionBipolar(id, (a) -> {
                if (!manualOverride)
                    action = a;
                return a*2;
            });
//            //eternal bias to stop
//            nar.goal(f[0].term, Tense.Eternal, 0f, 0.01f);
//            nar.goal(f[1].term, Tense.Eternal, 0f, 0.01f);
        }



//        actionUnipolar($.p("left"), (a) -> {
//            if (!manualOverride)
//                action = Util.clampBi((float) (action + a));
//            return a;
//        });
//        actionUnipolar($.p("right"), (a) -> {
//            if (!manualOverride)
//                action = Util.clampBi((float) (action - a));
//            return a;
//        });


//        new BeliefPredict(
//                new Termed[] { xVel, angVel, x},
//                8,
//                12,
//                new Termed[] { angX, angY, angVel, xVel },
//                //new LivePredictor.LSTMPredictor(0.1f, 2),
//                new LivePredictor.MLPPredictor(0.1f),
//                nar
//        );


        Iterable<SensorConcept> sensors = Iterables.concat(this.x, xVel,
                angX,
                angY,
                angVel, dAngVel);


//        AutoConceptualizer ae = new AutoConceptualizer(sensors, true, 8, nar) {
//            @Override
//            protected void onFeature(Term feature) {
//                System.out.println(feature);
//                //nar.que(feature, QUEST, nar.time()  + nar.dur());
//                nar.believe(IMPL.the(0, feature, $.varDep(1)), Tense.Present, 1f, 0.9f);
//            }
//        };
//        BitmapMatrixView bmp = new BitmapMatrixView(ae.ae.W.length, ae.ae.W[0].length, BitmapMatrixView.arrayRenderer(ae.ae.W));
//        DurService.on(nar, bmp::update);
//        SpaceGraph.window(bmp,400,400);


        window(Vis.beliefCharts(60,
                sensors,
                nar), 900, 900);

        //new RLBooster(this, HaiQAgent::new, 2);

        this.panel = new JPanel(new BorderLayout()) {
            public Stroke stroke = new BasicStroke(4);

            @Override
            public void paint(Graphics g) {
                update(g);
            }

            @Override
            public void update(Graphics g) {
                Dimension d = panel.getSize();
                Color cartColor = Color.ORANGE;
                Color arrowColor = Color.WHITE;
                Color trackColor = Color.GRAY;

                //Create the off-screen graphics context, if no good one exists.
                if ((offGraphics == null)
                        || (d.width != offDimension.width)
                        || (d.height != offDimension.height)) {
                    offDimension = d;
                    offImage = panel.createImage(d.width, d.height);
                    offGraphics = offImage.getGraphics();
                }

                //Erase the previous image.
                offGraphics.setColor(new Color(0, 0, 0, 0.25f));
                offGraphics.fillRect(0, 0, d.width, d.height);

                //Draw Track.
                double xs[] = {-2.5, 2.5, 2.5, 2.3, 2.3, -2.3, -2.3, -2.5};
                double ys[] = {-0.4, -0.4, 0., 0., -0.2, -0.2, 0, 0};
                int pixxs[] = new int[8], pixys[] = new int[8];
                for (int i = 0; i < 8; i++) {
                    pixxs[i] = pixX(d, xs[i]);
                    pixys[i] = pixY(d, ys[i]);
                }
                offGraphics.setColor(trackColor);
                offGraphics.fillPolygon(pixxs, pixys, 8);

                //Draw message
                // String msg = "Left Mouse Button: push left    Right Mouse Button: push right     Middle Button: PANIC";
                String msg = "Position = " + n2(pos) + " Angle = " + n2(angle) + " angleDot = " + n2(angleDot);
                offGraphics.drawString(msg, 20, d.height - 20);

                //Draw cart.
                offGraphics.setColor(cartColor);
                offGraphics.fillRect(pixX(d, pos - 0.2), pixY(d, 0), pixDX(d, 0.4), pixDY(d, -0.2));

                ((Graphics2D) offGraphics).setStroke(stroke);
                //Draw pole.
                //    offGraphics.setColor(cartColor);
                offGraphics.drawLine(pixX(d, pos), pixY(d, 0),
                        pixX(d, pos + Math.sin(angle) * poleLength),
                        pixY(d, poleLength * Math.cos(angle)));

                //Draw action arrow.
                if (action != 0) {
                    int signAction = (action > 0 ? 1 : (action < 0) ? -1 : 0);
                    int tipx = pixX(d, pos + 0.2 * signAction);
                    int tipy = pixY(d, -0.1);
                    offGraphics.setColor(arrowColor);
                    offGraphics.drawLine(pixX(d, pos), pixY(d, -0.1), tipx, tipy);
                    offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy + 4);
                    offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy - 4);
                }


                //Last thing: Paint the image onto the screen.
                g.drawImage(offImage, 0, 0, panel);

                drawFinished.set(true);
            }

        };

        // Handle keyboard events
        // LEFT KEY = push the cart left APPLY FORCE FROM RIGHT
        // RIGHT KEY = push the cart right APPLY FORCE FROM LEFT
        // SPACE KEY = Reset pole to centre

        JFrame f = new JFrame();
        f.setContentPane(panel);
        f.setSize(800, 600);
        f.setVisible(true);

        f.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == 'o') {
                    manualOverride = !manualOverride;
                    System.out.println("manualOverride=" + manualOverride);
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT)
                    action = -1;
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
                    action = 1;
                else if (e.getKeyChar() == ' ') {
                    action = 0;
                    //resetPole();
                }
            }
        });

        //start();
    }


    @Override
    protected float act() {
        //Update the state of the pole;
        // First calc derivatives of state variables
        double force = forceMag * action;
        //double force = action;
        double sinangle = Math.sin(angle);
        double cosangle = Math.cos(angle);
        double angleDotSq = angleDot * angleDot;
        double common = (force + poleMassLength * angleDotSq * sinangle
                - fricCart * (posDot < 0 ? -1 : 0)) / totalMass;

        double angleDDot = (gravity * sinangle - cosangle * common
                - fricPole * angleDot / poleMassLength) /
                (halfPole * (fourthirds - poleMass * cosangle * cosangle /
                        totalMass));
        double posDDot = common - poleMassLength * angleDDot * cosangle /
                totalMass;

        //Now update current state.
        pos += posDot * tau;


        if ((pos >= posMax) || (pos <= posMin)) {
            //bounce
            pos = Util.clamp((float) pos, posMin, posMax);

            //posDot = 0;
            //angleDDot = 0;

            posDot = -1f /* restitution */ * posDot;

            //posDot = -posDot;
            //angleDot = -angleDot;
            //angleDDot = -angleDDot;
        }

        posDot += posDDot * tau;
        posDot = Math.min(+velMax, Math.max(-velMax, posDot));

        angle += angleDot * tau;
        angleDot += angleDDot * tau;

        /**TODO
         // Above values represent current state of the cart and pole
         // Control system should take these values and make decision.
         // We are interested in angle and angleDot;
         // So we need a function here to set the state of the action for the next
         // update in time.
         **/


        //Display it.

        if (drawFinished.compareAndSet(true, false))
            SwingUtilities.invokeLater(panel::repaint);
//            //Delay depending on how far we are behind.
//            try {
//                startTime += delay;
//                Thread.sleep(Math.max(0,
//                        startTime - System.currentTimeMillis()));
//            } catch (InterruptedException e) {
//                break;
//            }

        //float rewardLinear = (float) (2f - Math.abs(MathUtils.normalizeAngle(angle, 0))) / 2f;

        float rewardLinear = (float) (Math.cos(angle));
        return rewardLinear;

        //float rewardCubed = (float) Math.pow(rewardLinear, 3);
        //return rewardCubed;


//        System.out.println(angle);
//        return (float) angleDot;
    }


    int pixX(Dimension d, double v) {
        return (int) Math.round((v + 2.5) / 5.0 * d.width);
    }

    int pixY(Dimension d, double v) {
        return (int) Math.round(d.height - (v + 2.5) / 5.0 * d.height);
    }

    int pixDX(Dimension d, double v) {
        return (int) Math.round(v / 5.0 * d.width);
    }

    public int pixDY(Dimension d, double v) {
        return (int) Math.round(-v / 5.0 * d.height);
    }

    void resetPole() {
        pos = 0.;
        posDot = 0.;
        angle = 0.;
        angleDot = 0.;
    }


}