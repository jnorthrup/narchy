package spacegraph.space3d;

import com.jogamp.opengl.GL2;
import jcog.math.v3;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.collision.ClosestRay;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.util.Active;
import spacegraph.video.JoglWindow;

import java.util.List;
import java.util.function.Consumer;

/**
 * volumetric subspace.
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public abstract class Spatial<X> implements Active {

    public final X id;
    private final int hash;
    public boolean preactive;


    /**
     * the draw order if being drawn
     * order = -1: inactive
     * order > =0: live
     */
    @Deprecated public short order;

    protected Spatial() {
        this(null);
    }

    Spatial(X k) {
        this.id = k != null ? k : (X) this;
        this.hash = k != null ? k.hashCode() : super.hashCode();
    }


    @Override
    public String toString() {

        return id + "<" +

                '>';
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj; // || id.equals(((Spatial) obj).id);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    public void update(Dynamics3D world) {
    }


    @Override
    public boolean active() {
        return order >= 0 || preactive;
    }

    @Override
    public void preActivate(boolean b) {

        this.preactive = b;

    }


    boolean collidable() {
        return true;
    }

    /**
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    public Surface onTouch(Finger finger, Collidable body, ClosestRay hitPoint, short[] buttons, SpaceGraph3D space) {
        return null;
    }

    /**
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        return false;
    }


    public abstract void forEachBody(Consumer<Collidable> c);

    public abstract @Nullable List<TypedConstraint> constraints();

    public abstract void renderAbsolute(GL2 gl, int dtMS);

    public abstract void renderRelative(GL2 gl, Collidable body, float dtS);

    public void delete(Dynamics3D dyn) {
        order = -1;
        preactive = false;
    }

    public void stabilize(v3 boundsMin, v3 boundsMax) {


        forEachBody(b -> {
            v3 t = b.transform;


            t.clamp(boundsMin, boundsMax);
        });
    }


    public abstract float radius();

    public void onUntouch(JoglWindow space) {

    }
}
