package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.Path2D;

public class SketchedPath extends Surface {

    private final Path2D path;

    public SketchedPath(Path2D path) {
        this.path = path;
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        if (path.points() > 1) {
            gl.glLineWidth(8);
            gl.glColor4f(0.5f, 0.5f, 1f, 0.8f);
            gl.glBegin(GL.GL_LINE_STRIP);
            path.vertex2f(gl);
            gl.glEnd();
        }
    }

}
