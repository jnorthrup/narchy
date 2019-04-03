package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;

public final class EmptySurface extends Surface {

    public EmptySurface() {
        visible = false;
    }


    @Override
    public Surface visible(boolean b) {
        return this; 
    }

}
