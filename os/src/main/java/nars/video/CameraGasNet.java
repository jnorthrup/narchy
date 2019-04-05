package nars.video;

import com.jogamp.opengl.GL2;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.math.FloatNormalized;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.util.Timed;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.SimpleSurface;
import spacegraph.video.Draw;

import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraGasNet<P extends Bitmap2D> implements Consumer<NAR> {

    private final Timed timed;

    private final P src;


    final NeuralGasNet net;

    public CameraGasNet(Atomic root, P src, Game agent, int blobs) {

        this.src = src;

        this.timed = agent.nar();

        this.net = new NeuralGasNet(3, (short)blobs) {

            @NotNull @Override public Centroid newCentroid(int i, int dims) {
                return new Centroid(i, dims);
            }

        };

        int width = src.width();
        int height = src.height();

        IntStream.range(0, blobs).forEach(i->{
            Term base = $.func("blob" , $.the(i), root);

            FloatSupplier v2 = () -> {
                Centroid node = net.node(i);
                if (node!=null)
                    return (float) node.getEntry(0);
                else
                    return Float.NaN;
            };
            agent.senseNumber($.inh(base, Atomic.the("x")), new FloatNormalized(v2, 0f, 1f));
            FloatSupplier v1 = () -> {
                Centroid node = net.node(i);
                if (node!=null)
                    return (float) node.getEntry(1);
                else
                    return Float.NaN;
            };
            agent.senseNumber($.inh(base, Atomic.the("y")), new FloatNormalized(v1, 0f, 1f));
            FloatSupplier v = () -> {
                Centroid node = net.node(i);
                if (node!=null)
                    return (float) node.getEntry(2);
                else
                    return Float.NaN;
            };
            agent.senseNumber($.inh(base, Atomic.the("c")), new FloatNormalized(v, 0f, 1f));

            
            
            
        });


        agent.onFrame(this);

        SpaceGraph.window(new SimpleSurface() {
            @Override
            protected void paint(GL2 gl, ReSurface reSurface) {
                int nodes = net.size();
                for (int i = 0; i < nodes; i++) {
                    Centroid n = net.node(i);
                    float e = (float) ((1f + n.localDistance()) * (1f + n.localError()));
                    float x = (float) n.getEntry(0);
                    float y = (float) n.getEntry(1);
                    float c = (float) n.getEntry(2);
                    float r = 0.1f / (1f + e);
                    gl.glColor4f(c, 0, (0.25f * (1f-c)), 0.75f );
                    Draw.rect(x, 1f - y, r, r, gl);
                }
            }
        }, 500, 500);
    }



    @Override
    public void accept(NAR n) {

        src.update();

        int width = src.width();
        int height = src.height();

        int pixels = width * height;

        net.alpha.set(0.005f);

        net.setLambdaPeriod(64);
        
        net.setWinnerUpdateRate(0.05f, 0.01f);


        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                float color = src.brightness(w, h);
                if (timed.random().nextFloat() - 0.05f <= color)
                
                    net.put(w/((float)width), h/((float)height), color );
            }
        }
    }

}
