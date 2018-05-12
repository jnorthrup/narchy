package spacegraph;

import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;
import spacegraph.space2d.SpaceGraphFlat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space2d.widget.windo.Dyn2DSurface;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.Spatial;
import spacegraph.video.JoglSpace;

import java.util.function.Supplier;

public enum SpaceGraph { ;



    /** creates window with 2d with single surface layer, maximized to the size of the window */
    public static JoglSpace window(Surface s, int w, int h, boolean async) {
        JoglSpace win = new SpaceGraphFlat(
                new ZoomOrtho(s)
        );
        if (w > 0 && h > 0) {
            win.show(w, h, async);
        }
        return win;
    }

    public static JoglSpace window(Object o, int w, int h) {
        return window(o, w, h, false);
    }

    /** generic window creation entry point */
    public static JoglSpace window(Object o, int w, int h, boolean async) {
        if (o instanceof JoglSpace) {
            JoglSpace s = (JoglSpace) o;
            s.show(w, h, async);
            return s;
        } else if (o instanceof Spatial) {
            SpaceGraphPhys3D win = new SpaceGraphPhys3D(((Spatial) o));
            win.show(w, h,async);
            return win;
        } else if (o instanceof Surface) {
            return window(((Surface) o), w, h, async);
        } else {
            return window(new AutoSurface<>(o), w, h, async);
        }
    }


    /** creates window with new 2d physics "wall" containing the provided widgets */
    public static Dyn2DSurface wall(int width, int height) {
        Dyn2DSurface s = new Dyn2DSurface();
        s.pos(-1, -1, 1, 1);

        //SpaceLogConsole log = new SpaceLogConsole();
        //log.visible(false);

        //Label statusBar = new Label();


//        HUDOrtho hud = new HUDOrtho();
//
//        hud.set(
//                new Splitting(new EmptySurface(),
//                        //new Gridding(new EmptySurface(), new EmptySurface(), new EmptySurface(), log),
//                        new Gridding(new EmptySurface(), statusBar),
//                        0.1f)
//        );
//        //log.pos(0, 0.9f, 0.5f * width, 1f);
//        ///System.out.println(hud.bounds);
//
////        log.visible(false);
////        hud.set(
////            new Splitting(
////                new Gridding(new EmptySurface(), new EmptySurface(), new EmptySurface(), log),
////                new Gridding(new PushButton("+"),
////                        //new OmniBox(),
////                new CheckBox("Log", (Runnable)log::visible)),
////                0.1f
////        ));

        SpaceGraphFlat g = new SpaceGraphFlat(
                new ZoomOrtho(s) {


                    @Override
                    public boolean autoresize() {
                        zoom(s);
                        return false;
                    }

                    @Override
                    public void log(@Nullable Object key, float duration, Level level, Supplier<String> message) {
                        //if (log.visible())
                        //log.log(key, duration, level, message);
//                        if (statusBar.visible())
//                            statusBar.text(message.get());
                        //else: buffer?
                    }
                }
                //,hud
        );
        g.show(width, height, false);



        //win.setUndecorated(true);
        return s;
    }
}
