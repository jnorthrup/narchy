package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;

abstract public class SimpleSurface extends Surface {

    @Override
    protected void render(ReSurface r) {
        r.on(this::paint);
    }

    abstract protected void paint(GL2 gl, ReSurface reSurface);

}
