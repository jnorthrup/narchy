package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.gl2.GLUT;
import jcog.math.v3;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.widget.CuboidSurfaceGraph;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import static spacegraph.space2d.container.grid.Gridding.grid;

/** embedded 3d viewport for use on a 2d surface */
public class View3Din2D extends PaintSurface {

    private final SpaceDisplayGraph3D space;

    public View3Din2D(SpaceDisplayGraph3D space) {
        this.space = space;
    }
    @Override
    protected void paint(GL2 gl, ReSurface reSurface) {
        gl.glPushMatrix();
        int w = 100, h = 100; //HACK
        //gl.glViewport(0,0, w, h);
        space.init(gl);
        space.camera(new v3((float)(Math.random()*1.5f), (float)(Math.random()*0.5f), (float)(Math.random()*0.5f)), 1f);
        space.render(reSurface.dtS(), gl);
        gl.glColor3f(1,1,1);

        new GLUT().glutSolidCube(1);
//        Draw.circle(gl,new v2((float)(Math.random()*10), (float)(Math.random()*5f - 2.5f)),
//                true, 1, 5);

        //return to ortho
        int W = 1000, H = 1000; //HACK
        gl.glViewport(0, 0, W, H);
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glOrtho(0, W, 0, H, -1.5, 1.5);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        gl.glPopMatrix();
    }

    public static void main(String[] args) {
        SpaceGraph.window(grid(new View3Din2D(
                new SpaceDisplayGraph3D(new CuboidSurfaceGraph("x",
                        new BitmapLabel("y"),
                        10, 10)).camPos(0,0,-20)
        ), new PushButton("x")), 800, 800);
    }
}
