package spacegraph;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamics3D;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglWindow;

import java.util.List;
import java.util.function.Consumer;

/**
 * volumetric subspace.
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public abstract class Spatial<X> implements Active {

    public final X id;
    public final int hash;
    public boolean preactive;


    /**
     * the draw order if being drawn
     * order = -1: inactive
     * order > =0: live
     */
    public short order;



    protected Spatial() {
        this(null);
    }

    protected Spatial(X k) {
        this.id = k!=null ? k : (X) this;
        this.hash = k!=null ? k.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {

        return id + "<" +
                //(body!=null ? body.shape() : "shapeless")  +
                ">";
    }




    @Override
    public final boolean equals(Object obj) {

        return this == obj || id.equals(((Spatial) obj).id);
    }

    @Override
    public final int hashCode() {
        return hash;
    }





    public void update(Dynamics3D world) {
        //create and update any bodies and constraints
    }

//    public final boolean isShown(short nextOrder, Dynamics world) {
//        if (active()) {
//            return true;
//        } else {
//            return false;
//        }
//    }

    @Override
    public boolean active() {
        return order >= 0 || preactive;
    }

    @Override
    public void preActivate(boolean b) {

        this.preactive = b;

    }


    public boolean collidable() {
        return true;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public Surface onTouch(Finger finger, Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        return null;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        return false;
    }



    //abstract public Iterable<Collidable> bodies();
    abstract public void forEachBody(Consumer<Collidable> c);

    @Nullable abstract public List<TypedConstraint> constraints();

    public abstract void renderAbsolute(GL2 gl, int dtMS);

    public abstract void renderRelative(GL2 gl, Collidable body, int dtMS);

    public void delete(Dynamics3D dyn) {
        order = -1;
        preactive = false;
    }

    public void stabilize(v3 boundsMin, v3 boundsMax) {
        //v3 zero = v(0,0,0);

        forEachBody(b -> {
            v3 t = b.transform;
            //((Dynamic)b).clearForces();
            //((Dynamic)b).setLinearVelocity(zero);
            t.clamp(boundsMin, boundsMax);
        });
    }


    abstract public float radius();

    public void onUntouch(JoglWindow space) {

    }
}
