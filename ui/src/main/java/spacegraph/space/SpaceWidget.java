package spacegraph.space;

import com.jogamp.opengl.GL2;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.Quat4f;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglWindow;
import spacegraph.widget.button.PushButton;

import java.util.List;
import java.util.function.Consumer;

/** a spatial holding a Surface, implementing an interface between 3d and 2d UI dynamics */
abstract public class SpaceWidget<T> extends Cuboid<T> {

    boolean touched = false;

    public SpaceWidget(T x) {
        super(x, 1, 1);

        setFront(
            //col(
                //new Label(x.toString())
                new PushButton(x.toString()).click(this::onClicked)
                //row(new FloatSlider( 0, 0, 4 ), new PushButton("x"))
                        //, new BeliefTableChart(nar, x))
                    //new CheckBox("?")
            //)
                //new TermIcon(x)
        );

    }

    protected void onClicked(PushButton b) {
    }



    abstract public Iterable<? extends EDraw<?>> edges();


    @Override
    public void onUntouch(JoglWindow space) {
        touched = false;
    }

    @Override
    public Surface onTouch(Finger finger, Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(finger, body, hitPoint, buttons, space);
//        if (s != null) {
//        }

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

    public interface TermVis<X extends SpaceWidget> extends Consumer<List<X>> {

    }

    /** for simple element-wise functions */
    public interface SimpleNodeVis<X extends SpaceWidget<?>> extends Consumer<List<X>> {
        default void accept(List<X> l) {
            l.forEach(this::each);
        }

        void each(X e);
    }


}
