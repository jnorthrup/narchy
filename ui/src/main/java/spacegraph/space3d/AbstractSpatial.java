package spacegraph.space3d;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.space3d.phys.Collidable;
import spacegraph.space3d.phys.constraint.TypedConstraint;
import spacegraph.video.Draw;

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
        
    }

    @Override
    public void renderRelative(GL2 gl, Collidable body, int dtMS) {

        colorshape(gl);
        Draw.draw(gl, body.shape());































    }





    void colorshape(GL2 gl) {
        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
    }


}
