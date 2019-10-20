package spacegraph.space3d.widget;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.event.Off;
import jcog.math.v3;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceGraph;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.shape.SphereShape;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglWindow;

import java.util.function.Consumer;

/**
 * adapter/mount for an attached (forward-facing) 2D surface (embeds a surface in 3D space)
 */
public class SurfacedCuboid<X> extends SimpleSpatial<X> implements SurfaceGraph {

    public volatile @Nullable Surface front;
    private static final float zOffset = 0.1f;

    private final ReSurface rendering = new ReSurface();

    float pixelScale = 512f;


    SurfacedCuboid(X x, float w, float h) {
        this(x, null, w, h);
    }


    public SurfacedCuboid(X x, Surface front, float w, float h) {
        this(x, front, w, h, (Math.min(w, h) / 2f));
    }

    private SurfacedCuboid(X x, Surface front, float w, float h, float d) {
        super(x);

        scale(w, h, d);

        if (front!=null) {
            front.resize(w, h);
            front(front);
        }
    }

    /** set the "front"-facing surface */
    public SurfacedCuboid front(Surface front) {
        //TODO non-synchronized
        synchronized (this) {
            @Nullable Surface prevFront = this.front;
            if (prevFront != front) {
                if (prevFront != null)
                    prevFront.stop();
                this.front = front;
                if (front != null) {
                    front.start(this);
                }
            }
        }
        return this;
    }

//    @Override
//    public SurfaceGraph root() {
//        throw new TODO();
//    }

    @Override
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(body, hitPoint, charCode, pressed)) {

            return front instanceof KeyPressed && ((KeyPressed) front).key(null, charCode, pressed);
        }
        return true;
    }


    @Override
    public final void renderRelative(GL2 gl, Collidable body, float dtS) {
        super.renderRelative(gl, body, dtS);


        if (front != null) {


            gl.glTranslatef(-0.5f, -0.5f, 0.5f + (float) (shape instanceof SphereShape ? 5 : 0) + zOffset);

            //gl.glScalef(1f/pixelScale, 1f/pixelScale, 1f/pixelScale);

            gl.glDepthMask(false);


            rendering.start(gl, pixelScale, pixelScale, dtS , 10.0F);
            rendering.psw = rendering.psh = pixelScale;
//            rendering.w = front.w();
//            rendering.h = front.h();
            front.renderIfVisible(rendering);

            gl.glDepthMask(true);

        }
    }


    @Override
    public Off onUpdate(Consumer<JoglWindow> c) {
        throw new TODO();
    }

    @Override
    public Off animate(Animated c) {
        throw new TODO();
    }
}
