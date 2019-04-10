package spacegraph;

import jcog.Skill;
import jdk.jshell.tool.JavaShellToolBuilder;
import spacegraph.space2d.OrthoSurfaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space3d.SpaceDisplayGraph3D;
import spacegraph.space3d.Spatial;
import spacegraph.video.JoglDisplay;

@Skill("Direct_manipulation_interface")
public class SpaceGraph {

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
    public static OrthoSurfaceGraph surfaceWindow(Surface s, int w, int h) {
        return new OrthoSurfaceGraph(s, w, h);
    }

    /**
     * generic window creation entry point
     */
    public static JoglDisplay window(Object o, int w, int h) {
        if (o instanceof JoglDisplay) {
            JoglDisplay s = (JoglDisplay) o;
            s.video.show(w, h);
            return s;
        } else if (o instanceof Spatial) {
            SpaceDisplayGraph3D win = new SpaceDisplayGraph3D(((Spatial) o));
            win.video.show(w, h);
            return win;
        } else {
            Surface s = o instanceof Surface ? ((Surface) o) : new ObjectSurface<>(o);
            return surfaceWindow(s, w, h);
        }
    }


//    /** creates window with new 2d physics "wall" containing the provided widgets */
//    @Deprecated public static GraphEdit wall(int width, int height) {
//        GraphEdit s = new GraphEdit();
//        s.pos(-1, -1, 1, 1);
//
//
//        ZoomOrtho ortho = new ZoomOrtho(s) {
//
//
//            @Override
//            public boolean autoresize() {
//                return false;
//            }
//
//            @Override
//            public void log(@Nullable Object key, float duration, Level level, Supplier<String> message) {
//
//
//            }
//
////            @Override
////            protected boolean tangible() {
////                return true;
////            }
//        };
//
//        Exe.invokeLater(()->{
//            SpaceGraphFlat g = new SpaceGraphFlat(ortho);
//            g.show(width, height);
//            Exe.invokeLater(()->{
//                ortho.zoom(s);
//            });
//        });
//
//
//
//        return s;
//    }
}
