package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;

public class EmptySurface extends Surface {

    public EmptySurface() {
        visible = false;
    }


    @Override
    public Surface visible(boolean b) {
        return this; 
    }


    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {

    }

}
