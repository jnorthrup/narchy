package spacegraph.space3d.test;

import com.google.common.primitives.Doubles;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.exe.Loop;
import jcog.learn.gng.NeuralGasMap;
import jcog.math.FloatRange;
import jcog.math.StreamingNormalizer;
import jcog.net.attn.MeshMap;
import org.jctools.queues.MpscArrayQueue;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.video.Draw;

import java.util.List;
import java.util.Queue;

import static spacegraph.SpaceGraph.window;

/**
 * a tool for realtime multidimensional data visualization
 * input modes:
 * UDPeer net
 * -each data model has its own network
 * <p>
 * stdio CSV (TODO)
 * <p>
 * visualization models:
 * 3D gasnet pointcloud
 */
public class Dimensionaleyez extends SimpleSpatial {

    final static int THREE_D = 3;
    final static int IN = 5;
    final NeuralGasMap n = new NeuralGasMap(IN, 64, THREE_D);
    final FloatRange scale = new FloatRange(10, 0.5f, 300f);

    final Queue<double[]> queue = new MpscArrayQueue<>(1024);

    private final StreamingNormalizer s;

    public Dimensionaleyez(String id) {
        super(id);

        s = new StreamingNormalizer(IN);

        MeshMap<Integer, List<Float>> m = MeshMap.get(id, this::accept);

        new Loop() {

            @Override
            public boolean next() {
                double[] x;
                while ((x = queue.poll()) != null) {
                    float[] df = Util.toFloat(x); 
                    x = Util.toDouble(s.normalize(df, df)); 

                    


                    n.put(x);
                }


                n.update();
                return true;

            }
        }.setFPS(20f);
    }



    private void accept(Integer k, List v) {
        double[] da = Doubles.toArray(v);
        queue.add(da);
    }

    @Override
    public void renderRelative(GL2 gl, Collidable body, float dtS) {

        float s = scale.floatValue();
        n.forEachNode((NeuralGasMap.AECentroid n) -> {
            float[] d = n.center();
            if (d == null)
                return;

            float d0 = d[0];
            if (d0 != d0) {
                
                return;
            }
            float x = d0 * s;
            float y = d[1] * s;
            float z = d.length > 2 ? d[2] * s : 0;

            float last = ((float) n.getEntry(n.getDimension() - 1) + 1f) / 2f;

            gl.glPushMatrix();
            gl.glTranslatef(x, y, z);
            float p = 0.3f + (float) (0.7f / (1f + n.localDistance()));

            float sat = 0.5f;
            float hue = (n.id %10)/10f;
            float bri = 0.5f;
            float size = last;

            Draw.hsb(gl, hue, sat, p, bri);
            Draw.glut.glutSolidCube(1f * size);
            gl.glPopMatrix();
        });
    }

    public static void main(String[] s) {
        window(new Dimensionaleyez("d1"), 1000, 800);


    }
}
