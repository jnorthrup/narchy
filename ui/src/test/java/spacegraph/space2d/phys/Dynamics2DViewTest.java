package spacegraph.space2d.phys;

import com.jogamp.opengl.GL2;
import jcog.math.v2;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.dyn2d.jbox2d.BlobTest4;
import spacegraph.space2d.phys.dynamics.Dynamics2D;

import static spacegraph.SpaceGraph.window;

class Dynamics2DViewTest {

    public static void main(String[] args) {
        Dynamics2D w = new Dynamics2D();
        w.setGravity(new v2(0, -0.9f));
        w.setContinuousPhysics(true);
        w.setWarmStarting(true);
        w.setSubStepping(true);

        //new CarTest().accept(w);
        //new TheoJansenTest().accept(w);
        new BlobTest4().accept(w);

        window(new Dynamics2DView(w) {
            @Override
            protected void paint(GL2 gl, SurfaceRender surfaceRender) {
                w.step(0.04f, 4, 1);
                super.paint(gl, surfaceRender);
            }
        }, 800, 800);


    }
}