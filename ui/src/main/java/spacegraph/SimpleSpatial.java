package spacegraph;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import jcog.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SimpleBoxShape;
import spacegraph.phys.shape.SphereShape;

import java.util.List;
import java.util.function.Consumer;

/** simplified implementation which manages one body and N constraints. useful for simple objects */
public class SimpleSpatial<X> extends AbstractSpatial<X> {


    public Dynamic body;

    @Nullable
    private final List<TypedConstraint> constraints = null; //new FasterList();

    /** physics motion state */
    //public final Motion motion = new Motion();
    //private final String label;
    public CollisionShape shape;

    /** prevents physics movement */
    public boolean motionLock;


    public final float[] shapeColor;
    public final Transform transform = new Transform();
    public boolean active;

    public SimpleSpatial(X x) {
        super(x);

        shapeColor = new float[] { 0.5f, 0.5f, 0.5f, 0.9f };
        this.shape = newShape();
        scale(1,1,1);

        //String label = label(x);

        //HACK
        //int MAX_LABEL_LEN = 16;
        //this.label = label.length() >= MAX_LABEL_LEN ? (label.substring(0, MAX_LABEL_LEN) + "..") : label;

    }

    @Override
    public void delete(Dynamics dyn) {
//        if (shape!=null) {
//            shape.setUserPointer(null);
//            shape = null;
//        }

        super.delete(dyn);

        synchronized(id) {
            if (body != null) {
                body.destroy(dyn);
                body = null;
            }
        }

    }

    @Override
    public void preActivate(boolean b) {
        active = b;
        super.preActivate(b);
    }

    @Override
    public boolean active() {
        return active && body!=null && super.active();
    }


    protected String label(X x) {
        return x!=null ? x.toString() : toString();
    }



    public SimpleSpatial color(float r, float g, float b) {
        return color(r, g, b, 1f);
    }

    public SimpleSpatial color(float r, float g, float b, float a) {
        shapeColor[0] = r;
        shapeColor[1] = g;
        shapeColor[2] = b;
        shapeColor[3] = a;
        return this;
    }

    public final Transform transform() {
        return transform;
    }

    public void moveX(float x, float rate) {
        v3 center = transform();
        move(Util.lerp(rate, center.x, x), center.y, center.z);
    }

    public void moveY(float y, float rate) {
        v3 center = transform();
        move(center.x, Util.lerp(rate, center.y, y), center.z);
    }


    public void moveZ(float z, float rate) {
        v3 center = transform();
        move(center.x, center.y, Util.lerp(rate, center.z, z));
    }

    public void move(v3 target, float rate) {
        move(target.x, target.y, target.z, rate);
    }

    public void move(float x, float y, float z, float rate) {
        v3 center = transform();
        move(
                Util.lerp(rate, center.x, x),
                Util.lerp(rate, center.y, y),
                Util.lerp(rate, center.z, z)
        );
    }


    public final void move(v3 p) {
        move(p.x, p.y, p.z);
    }

    public SimpleSpatial move(float x, float y, float z) {
        if (!motionLock) {
            transform().set(x, y, z);
            reactivate();
        }
        return this;
    }

//    /** interpolates rotation to the specified axis vector and rotation angle around it */
//    public void rotate(float nx, float ny, float nz, float angle, float speed) {
//        if (motionLock)
//            return;
//
//        Quat4f tmp = new Quat4f();
//
//
//        Quat4f target = new Quat4f();
//        target.setAngle(nx,ny,nz,angle);
//
//        rotate(target, speed, tmp);
//    }


    public void rotate(Quaternion target, float speed) {
        if (motionLock) return;
        rotate(target, speed, new Quaternion());
    }

    public void rotate(Quaternion target, float speed, Quaternion tmp) {
        if (motionLock)
            return;

        Quaternion current = transform.getRotation(tmp);
        current.setSlerp(current, target, speed);
        transform.setRotation(current);

        reactivate();
    }

    public void rotate(float tx, float ty, float tz, float angle, float speed) {
        Quaternion q = transform.getRotation(new Quaternion());
        q.rotateByAngleNormalAxis(angle, tx, ty, tz);
        rotate(q, speed);
    }


    public void reactivate() {
        if (body!=null)
            body.activate(collidable());
    }


    public void moveDelta(v3 v, float speed) {
        moveDelta(v.x, v.y, v.z, speed);
    }

    public void moveDelta(float dx, float dy, float dz, float speed) {
        move(
                x() + dx,
                y() + dy,
                z() + dz,
                speed);
    }
    public void moveDelta(float dx, float dy, float dz) {
        move(
                x() + dx,
                y() + dy,
                z() + dz);
    }

    public void scale(float sx, float sy, float sz) {


        //if (shape instanceof ConvexInternalShape) {
//            ((ConvexInternalShape)shape).
//                    localScaling.set(sx,sy,sz);
                    //implicitShapeDimensions.set(sx, sy, sz);

        if (shape instanceof SimpleBoxShape) {
            ((SimpleBoxShape)shape).setSize(sx, sy, sz);
        } else if (shape instanceof SphereShape) {
            ((SphereShape)shape).setRadius(Util.max(Math.abs(sx), Math.abs(sy), Math.abs(sz)));
        } else {
            throw new UnsupportedOperationException();
        }

//            ((ConvexInternalShape)shape).
//                    setLocalScaling(v(sx,sy,sz));
//

//        } else {
//            throw new UnsupportedOperationException();
//        }

//            ((SimpleBoxShape)shape).size(sx, sy, sz);
//        else
//        ((SphereShape)shape).implicitShapeDimensions
//        shape.setLocalScaling(v(sx,sy,sz));



        reactivate();
    }

    //TODO make abstract
    protected CollisionShape newShape() {
        return new SimpleBoxShape();
        //return new BoxShape(v3.v(1, 1, 1));
    }

    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic b = Dynamics.newBody(
                mass(), //mass
                shape, transform,//motion,
                +1, //group
                collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
        );

        //b.setLinearFactor(1,1,0); //restricts movement to a 2D plane


        //b.setDamping(0.95f, 0.95f);
        //b.setFriction(0.9f);


        return b;
    }

    public float mass() {
        if (body == null)
            return 1f;
        return body.mass();
    }

    @Override
    public void renderAbsolute(GL2 gl, long timeMS) {
        super.renderAbsolute(gl, timeMS);
    }

    @Override protected void colorshape(GL2 gl) {
        gl.glColor4fv(shapeColor, 0);
    }

//    protected void renderLabel(GL2 gl, float scale) {
//        gl.glColor4f(1f, 1f, 1f, 1f);
//        gl.glLineWidth(1f);
//        Draw.text(gl, marquee(), scale, 0, 0, 0.5f + 0.1f);
//    }
//
//    public String marquee() {
//        //TODO add animated scrolling marquee substring window
//        return label;
//    }

//    @Override
//    public void start(short order) {
//        super.start(order);
//        reactivate();
//    }



    public void motionLock(boolean b) {
        motionLock = b;
    }
    public float x() {  return transform().x;        }
    public float y() {  return transform().y;        }
    public float z() {  return transform().z;        }

//    protected void updateContinue() {
//        //if (body.broadphase()==null)
//        //reactivate();
//
//    }

    @Override
    public void update(Dynamics world) {
        if (body == null) {
            this.body = create(world);
        } else {
            reactivate(); //necessary?
        }
    }



    protected Dynamic create(Dynamics world) {
        Dynamic b = newBody(collidable());
        b.setData(this);
        return b;
    }

//
//    @Override
//    public boolean stop() {
//        if (super.stop()) {
//        }
//    }

    @Override
    public List<TypedConstraint> constraints() {
        return constraints;
    }


    @Override
    public float radius() {
        return shape.getBoundingRadius();
    }


    @Override
    public void forEachBody(Consumer<Collidable> c) {
        Dynamic b = this.body;
        if (b !=null)
            c.accept(b);
    }
}
