package spacegraph.space2d.widget.menu;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.MutableUnitContainer;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by me on 12/2/16.
 */
abstract public class Menu extends MutableUnitContainer {

    protected final ContentView content;

    public final Map<String, Supplier<Surface>> options;


    protected Function<Surface, Surface> wrapper = x->x;


    /** view model */
    abstract static class ContentView {
        abstract public Surface view();

        abstract public void active(Surface surface);
        abstract public boolean inactive(Surface surface);

        public abstract boolean isEmpty();
    }


    static class GridView extends ContentView {


        final Gridding view = new Gridding();

        @Override
        public boolean isEmpty() {
            return view.isEmpty();
        }

        @Override
        public Surface view() {
            return view;
        }

        @Override
        public void active(Surface surface) {
            view.add(surface);
        }

        @Override
        public boolean inactive(Surface surface) {
            return view.remove(surface);
        }
    }


    public Menu(Map<String, Supplier<Surface>> options) {
        this(new GridView(), options);
    }

    public Menu(ContentView content, Map<String, Supplier<Surface>> options) {
        super();

        this.options = options;
        this.content = content;
    }

    public Menu setWrapper(Function<Surface,Surface> wrapper) {
        synchronized (this) {
            this.wrapper = wrapper;
            return this;
        }
    }

    /** TODO */
    abstract public static class ContentWall extends ContentView {

        public ContentWall() {
            super();
//            setContent(new GraphEdit());
//            setWrapper(x -> new Windo(new MetaFrame(x)).size(w()/2, h()/2));
        }
    }




}
