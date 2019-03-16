package spacegraph;

import jcog.Skill;
import spacegraph.space2d.SpaceGraphFlat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.Spatial;
import spacegraph.video.JoglSpace;

@Skill("Direct_manipulation_interface")
public enum SpaceGraph { ;


    /** creates window with 2d with single surface layer, maximized to the size of the window */
    public static JoglSpace window(Surface s, int w, int h) {
        JoglSpace win = new SpaceGraphFlat(s);
        if (w > 0 && h > 0) {
            win.display.show(w, h);
        }
        return win;
    }

    /** generic window creation entry point */
    public static JoglSpace window(Object o, int w, int h) {
        if (o instanceof JoglSpace) {
            JoglSpace s = (JoglSpace) o;
            s.display.show(w, h);
            return s;
        } else if (o instanceof Spatial) {
            SpaceGraphPhys3D win = new SpaceGraphPhys3D(((Spatial) o));
            win.display.show(w, h);
            return win;
        } else if (o instanceof Surface) {
            return window(((Surface) o), w, h);
        } else {
            return window(new ObjectSurface<>(o), w, h);
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
