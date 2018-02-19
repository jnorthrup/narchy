package spacegraph;

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
