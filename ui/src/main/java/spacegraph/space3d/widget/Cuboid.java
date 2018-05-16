package spacegraph.space3d.widget;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.Util;
import jcog.event.On;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
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
 * https://en.wikipedia.org/wiki/Cuboid
 * Serves as a mount for an attached (forward-facing) 2D surface (embeds a surface in 3D space)
 */
public class Cuboid<X> extends SimpleSpatial<X> implements SurfaceRoot {

    @Nullable
    public Surface front;
    static final float zOffset = 0.1f; //relative to scale

    @Nullable
    public Finger finger;
    private v3 mousePick;
    //private float padding;

    public Cuboid(X x, float w, float h) {
        this(x, null, w, h);
    }

    public Cuboid(Surface front, float w, float h) {
        this((X) front, front, w, h);
    }

    public Cuboid(X x, Surface front, float w, float h) {
        this(x, front, w, h, (Math.min(w, h) / 2f));
    }

    public Cuboid(Surface front, float w, float h, float d) {
        this((X) front, front, w, h, d);
    }

    public Cuboid(X x, Surface front, float w, float h, float d) {
        super(x);

        scale(w, h, d);


        setFront(front);

    }

    public void setFront(Surface front) {
        synchronized (this) {
            this.front = front;
            this.finger = null; //new Finger(this);
            if (front != null) {
                front.start(this);
            }
        }
    }

    @Override
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(body, hitPoint, charCode, pressed)) {

            return front != null && front.tryKey(null, charCode, pressed);
        }
        return true;
    }

    @Override
    public Surface onTouch(Finger finger, Collidable body, ClosestRay r, short[] buttons, SpaceGraphPhys3D space) {

        if (body != null) {

            //rotate to match camera's orientation (billboarding)
            Object d = body.data();
            if (d instanceof SimpleSpatial) {
                //SimpleSpatial sd = (SimpleSpatial)d;
                //Quat4f target = Quat4f.angle(-space.camFwd.x, -space.camFwd.y, -space.camFwd.z, 0);
                //Quat4f target = new Quat4f();

                //sd.rotate( -space.camFwd.x, -space.camFwd.y, -space.camFwd.z, 0, 0.2f);

//                com.jogamp.common.util.SyncedRingbuffer
//                Transform bt = body.worldTransform;
//                // TODO somehow use the object's local transformation ? sd.transform().getRotation(...);
//                target.setAngle(
//                        space.camFwd.x-bt.x,
//                        space.camFwd.y - bt.y,
//                        space.camFwd.z -bt.z,
//                        (float) Math.PI
//                );
//
//                target.normalize();
//


//                sd.rotate(target, 0.2f); //new Quat4f());
//                //System.out.println("  : " + sd.transform().getRotation(new Quat4f()));
            }
//
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

                if (Util.equals(localPoint.z, frontZ, zTolerance)) { //top surface only, ignore sides and back

                    this.mousePick = r.hitPointWorld;

                    this.finger = finger;
                    //System.out.println(localPoint + " " + thick);

                    finger.pos.set(
                        localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f
                    );
                    Surface f = this.finger.on(front);
                    finger.update(buttons);
                    return f;
                    //return mouseFront.update(null, localPoint.x, localPoint.y, buttons);
                }
            } else {

                if (this.finger != null) {
                    this.finger.off(this);
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

            //float p = this.padding;

            //gl.glPushMatrix();

            //float pp = 1f - (p / 2f);
            //float pp = 1f;

            gl.glTranslatef(-0.5f, -0.5f, 0.5f + (shape instanceof SphereShape ? 5 : 0)+zOffset);
            //gl.glScalef(pp, pp, 1f);

            //Transform t = transform();
            //float tw = t.x;
            //float th = t.y;
            //gl.glDepthMask(false);
            float pixelScale = 1;
            front.render(gl, pixelScale, pixelScale, dtMS);
            //gl.glDepthMask(true);

            //gl.glPopMatrix();
        }
    }

    @Override
    public void renderAbsolute(GL2 gl, int dtMS) {
        super.renderAbsolute(gl, dtMS);

        //display pick location (debugging)
        if (mousePick != null) {
            gl.glPushMatrix();
            gl.glTranslatef(mousePick.x, mousePick.y, mousePick.z);
            gl.glScalef(0.25f, 0.25f, 0.25f);
            gl.glColor4f(1f, 1f, 1f, 0.5f);
            gl.glRotated(Math.random() * 360.0, Math.random() - 0.5f, Math.random() - 0.5f, Math.random() - 0.5f);
            //gl.glDepthMask(false);
            Draw.rect(gl, -0.5f, -0.5f, 1, 1);
            //gl.glDepthMask(true);
            gl.glPopMatrix();
        }
    }


    @Override
    public void the(String key, @Nullable Object added, @Nullable Runnable onRemove) {
        //TODO ignored
    }

    @Override
    public Object the(String key) {
        //TODO ignored
        return null;
    }

    @Override
    public On onUpdate(Consumer<JoglWindow> c) {
        throw new TODO();
    }

    @Override
    public On animate(Animated c) {
        throw new TODO();
    }
}
