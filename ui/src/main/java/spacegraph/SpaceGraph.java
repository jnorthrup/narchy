package spacegraph;

import spacegraph.space2d.SpaceGraphFlat;
import spacegraph.space2d.Surface;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.space2d.widget.meta.AutoSurface;
import spacegraph.space3d.SpaceGraphPhys3D;
import spacegraph.space3d.Spatial;
import spacegraph.video.JoglSpace;

public enum SpaceGraph { ;


    public static JoglSpace window(Surface s, int w, int h) {
        JoglSpace win = new SpaceGraphFlat(
                new ZoomOrtho(s)
        );
        if (w > 0 && h > 0) {

            win.show(w, h);
        }
        return win;
    }

    public static JoglSpace window(Object o, int w, int h) {
        if (o instanceof JoglSpace) {
            JoglSpace s = (JoglSpace) o;
            s.show(w, h);
            return s;
        } else if (o instanceof Spatial) {
            return SpaceGraphPhys3D.window(((Spatial) o), w, h);
        } else if (o instanceof Surface) {
            return window(((Surface) o), w, h);
        } else {
            return window(new AutoSurface(o), w, h);
        }
    }
}
