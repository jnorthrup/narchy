package spacegraph.space2d.widget.menu;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.MutableUnitContainer;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by me on 12/2/16.
 */
abstract public class Menu extends MutableUnitContainer {

    protected final MenuView content;

    public final Map<String, Supplier<Surface>> options;


    protected Function<Surface, Surface> wrapper = x->x;


    /** view model */
    public abstract static class MenuView {
        abstract public Surface view();

        abstract public void active(Surface surface);
        abstract public boolean inactive(Surface surface);

        public abstract boolean isEmpty();
    }

    public Menu(Map<String, Supplier<Surface>> menu, MenuView view) {
        super();

        this.options = menu;
        this.content = view;
    }

    public Menu setWrapper(Function<Surface,Surface> wrapper) {
        synchronized (this) {
            this.wrapper = wrapper;
            return this;
        }
    }


}
