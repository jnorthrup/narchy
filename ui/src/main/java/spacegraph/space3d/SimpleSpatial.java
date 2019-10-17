package spacegraph.space3d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import jcog.Util;
import jcog.math.v3;
import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.phys.Body3D;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.Dynamics3D;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.shape.CollisionShape;
import spacegraph.space3d.phys.shape.SimpleBoxShape;
import spacegraph.space3d.phys.shape.SphereShape;

import java.util.List;
import java.util.function.Consumer;

/** simplified implementation which manages one body and N constraints. useful for simple objects */
public class SimpleSpatial<X> extends AbstractSpatial<X> {


    public Body3D body;

    private final @Nullable List<TypedConstraint> constraints = null;

    /** physics motion state */
    
    
    public CollisionShape shape;





    public final float[] shapeColor;
    public final Transform transform = new Transform().setIdentity();
    private boolean active;

    public SimpleSpatial(X x) {
        super(x);

        shapeColor = new float[] { 0.5f, 0.5f, 0.5f, 0.9f };
        this.shape = newShape();
        scale(1,1,1);

        

        

    }

    public SimpleSpatial() {
        this((X)Util.uuid64()); //HACK
    }

    @Override
    public void delete(Dynamics3D dyn) {





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



        transform.set(x, y, z);
        reactivate();
        return this;
    }
















    private void rotate(Quaternion target, float speed) {

        rotate(target, speed, new Quaternion());
    }

    public void rotate(Quaternion target, float speed, Quaternion tmp) {



        Quaternion current = transform.getRotation(tmp);
        current.setSlerp(current, target, speed);
        transform.setRotation(current);

        reactivate();
    }

    public SimpleSpatial rotate(float tx, float ty, float tz, float angle, float speed) {
        Quaternion q = transform.getRotation(new Quaternion());
        q.rotateByAngleNormalAxis(angle, tx, ty, tz);
        rotate(q, speed);
        return this;
    }


    private void reactivate() {
        if (body!=null)
            body.activate(collidable());
    }


    public void moveDelta(v3 v, float speed) {
        moveDelta(v.x, v.y, v.z, speed);
    }

    private void moveDelta(float dx, float dy, float dz, float speed) {
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

    public SimpleSpatial scale(float s) {
        return scale(s, s, s);
    }

    public SimpleSpatial scale(float sx, float sy, float sz) {


        if (shape instanceof SimpleBoxShape) {
            ((SimpleBoxShape)shape).setSize(sx, sy, sz);
        } else if (shape instanceof SphereShape) {
            shape.setLocalScaling(Math.abs(sx), Math.abs(sy), Math.abs(sz));
            
        } else {
            throw new UnsupportedOperationException();
        }
















        reactivate();
        return this;
    }

    
    protected CollisionShape newShape() {
        return new SimpleBoxShape();
        
    }

    public Body3D newBody(boolean collidesWithOthersLikeThis) {
        Body3D b = Dynamics3D.newBody(
                mass(), 
                shape, transform,
                +1, 
                collidesWithOthersLikeThis ? -1 : -1 & ~(+1) 
        );

        


        
        


        return b;
    }

    public float mass() {
        if (body == null)
            return 1f;
        return body.mass();
    }


    @Override protected void colorshape(GL2 gl) {
        gl.glColor4fv(shapeColor, 0);
    }























    public float x() {  return transform().x;        }
    public float y() {  return transform().y;        }
    protected float z() {  return transform().z;        }







    @Override
    public void update(Dynamics3D world) {
        if (body == null) {
            this.body = create(world);
        } else {
            reactivate(); 
        }
    }



    protected Body3D create(Dynamics3D world) {
        Body3D b = newBody(collidable());
        b.setData(this);
        return b;
    }








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
        Body3D b = this.body;
        if (b !=null)
            c.accept(b);
    }
}
