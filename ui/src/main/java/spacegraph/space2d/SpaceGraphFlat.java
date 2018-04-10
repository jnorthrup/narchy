package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import spacegraph.video.JoglSpace;

public class SpaceGraphFlat extends JoglSpace {

    public SpaceGraphFlat(Surface... o) {
        super();
        for (Surface s : o)
            add(s);
    }


    @Override
    protected void initDepth(GL2 gl) {
        //disabled
    }

}
