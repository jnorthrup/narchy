package spacegraph;

import jcog.Skill;
import jcog.thing.Thing;
import jdk.jshell.tool.JavaShellToolBuilder;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.video.JoglDisplay;
import spacegraph.video.OrthoSurfaceGraph;

@Skill("Direct_manipulation_interface")
public class SpaceGraph extends Thing<SpaceGraph, Object> {

    public static void main(String[] args) throws Exception {
        //https://www.infoq.com/articles/jshell-java-repl
        /*
        You can utilize this directory to store any startup or initialization code. This is a feature that's directly supported by JShell with the --startup parameter.

$ jshell --startup startups/custom-startup
         */
        JavaShellToolBuilder.builder()
                .start(
                //        "--classpath=\"*\""
                );

    }

    /**
     * creates window with 2d with single surface layer, maximized to the size of the window
     */
    public static OrthoSurfaceGraph window(Surface s, int w, int h) {
        return new OrthoSurfaceGraph(s, w, h);
    }

    /**
     * generic window creation entry point
     */
    public static JoglDisplay window(Object o, int w, int h) {
        if (o instanceof JoglDisplay) {
            var s = (JoglDisplay) o;
            s.video.show(w, h);
            return s;
        } else if (o instanceof Spatial) {
            var win = new SpaceGraph3D(((Spatial) o));
            win.video.show(w, h);
            return win;
        } else if (o instanceof Surface) {
            return window((Surface) o, w, h);
        } else {
            return window(new ObjectSurface(o), w, h);
        }
    }


}
