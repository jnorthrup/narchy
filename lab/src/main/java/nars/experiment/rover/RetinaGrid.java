package nars.experiment.rover;

import com.jogamp.opengl.GL2;
import jcog.math.v3;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.SphereShape;

import static jcog.math.v3.v;

/**
 * Created by me on 9/13/16.
 */
public class RetinaGrid extends SimpleSpatial {

    public final RetinaPixel[][] retinas;

    public RetinaGrid(String id, v3 src, v3 fwd, v3 left, v3 up, int w, int h, float rangeMax) {
        super(id);

        retinas = new RetinaPixel[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                RetinaPixel r = new RetinaPixel(this);

                r.localPosition = src;

                r.localDirection = v(fwd);
                r.localDirection.addScaled(left, 2f * (((float) x) / (w - 1) - 0.5f));
                r.localDirection.addScaled(up, 2f * (((float) y) / (h - 1) - 0.5f));

                r.rangeMax = rangeMax;

                retinas[x][y] = r;
            }
        }

    }

    @Override
    protected CollisionShape newShape() {
        return new SphereShape(0.5f);
    }

    @Override
    public void update(Dynamics3D world) {
        for (RetinaPixel[] rr : retinas)
            for (RetinaPixel r : rr)
                r.update(world);

        super.update(world);
    }

    @Override
    public void renderAbsolute(GL2 gl, int dtMS) {
        for (RetinaPixel[] rr : retinas)
            for (RetinaPixel r : rr)
                r.render(gl);

        super.renderAbsolute(gl, dtMS);
    }

}
