package nars.rl.example;

import automenta.vivisect.swing.NWindow;
import com.google.common.collect.Iterables;
import jurls.core.utils.MatrixImage;
import nars.Symbols;
import nars.Video;
import nars.concept.Concept;
import nars.rl.QEntry;
import nars.rl.QLAgent;
import nars.term.Term;
import org.apache.commons.math3.util.FastMath;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Created by me on 5/2/15.
 */
public class QVis extends JPanel implements Runnable {

    final java.util.List<Term> xstates = new ArrayList();
    final java.util.List<Operation> xactions = new ArrayList();
    private final QLAgent<Term> agent;

    public final MatrixImage mi = new MatrixImage(400,400) {


        @Override
        protected void pixel(final BufferedImage image, final int j, final int i, double value) {

            float pri = 0;
            float elig = 0;


            if (i >= xactions.size()) return;
            if (j >= xstates.size()) return;

            QEntry v = agent.ql.getEntry(xstates.get(j), xactions.get(i));
            int color;
            if (v != null) {
                Concept c = v.concept;
                float p = 0.4f + 0.6f * pri;


                float beliefValue = (float) v.getQSentence(Symbols.GOAL);
                float qValue = (float) v.getQ();

                float hue = (float)(FastMath.sin(qValue) + 1.0)/2f;


                float bri = 0.5f*p+0.5f;

                float sat = 0.5f + 0.5f * beliefValue;

                color = Video.colorHSB(hue, sat, bri);
                

            }
            else {
                color = Color.BLACK.getRGB();
            }


            image.setRGB(j, i, color);

            

        }

    };
    final MatrixImage ai = new MatrixImage(30,400, -1, 1);
    final MatrixImage aqi = new MatrixImage(30,400, -1, 1);
    private final NWindow nmi;

    MatrixImage.Data2D qData = new MatrixImage.Data2D() {

        @Override
        public double getValue(final int y, final int x) {
            return 0;
            
        }

    };
    MatrixImage.Data2D aData = new MatrixImage.Data2D() {

        @Override
        public double getValue(final int y, final int x) {
            return agent.getNARDesire(y);
        }

    };
    MatrixImage.Data2D qActData = new MatrixImage.Data2D() {

        @Override
        public double getValue(final int y, final int x) {
            double d = agent.getQDesire(y);

            return d;
        }

    };


    public QVis(QLAgent agent) {
        super(new BorderLayout());
        this.agent = agent;

        JPanel x = new JPanel(new FlowLayout());
        x.add(ai);
        x.add(aqi);
        add(x, BorderLayout.WEST);
        add(mi, BorderLayout.CENTER);


        nmi = new NWindow("Q", this).show(400, 400);
    }

    @Override
    public void run() {

        int ac = agent.getNumActions();

        mi.draw(qData, xstates.size(), xactions.size(), -1, 1);
        ai.draw(aData, 1, ac, 0, 1);
        aqi.draw(qActData, 1, ac, 0, 1);


        nmi.setTitle(xstates.size() + " states, " + xactions.size() + " actions");







    }

    public int val2col(final double n, final double min, final double max, final float bright) {
        final double mean = (max + min) / 2.0;
        final double n5 = min + 2.0 * (max - min) / 3.0;
        double r;
        if (n < mean) {
            r = (255.0 * (min - n) / (mean - min)) + 255;
        } else {
            r = 0;
        }
        if (r < 0) {
            r = 0;
        }
        if (r > 255) {
            r = 255;
        }
        double g;
        if (n < mean) {
            g = (255.0 * (n - min) / (mean - min));
        } else if (n < n5) {
            g = 255;
        } else {
            g = (255.0 * (n5 - n) / (max - n5)) + 255.0;
        }
        if (g < 0) {
            g = 0;
        }
        if (g > 255) {
            g = 255;
        }
        double b;
        if (n < mean) {
            b = 0;
        } else if (n < n5) {
            b = (255.0 * (n - mean) / (n5 - mean));
        } else {
            b = 255.0;
        }
        if (b < 0) {
            b = 0;
        }
        if (b > 255) {
            b = 255;
        }

        r = r * bright;
        g = g * bright;
        b = b * bright;

        int ir = (int)r;
        int ig = (int)g;
        int ib = (int)b;

        return 255 << 24 | ib << 16 | ig << 8 | ir;
    }


    public void frame() {

        
        xstates.clear();
        Iterables.addAll(xstates, agent.ql.rows);
        
        
        xactions.clear();
        Iterables.addAll(xactions, agent.ql.cols);
        


        SwingUtilities.invokeLater(this::run);
    }






























}
