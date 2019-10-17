package spacegraph.space3d.widget;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.util.math.Quat4f;
import spacegraph.video.Draw;

import java.util.List;
import java.util.function.Consumer;

/** a spatial holding a Surface, implementing an interface between 3d and 2d UI dynamics */
public abstract class SpaceWidget<T> extends SurfacedCuboid<T> {

    private boolean touched = false;

    public SpaceWidget(T x) {
        super(x, 1, 1);

        front(
            
                
                new PushButton(x.toString()).clicked(this::onClicked)
                
                        
                    
            
                
        );

    }

    protected void onClicked(PushButton b) {
    }



    public abstract Iterable<? extends EDraw<?>> edges();


//    @Override
//    public void onUntouch(JoglWindow space) {
//        touched = false;
//    }

//    @Override
//    public Surface onTouch(Finger finger, Collidable body, ClosestRay hitPoint, short[] buttons, SpaceDisplayGraph3D space) {
//        Surface s = super.onTouch(finger, body, hitPoint, buttons, space);
//
//
//
//        touched = true;
//
//
//
//
//
//        return s;
//    }


    private static void render(GL2 gl, SimpleSpatial src, float twist, Iterable<? extends EDraw> ee) {


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



        render(gl, this,
                
                0,
                edges());




//        if (touched) {
//            gl.glPushMatrix();
//            gl.glTranslatef(x(), y(), z());
//            float r = radius() * 2f;
//            gl.glScalef(r, r, r);
//            Draw.drawCoordSystem(gl);
//            gl.glPopMatrix();
//        }
    }

    @FunctionalInterface
    public interface TermVis<X extends SpaceWidget> extends Consumer<List<X>> {

    }

    /** for simple element-wise functions */
    @FunctionalInterface
    public interface SimpleNodeVis<X extends SpaceWidget<?>> extends Consumer<List<X>> {
        default void accept(List<X> l) {
            l.forEach(this::each);
        }

        void each(X e);
    }


}
