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

    @Nullable
    public volatile Surface front;
    private static final float zOffset = 0.1f;

//    @Nullable
//    private Finger finger;
//    private v3 mousePick;


    SurfacedCuboid(X x, float w, float h) {
        this(x, null, w, h);
    }


    public SurfacedCuboid(X x, Surface front, float w, float h) {
        this(x, front, w, h, (Math.min(w, h) / 2f));
    }

    private SurfacedCuboid(X x, Surface front, float w, float h, float d) {
        super(x);


        scale(w, h, d);


        front(front);

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
                    front.resize(1, 1);
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

//    @Override
//    public Surface onTouch(Finger finger, Collidable body, ClosestRay r, short[] buttons, SpaceDisplayGraph3D space) {
//
//        if (body != null) {
//
//
////            Object d = body.data();
////            if (d instanceof SimpleSpatial) {
////
////
////            }
//
//            Surface s0 = super.onTouch(finger, body, r, buttons, space);
//            if (s0 != null)
//                return s0;
//        }
//
//
//        if (front != null) {
//            Transform it = Transform.t(transform).invert();
//            v3 localPoint = it.transform(v(r.hitPointWorld));
//
//            if (body != null && body.shape() instanceof SimpleBoxShape) {
//                SimpleBoxShape shape = (SimpleBoxShape) body.shape();
//                float frontZ = shape.z() / 2;
//                float zTolerance = frontZ / 4f;
//
//                if (Util.equals(localPoint.z, frontZ, zTolerance)) {
//
//                    this.mousePick = r.hitPointWorld;
//
//                    this.finger = finger;
//
//                    //TODO
//                    //front.posOrtho.set()
//                    //finger.posOrtho.set(
//                      //      localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f
//                    //);
//
//                    ((NewtMouseFinger)this.finger).finger((f)->front); //TODO check
//
//                    finger.updateButtons(buttons);
//
//                    return finger.touching();
//
//                }
//            } else {
//
//                if (this.finger != null) {
//                    this.finger.off(front);
//                    this.finger = null;
//                }
//            }
//        }
//
//
//        return null;
//    }


    @Override
    public final void renderRelative(GL2 gl, Collidable body, float dtS) {
        super.renderRelative(gl, body, dtS);


        if (front != null) {


            gl.glTranslatef(-0.5f, -0.5f, 0.5f + (shape instanceof SphereShape ? 5 : 0) + zOffset);

            gl.glDepthMask(false);

            float pixelScale = 1;
            rendering.start(gl, pixelScale,pixelScale, dtS , 1);
            front.renderIfVisible(rendering);

            gl.glDepthMask(true);

        }
    }

    private final ReSurface rendering = new ReSurface();

//    @Override
//    public void renderAbsolute(GL2 gl, int dtMS) {
//        super.renderAbsolute(gl, dtMS);
//
//
//        if (mousePick != null) {
//            gl.glPushMatrix();
//            gl.glTranslatef(mousePick.x, mousePick.y, mousePick.z);
//            gl.glScalef(0.25f, 0.25f, 0.25f);
//            gl.glColor4f(1f, 1f, 1f, 0.5f);
//            gl.glRotated(Math.random() * 360.0, Math.random() - 0.5f, Math.random() - 0.5f, Math.random() - 0.5f);
//
//            Draw.rect(-0.5f, -0.5f, 1, 1, gl);
//
//            gl.glPopMatrix();
//        }
//    }

    @Override
    public Off onUpdate(Consumer<JoglWindow> c) {
        throw new TODO();
    }

    @Override
    public Off animate(Animated c) {
        throw new TODO();
    }
}