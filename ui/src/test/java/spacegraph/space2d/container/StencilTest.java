package spacegraph.space2d.container;

import com.jogamp.opengl.GL2;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.video.Draw;

/** https://gist.github.com/daltonks/4c2d1c5e6fd5017ea9f0 */
public class StencilTest extends Surface {

    public static void main(String[] args) {
        SpaceGraph.window(new StencilTest(), 800, 800);
    }


    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {

        gl.glColor4f(1,1,1, 0.75f);
        Draw.rect(gl, bounds);

        Draw.stencilMask(gl, true, (g)-> {
            //"Drawing" the objects only ends up writing 1's to the stencil buffer no matter what color or depth they are
            //drawAllObjectsThatNeedAnOutline();
            gl.glColor3f(0,0,1);
            Draw.rect(gl, bounds.scale(0.75f).move(0.5f, 0.5f));
        }, (g)->{
            gl.glColor3f(1,0,0);
            Draw.rect(gl, bounds.scale(0.75f).move(Math.random()*w(), Math.random()*h()));
        });

        //Draw everything, including objects that need an outline (because we haven't really "drawn" them yet)
        //drawEverything();
        gl.glColor4f(0,1,0, 0.2f);
        Draw.rect(gl, bounds.scale(0.75f).move(Math.random()*w(), Math.random()*h()));

    }
}
