package spacegraph;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.phys.Collidable;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.render.Draw;

import java.util.Collections;
import java.util.List;

/**
 * Created by me on 9/13/16.
 */
public abstract class AbstractSpatial<X> extends Spatial<X> {

    protected AbstractSpatial(X x) {
        super(x);
    }

    @Nullable
    @Override
    public List<TypedConstraint> constraints() {
        return Collections.emptyList();
    }

    @Override
    public void renderAbsolute(GL2 gl, int dtMS) {
        //blank
    }

    @Override
    public void renderRelative(GL2 gl, Collidable body, int dtMS) {

        colorshape(gl);
        Draw.draw(gl, body.shape());

//        CollisionShape shape = body.shape();

//        if (shape instanceof SimpleBoxShape) {
//            //render surface on BoxShape face
//
//            SimpleBoxShape bshape = (SimpleBoxShape) shape;
//
//            gl.glPushMatrix();
//
//            //if (sx > sy) {
//            float ty = bshape.y();
//            float tx = ty / bshape.x();
//            //} else {
//            //  tx = sx;
//            //  ty = sx/sy;
//            //}
//
//            //gl.glTranslatef(-1/4f, -1/4f, 0f); //align TODO not quite right yet
//
//            if (tx!=1 || ty!=1) {
//                gl.glScalef(tx, ty, 1f);
//            }
//
//            renderRelativeAspect(gl);
//
//            if (tx!=1 || ty!=1) {
//                gl.glPopMatrix();
//            }
//        }

    }

//    protected void renderRelativeAspect(GL2 gl) {
//
//    }

    protected void colorshape(GL2 gl) {
        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
    }


}
