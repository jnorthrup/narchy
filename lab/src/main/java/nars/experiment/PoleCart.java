package nars.experiment;

import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.GameX;
import nars.NAR;
import nars.agent.Reward;
import nars.concept.action.BiPolarAction;
import nars.concept.sensor.DigitizedScalar;
import nars.term.Term;
import nars.term.atom.Atomic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.agent.GameTime.fps;

/**
 * adapted from:
 * http:
 * https:
 * <p>
 * see also: https:
 */
public class PoleCart extends GameX {


    private final DigitizedScalar xVel;
    private final DigitizedScalar x;
    private final AtomicBoolean drawFinished = new AtomicBoolean(true);


    static final float fps = 25;

    public static void main(String[] arg) {
        //polecart(-1);

        //int instances = 2; int threadsEach = 1;
        int instances = 1; int threadsEach = 4;
        for (int i = 0; i < instances; i++)
            runRTNet((n)->new PoleCart(
                    instances > 1 ?
                            $.p(Atomic.the(PoleCart.class.getSimpleName()), n.self()) :
                            $.the(PoleCart.class.getSimpleName()), n),
                    threadsEach, fps, fps, 8);
    }





    public static class RL {
        public static void main(String[] args) {
            runRL(n -> {

                try {
                    PoleCart p = new PoleCart($.the("rl"), n);

                    p.tau.set(0.004f);

                    return p;
                } catch (Exception e) {

                    e.printStackTrace();
                    return null;
                }
            }, fps, fps);

        }
    }

    private final JPanel panel;


    Dimension offDimension;
    Image offImage;
    Graphics offGraphics;


    double pos, posDot, angle, angleDot;

    final float posMin = -2f, posMax = +2f;
    float velMax = 10;
    boolean manualOverride;


    static final double cartMass = 1.;
    static final double poleMass = 0.1;
    static final double poleLength = 1f;
    static final double gravity = 9.8;
    static final double forceMag =
            //100.;
            200;
    public final FloatRange tau = new FloatRange(0.005f, 0.001f, 0.02f);
    //0.01;
    //0.005;
    //0.0025f;
    static final double fricCart = 0.00005;
    static final double fricPole =
            0.01f;
    static final double totalMass = cartMass + poleMass;
    static final double halfPole = 0.5 * poleLength;
    static final double poleMassLength = halfPole * poleMass;
    static final double fourthirds = 4. / 3.;


    DigitizedScalar angX;
    DigitizedScalar angY;

    DigitizedScalar angVel;


    double action;

    public PoleCart(Term id, NAR nar) {

        super(id, fps(fps), nar);


        pos = 0.;
        posDot = 0.;
        angle = 0.2;
        angleDot = 0.;
        action = 0;


        /**
         returnObs.doubleArray[0] = theState.getX();
         returnObs.doubleArray[1] = theState.getXDot();
         returnObs.doubleArray[2] = theState.getTheta();
         returnObs.doubleArray[3] = theState.getThetaDot();
         */


        this.x = senseNumberBi($.inh("x", id),
                new FloatNormalized(() -> (float) pos, posMin, posMax));
        this.xVel = senseNumberBi($.inh("dx", id),
                new FloatNormalized(() -> (float) posDot)
        );


        FloatSupplier angXval = () -> (float) (0.5f + 0.5f * (Math.sin(angle)));
        FloatSupplier angYval = () -> (float) (0.5f + 0.5f * (Math.cos(angle)));

//
//        angX = senseNumber(a->$.inst(Int.the(a),$.the("angX")), angXval, 2, DigitizedScalar.FuzzyNeedle);
//
//
//
//        angY = senseNumber(a->$.inst(Int.the(a), $.the("angY")), angYval, 2, DigitizedScalar.FuzzyNeedle);
//
//        angX.resolution(0.02f);
//        angY.resolution(0.02f);
        this.angX = senseNumberBi($.inh("angX",id),
                () -> (float) (0.5f + 0.5f * (Math.sin(angle))));
        this.angY = senseNumberBi($.inh("angY",id),
                () -> (float) (0.5f + 0.5f * (Math.cos(angle))));


        this.angVel = senseNumberBi($.inh("angVel",id),

                new FloatNormalized(() -> (float) angleDot)
        );


        //initBipolar();
        initUnipolar();


//        SpaceGraph.window(NARui.beliefCharts(512,
//                sensors,
//                nar), 900, 900);


        this.panel = new JPanel(new BorderLayout()) {
            public final Stroke stroke = new BasicStroke(4);

            @Override
            public void paint(Graphics g) {
                update(g);
            }

            @Override
            public void update(Graphics g) {
                Dimension d = panel.getSize();
                Color cartColor = Color.ORANGE;
                Color trackColor = Color.GRAY;


                if ((offGraphics == null)
                        || (d.width != offDimension.width)
                        || (d.height != offDimension.height)) {
                    offDimension = d;
                    offImage = panel.createImage(d.width, d.height);
                    offGraphics = offImage.getGraphics();
                }


                float clearRate = 0.5f;
                offGraphics.setColor(new Color(0, 0, 0, clearRate));
                offGraphics.fillRect(0, 0, d.width, d.height);


                double[] xs = {-2.5, 2.5, 2.5, 2.3, 2.3, -2.3, -2.3, -2.5};
                double[] ys = {-0.4, -0.4, 0., 0., -0.2, -0.2, 0, 0};
                int[] pixxs = new int[8];
                int[] pixys = new int[8];
                for (int i = 0; i < 8; i++) {
                    pixxs[i] = pixX(d, xs[i]);
                    pixys[i] = pixY(d, ys[i]);
                }
                offGraphics.setColor(trackColor);
                offGraphics.fillPolygon(pixxs, pixys, 8);


//                String msg = "Position = " + n2(pos) + " Angle = " + n2(angle) + " angleDot = " + n2(angleDot);
//                offGraphics.drawString(msg, 20, d.height - 20);


                offGraphics.setColor(cartColor);
                offGraphics.fillRect(pixX(d, pos - 0.2), pixY(d, 0), pixDX(d, 0.4), pixDY(d, -0.2));

                ((Graphics2D) offGraphics).setStroke(stroke);


                offGraphics.drawLine(pixX(d, pos), pixY(d, 0),
                        pixX(d, pos + Math.sin(angle) * poleLength),
                        pixY(d, poleLength * Math.cos(angle)));


                if (action != 0) {
                    int signAction = (action > 0 ? 1 : (action < 0) ? -1 : 0);
                    int tipx = pixX(d, pos + 0.2 *
                            //signAction
                            action
                    );
                    Color arrowColor = new Color(0.25f, 0.5f + Util.tanhFast(Math.abs((float)action))/2f, 0.25f);
                    int tipy = pixY(d, -0.1);
                    offGraphics.setColor(arrowColor);
                    offGraphics.drawLine(pixX(d, pos), pixY(d, -0.1), tipx, tipy);
                    offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy + 4);
                    offGraphics.drawLine(tipx, tipy, tipx - 4 * signAction, tipy - 4);
                }


                g.drawImage(offImage, 0, 0, panel);

                drawFinished.set(true);
            }

        };


        JFrame f = new JFrame();
        f.setContentPane(panel);
        f.setSize(800, 300);
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

                }
            }
        });


        Reward r = rewardNormalized("balanced", -1, +1, this::update);

        //new RewardBooster(r);

//        new BeliefPredict(
//                java.util.List.of(
//                        x, xVel,
//                        angVel, angX, angY),
//                8,
//                4 * nar.dur(),
//                4,
//                new LivePredictor.LSTMPredictor(0.1f, 1),
//                //new LivePredictor.MLPPredictor(0.1f),
//                nar
//        );
//        window(NARui.beliefCharts(nar, x, xVel, angVel, angX, angY), 700, 700);

    }

    public void initBipolar() {
        final float SPEED = 1f;
        BiPolarAction F = actionBipolarFrequencyDifferential(id, false, (x) -> {
            float a =
                    x * SPEED;
                    //(x * x * x) * SPEED;
            this.action = a;
            return x;
        });
    }

    public void initUnipolar() {
        actionUnipolar($.inh(("L"),id), (a) -> {
            if (!manualOverride) {
                synchronized (PoleCart.this) {
                    action = Util.clampBi((float) (action + a));
                    //action = Util.clampBi((float) (action + a * a));
                }
            }
            return a;
        });
        actionUnipolar($.inh(("R"),id), (a) -> {
            if (!manualOverride) {

                synchronized (PoleCart.this) {
                    action = Util.clampBi((float) (action - a));
                    //action = Util.clampBi((float) (action - a * a));
                }
            }
            return a;
        });
    }


    protected float update() {


        double force = forceMag * action;

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


        float tau = this.tau.floatValue();
        pos += posDot * tau;


        if ((pos >= posMax) || (pos <= posMin)) {

            pos = Util.clamp((float) pos, posMin, posMax);


            posDot = -1f /* restitution */ * posDot;


        }

        posDot += posDDot * tau;
        posDot = Math.min(+velMax, Math.max(-velMax, posDot));

        angle += angleDot * tau;
        angleDot += angleDDot * tau;

        /**TODO
         **/


        if (drawFinished.compareAndSet(true, false))
            SwingUtilities.invokeLater(panel::repaint);


        float rewardLinear = (float) (Math.cos(angle));
        return rewardLinear;
        //return rewardLinear * rewardLinear * rewardLinear;


    }


    int pixX(Dimension d, double v) {
        return (int) Math.round((v + 2.5) / 5.0 * d.width);
    }


    int pixDX(Dimension d, double v) {
        return (int) Math.round(v / 5.0 * d.width);
    }

    int pixY(Dimension d, double v) {
        return (int) Math.round(d.height - (v + 0.5f) / 2.0 * d.height);
    }

    public int pixDY(Dimension d, double v) {
        return (int) Math.round(-v / 2.0 * d.height);
    }

    void resetPole() {
        pos = 0.;
        posDot = 0.;
        angle = 0.;
        angleDot = 0.;
    }


}