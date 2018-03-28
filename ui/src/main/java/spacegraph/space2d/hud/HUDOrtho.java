package spacegraph.space2d.hud;

import spacegraph.space2d.Surface;

public class HUDOrtho extends Ortho {

    public HUDOrtho() {
        super();
    }

    @Override
    public void zoom(Surface su) {
        //HACK disabled
    }

    @Override
    protected boolean maximize() {
        return true;
    }
//    @Override
//    public void setSurface(Surface content) {
//        hud.children(content);
//    }

}
