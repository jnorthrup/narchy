package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.input.Wiring;
import spacegraph.render.Draw;

import javax.annotation.Nullable;

/** base class for a port implementation */
public class Port extends Widget implements Wiring.Wireable {

    protected Wiring wiringOut = null;
    protected Wiring wiringIn = null;

//            final FingerDragging dragInit = new FingerDragging(0) {
//
//                @Override
//                public void start(Finger f) {
//                    //root().debug(this, 1f, ()->"fingering " + this);
//                }
//
//                @Override
//                protected boolean drag(Finger f) {
//                    SurfaceRoot root = root();
//                    root.debug(this, 1f, ()->"drag " + this);
//                    if (f.tryFingering(this))
//                        return false;
//                    else
//                        return true;
//                }
//            };


    @Override
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {

        if (wiringOut !=null) {
            gl.glColor4f(0, 1, 0, 0.35f);
            Draw.rect(gl, bounds);
        }
        if (wiringIn!=null) {
            gl.glColor4f(0, 0, 1, 0.35f);
            Draw.rect(gl, bounds);
        }


    }

    @Override
    public Surface onTouch(Finger finger, short[] buttons) {

        if (finger!=null && buttons!=null) {
            if (finger.tryFingering(new Wiring(this)))
                return this;
        }

//                Surface c = super.onTouch(finger, buttons);
//                if (c != null)
//                    return c;

        return this;
    }

    @Override
    public void onWireIn(@Nullable Wiring w, boolean active) {
        this.wiringIn = active ? w : null;
    }

    @Override
    public void onWireOut(@Nullable Wiring w, boolean active) {
        this.wiringOut = active ? w : null;
    }
}
