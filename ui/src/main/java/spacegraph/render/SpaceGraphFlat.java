package spacegraph.render;

import spacegraph.SpaceGraph;
import spacegraph.Surface;

public class SpaceGraphFlat extends SpaceGraph {

    public SpaceGraphFlat(Surface... o) {
        super();
        for (Surface s : o)
            add(s);
    }


//    @Override
//    public void init(GL2 gl) {
//        super.init(gl);
//        gl.glDisable(GL.GL_DEPTH_TEST);
//    }
}
