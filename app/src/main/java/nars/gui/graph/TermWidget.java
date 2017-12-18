package nars.gui.graph;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import nars.term.Termed;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.math.Quat4f;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SphereShape;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglSpace;
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;
import spacegraph.widget.text.Label;

import java.util.List;
import java.util.function.Consumer;

abstract public class TermWidget<T extends Termed> extends Cuboid<T> {

    boolean touched = false;

    public TermWidget(T x) {
        super(x, 1, 1);

        setFront(
            //col(
                new Label(x.toString())
                //row(new FloatSlider( 0, 0, 4 ), new PushButton("x"))
                        //, new BeliefTableChart(nar, x))
                    //new CheckBox("?")
            //)
                //new TermIcon(x)
        );

    }

    @Override
    protected CollisionShape newShape() {
        return id.op().atomic ? new SphereShape() : super.newShape() /* cube */;
    }

    abstract public Iterable<? extends EDraw<?>> edges();


    @Override
    public void onUntouch(JoglSpace space) {
        touched = false;
    }

    @Override
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(body, hitPoint, buttons, space);
        if (s != null) {
        }

        touched = true;

//        if (buttons.length > 0 && buttons[0] == 1) {
//            window(Vis.reflect(id), 800, 600);
//        }

        return s;
    }


    static void render(GL2 gl, SimpleSpatial src, float twist, Iterable<? extends EDraw> ee) {


        Quat4f tmpQ = new Quat4f();
        ee.forEach(e -> {
            float width = e.width;
            float thresh = 0.1f;
            if (width <= thresh) {
                float aa = e.a * (width / thresh);
                if (aa < 1/256f)
                    return;
                gl.glColor4f(e.r, e.g, e.b, aa /* fade opacity */);
                Draw.renderLineEdge(gl, src, e.tgt(), width);
            } else {
                Draw.renderHalfTriEdge(gl, src, e, width, twist, tmpQ);
            }
        });
    }

    @Override
    public void renderAbsolute(GL2 gl, int dtMS) {
//        gl.glDepthMask(false); //disable writing to depth buffer
//        gl.glDisable(GL.GL_DEPTH_BUFFER_BIT);

        render(gl, this,
                //(float)Math.PI * 2 * (timeMS % 4000) / 4000f,
                0,
                edges());

//        gl.glDepthMask(true);
//        gl.glEnable(GL.GL_DEPTH_BUFFER_BIT);

        if (touched) {
            gl.glPushMatrix();
            gl.glTranslatef(x(), y(), z());
            float r = radius() * 2f;
            gl.glScalef(r, r, r);
            Draw.drawCoordSystem(gl);
            gl.glPopMatrix();
        }
    }

    public interface TermVis<X extends TermWidget> extends Consumer<List<X>> {

    }

    /** for simple element-wise functions */
    public interface BasicTermVis<X extends TermWidget> extends Consumer<List<X>> {
        default void accept(List<X> l) {
            l.forEach(this::each);
        }

        void each(X e);
    }


}
