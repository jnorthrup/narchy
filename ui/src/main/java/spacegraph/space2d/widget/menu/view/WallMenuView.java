package spacegraph.space2d.widget.menu.view;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.menu.Menu;

/** TODO */
public class WallMenuView extends Menu.MenuView {

    private final EditGraph2D wall;

    public WallMenuView() {
        super();
//            setContent(new GraphEdit());
//            setWrapper(x -> new Windo(new MetaFrame(x)).size(w()/2, h()/2));

        this.wall = new EditGraph2D() {
//            @Override
//            public void doLayout(int dtMS) {
//                super.doLayout(dtMS);
//                //TODO move windows which are now outside of the view into view
//            }
        };
    }

    @Override
    public Surface view() {
        return new Clipped(wall);
    }

    @Override
    public void active(Surface surface) {
        Container w = wall.add(surface);
        w.pos(10, 10, 400, 300); //TODO
    }

    @Override
    public boolean inactive(Surface surface) {
        return wall.remove((Object)(((Surface)surface.parent).parent /* HACK */) )!=null;
    }

    @Override
    public boolean isEmpty() {
        return wall.childrenCount()==0;
    }
}
