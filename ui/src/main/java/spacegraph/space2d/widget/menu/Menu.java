package spacegraph.space2d.widget.menu;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.MutableUnitContainer;

import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Created by me on 12/2/16.
 */
public abstract class Menu extends MutableUnitContainer {

    protected final MenuView content;

    public final Map<String, Supplier<Surface>> options;


    protected UnaryOperator<Surface> wrapper = new UnaryOperator<Surface>() {
        @Override
        public Surface apply(Surface x) {
            return x;
        }
    };


    /** view model */
    public static interface MenuView {
        Surface view();

        void active(Surface surface);
        boolean inactive(Surface surface);

        boolean isEmpty();
    }

    public Menu(Map<String, Supplier<Surface>> menu, MenuView view) {
        super();
        this.options = menu;
        this.content = view;
    }

//    public Menu setWrapper(UnaryOperator<Surface> wrapper) {
//        synchronized (this) {
//            this.wrapper = wrapper;
//            return this;
//        }
//    }


}
