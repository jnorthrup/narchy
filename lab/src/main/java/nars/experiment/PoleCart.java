package nars.experiment;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.learn.LivePredictor;
import jcog.math.FloatPolarNormalized;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.agent.NAgent;
import nars.concept.Concept;
import nars.util.signal.BeliefPredict;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static jcog.Texts.n2;

/**
 * adapted from:
 * http:
 * https:
 * <p>
 * see also: https:
 */
public class PoleCart extends NAgentX {


    private final Concept xVel;
    private final Concept x;
    private final AtomicBoolean drawFinished = new AtomicBoolean(true);
    

    public static void main(String[] arg) {

        runRT((n) -> {

            try {
                NAgent a = new PoleCart(n);
                
                








                
                
                return a;
            } catch (Exception e) {

                e.printStackTrace();
                return null;
            }
        }, 60);
    }

    private final JPanel panel;

    
    Dimension offDimension;
    Image offImage;
    Graphics offGraphics;

    
    double pos, posDot, angle, angleDot;

    float posMin = -2f, posMax = +2f;
    float velMax = 10;
    boolean manualOverride;

    
    static final double cartMass = 1.; 
    static final double poleMass = 0.1; 
    static final double poleLength = 1f; 
    static final double gravity = 9.8; 
    static final double forceMag = 100.;
    static final double tau =
            0.01;
            //0.005;
            //0.0025f;
    static final double fricCart = 0.00005;
    static final double fricPole =
            
            0.01f;
    static final double totalMass = cartMass + poleMass;
    static final double halfPole = 0.5 * poleLength;
    static final double poleMassLength = halfPole * poleMass;
    static final double fourthirds = 4. / 3.;


    
    
    @NotNull Concept angX;
    @NotNull Concept angY;
    
    Concept angVel;
    

    
    double action;

    public PoleCart(NAR nar) {
        
        super("cart", nar);




        
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
        

        this.x = senseNumber($.the("x"),
                new FloatPolarNormalized(() -> (float) pos));
        this.xVel = senseNumber($.the("dx"),
                
                new FloatPolarNormalized(() -> (float) posDot)
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
        this.angX = senseNumber($.the("angX"),
                () -> (float) (0.5f + 0.5f * (Math.sin(angle))));
        this.angY = senseNumber($.the("angY"),
                () -> (float) (0.5f + 0.5f * (Math.cos(angle))));

        
        this.angVel = senseNumber($.the("angVel"),
                
                new FloatPolarNormalized(() -> (float) angleDot)
        );

























        actionUnipolar($.the("L"), (a) -> {
            if (!manualOverride)
                action = Util.clampBi((float) (action + a));
            return a;
        });
        actionUnipolar($.the("R"), (a) -> {
            if (!manualOverride)
                action = Util.clampBi((float) (action - a));
            return a;
        });


        new BeliefPredict(
                java.util.List.of(angX, angY, angVel, xVel ),
                8,
                1,
                4,
                new LivePredictor.LSTMPredictor(0.1f, 2),
                nar
        );


        Iterable<Concept> sensors = Iterables.concat(java.util.List.of(
                x, xVel,

                angVel
                
        ), angX, angY);















//        SpaceGraph.window(NARui.beliefCharts(512,
//                sensors,
//                nar), 900, 900);

        

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

                
                if ((offGraphics == null)
                        || (d.width != offDimension.width)
                        || (d.height != offDimension.height)) {
                    offDimension = d;
                    offImage = panel.createImage(d.width, d.height);
                    offGraphics = offImage.getGraphics();
                }

                
                offGraphics.setColor(new Color(0, 0, 0, 0.25f));
                offGraphics.fillRect(0, 0, d.width, d.height);

                
                double xs[] = {-2.5, 2.5, 2.5, 2.3, 2.3, -2.3, -2.3, -2.5};
                double ys[] = {-0.4, -0.4, 0., 0., -0.2, -0.2, 0, 0};
                int pixxs[] = new int[8], pixys[] = new int[8];
                for (int i = 0; i < 8; i++) {
                    pixxs[i] = pixX(d, xs[i]);
                    pixys[i] = pixY(d, ys[i]);
                }
                offGraphics.setColor(trackColor);
                offGraphics.fillPolygon(pixxs, pixys, 8);

                
                
                String msg = "Position = " + n2(pos) + " Angle = " + n2(angle) + " angleDot = " + n2(angleDot);
                offGraphics.drawString(msg, 20, d.height - 20);

                
                offGraphics.setColor(cartColor);
                offGraphics.fillRect(pixX(d, pos - 0.2), pixY(d, 0), pixDX(d, 0.4), pixDY(d, -0.2));

                ((Graphics2D) offGraphics).setStroke(stroke);
                
                
                offGraphics.drawLine(pixX(d, pos), pixY(d, 0),
                        pixX(d, pos + Math.sin(angle) * poleLength),
                        pixY(d, poleLength * Math.cos(angle)));

                
                if (action != 0) {
                    int signAction = (action > 0 ? 1 : (action < 0) ? -1 : 0);
                    int tipx = pixX(d, pos + 0.2 * signAction);
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
                    
                }
            }
        });

        
    }


    @Override
    protected float act() {
        
        
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