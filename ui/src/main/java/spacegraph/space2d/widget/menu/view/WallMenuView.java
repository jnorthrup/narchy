package spacegraph.space2d.widget.menu.view;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.Clipped;
import spacegraph.space2d.widget.menu.Menu;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Windo;

/** TODO */
public class WallMenuView extends Menu.MenuView {

    private final GraphEdit wall;

    public WallMenuView() {
        super();
//            setContent(new GraphEdit());
//            setWrapper(x -> new Windo(new MetaFrame(x)).size(w()/2, h()/2));

        this.wall = new GraphEdit() {
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
        Windo w = wall.add(surface);
        w.pos(10, 10, 400, 300); //TODO
    }

    @Override
    public boolean inactive(Surface surface) {
        return wall.remove((Object)surface)!=null;
    }

    @Override
    public boolean isEmpty() {
        return wall.childrenCount()==0;
    }
}
