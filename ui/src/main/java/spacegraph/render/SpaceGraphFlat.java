package spacegraph.render;

import spacegraph.Ortho;
import spacegraph.SpaceGraph;

public class SpaceGraphFlat extends SpaceGraph {

    public SpaceGraphFlat(Ortho o) {
        super();
        add(o);
    }


//    @Override
//    public void init(GL2 gl) {
//        super.init(gl);
//        gl.glDisable(GL.GL_DEPTH_TEST);
//    }
}
