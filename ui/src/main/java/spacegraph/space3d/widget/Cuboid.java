package spacegraph.space3d.widget;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.Util;
import jcog.event.Off;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space3d.SimpleSpatial;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.SimpleBoxShape;
import spacegraph.space3d.phys.shape.SphereShape;
import spacegraph.util.animate.Animated;
import spacegraph.util.math.v3;
import spacegraph.video.Draw;
import spacegraph.video.JoglWindow;

import java.util.function.Consumer;

import static spacegraph.util.math.v3.v;

/**
 * https:
 * Serves as a mount for an attached (forward-facing) 2D surface (embeds a surface in 3D space)
 */
public class Cuboid<X> extends SimpleSpatial<X> implements SurfaceRoot {

    @Nullable
    public volatile Surface front;
    private static final float zOffset = 0.05f;

    @Nullable
    private Finger finger;
    private v3 mousePick;


    Cuboid(X x, float w, float h) {
        this(x, null, w, h);
    }


    private Cuboid(X x, Surface front, float w, float h) {
        this(x, front, w, h, (Math.min(w, h) / 2f));
    }

    private Cuboid(X x, Surface front, float w, float h, float d) {
        super(x);

        scale(w, h, d);


        setFront(front);

    }

    void setFront(Surface front) {
        synchronized (this) {
            this.front = front;
            this.finger = null;
            if (front != null) {
                front.start(this);
            }
        }
    }

    @Override
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(body, hitPoint, charCode, pressed)) {

            return front instanceof KeyPressed && ((KeyPressed) front).key(null, charCode, pressed);
        }
        return true;
    }

    @Override
    public Surface onTouch(Finger finger, Collidable body, ClosestRay r, short[] buttons, SpaceGraphPhys3D space) {

        if (body != null) {


            Object d = body.data();
//            if (d instanceof SimpleSpatial) {
//
//
//            }

            Surface s0 = super.onTouch(finger, body, r, buttons, space);
            if (s0 != null)
                return s0;
        }


        if (front != null) {
            Transform it = Transform.t(transform).inverse();
            v3 localPoint = it.transform(v(r.hitPointWorld));

            if (body != null && body.shape() instanceof SimpleBoxShape) {
                SimpleBoxShape shape = (SimpleBoxShape) body.shape();
                float frontZ = shape.z() / 2;
                float zTolerance = frontZ / 4f;

                if (Util.equals(localPoint.z, frontZ, zTolerance)) {

                    this.mousePick = r.hitPointWorld;

                    this.finger = finger;


                    finger.posOrtho.set(
                            localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f
                    );
                    Surface f = this.finger.on(front);
                    finger.update(buttons);
                    return f;

                }
            } else {

                if (this.finger != null) {
                    this.finger.off(front);
                    this.finger = null;
                }
            }
        }


        return null;
    }


    @Override
    public final void renderRelative(GL2 gl, Collidable body, int dtMS) {
        super.renderRelative(gl, body, dtMS);


        if (front != null) {


            gl.glTranslatef(-0.5f, -0.5f, 0.5f + (shape instanceof SphereShape ? 5 : 0) + zOffset);


            gl.glDepthMask(false);

            float pixelScale = 1;
            front.render(gl, rendering.restart(1, 1, dtMS).set(pixelScale, pixelScale, pixelScale/2, pixelScale/2));

            gl.glDepthMask(true);

        }
    }

    private final SurfaceRender rendering = new SurfaceRender();

    @Override
    public void renderAbsolute(GL2 gl, int dtMS) {
        super.renderAbsolute(gl, dtMS);


        if (mousePick != null) {
            gl.glPushMatrix();
            gl.glTranslatef(mousePick.x, mousePick.y, mousePick.z);
            gl.glScalef(0.25f, 0.25f, 0.25f);
            gl.glColor4f(1f, 1f, 1f, 0.5f);
            gl.glRotated(Math.random() * 360.0, Math.random() - 0.5f, Math.random() - 0.5f, Math.random() - 0.5f);

            Draw.rect(-0.5f, -0.5f, 1, 1, gl);

            gl.glPopMatrix();
        }
    }


    @Override
    public void the(String key, @Nullable Object added, @Nullable Runnable onRemove) {

    }

    @Override
    public Object the(String key) {

        return null;
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
