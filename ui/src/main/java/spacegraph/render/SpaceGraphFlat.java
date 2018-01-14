package spacegraph.render;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;

public class SpaceGraphFlat extends SpaceGraph {

    public SpaceGraphFlat(Ortho o) {
        super();
        add(o);
    }

    @Override
    protected void initLighting() {

    }

//    @Override
//    public void init(GL2 gl) {
//        super.init(gl);
//        gl.glDisable(GL.GL_DEPTH_TEST);
//    }
}
