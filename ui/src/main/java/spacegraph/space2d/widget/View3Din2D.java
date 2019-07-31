package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.text.BitmapLabel;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.widget.CuboidSurfaceGraph;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/** embedded 3d viewport for use on a 2d surface */
public class View3Din2D extends PaintSurface {

    private final SpaceDisplayGraph3D space;

    public View3Din2D(SpaceDisplayGraph3D space) {
        this.space = space;
    }

    @Override
    protected void paint(GL2 gl, ReSurface r) {
        gl.glPushMatrix();

        int px1 = Math.round((x() - r.x1)*r.scaleX);
        int py1 = Math.round((y() - r.y1)*r.scaleY);
        int px2 = Math.round((x() - r.x1 + w())*r.scaleX);
        int py2 = Math.round((y() - r.y1 + h())*r.scaleY);
        gl.glViewport(px1, py1, px2-px1, py2-py1);

        render(gl, r);

        //restore ortho state
        gl.glMatrixMode(GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glViewport(0,0, Math.round(r.pw), Math.round(r.ph));
        gl.glOrtho(0, r.pw, 0, r.ph, -1.5, 1.5);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        gl.glPopMatrix();
    }

    protected void render(GL2 gl, ReSurface r) {
//        space.camPos((float)(Math.random()*0.5f), (float)(Math.random()*0.5f), 5);
//        space.camFwd((float)(Math.random()-0.5f)*2, (float)(Math.random()-0.5f)*2, -1);

        space.renderVolumeEmbedded(r.dtS(), gl, bounds);
    }

    public static void main(String[] args) {
        SpaceDisplayGraph3D s = new SpaceDisplayGraph3D().camPos(0, 0, 5);
//        for (int x = -10; x < 10; x++) {
//            for (int y = -10; y < 10; y++) {
//                s.add(
//                    new SimpleSpatial().move(x, y, 0).scale(0.75f).color(1, 1, 1)
//                );
//            }
//        }

        s.add(
                            new CuboidSurfaceGraph("x",
                        new BitmapLabel("y"),
                        2, 2));

        SpaceGraph.window(new Splitting(new PushButton("y"), 0.9f, new Splitting(
            new View3Din2D(s),
    0.1f, new PushButton("x")).resizeable()).resizeable(), 800, 800);
    }
}
