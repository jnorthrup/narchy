package spacegraph.space2d.widget.tab;

import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.text.VectorLabel;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 12/2/16.
 */
public class TabPane extends Splitting {


    private static final float CONTENT_VISIBLE_SPLIT = 0.9f;

    private final Gridding tabs;

    private final AbstractContent model;

    private Function<Surface, Surface> wrapper = x->x;




    public TabPane(Map<String, Supplier<Surface>> builder) {
        this(builder, CheckBox::new);
    }

    public TabPane(Map<String, Supplier<Surface>> builder, Function<String, ToggleButton> buttonBuilder) {
        this();
        addToggles(builder, buttonBuilder);
    }

    /** view model */
    abstract static class AbstractContent {
        abstract public Surface view();

        abstract public void add(Surface surface);
        abstract public boolean remove(Surface surface);

        public abstract boolean isEmpty();
    }

    static class GridContent extends AbstractContent {

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
        public void add(Surface surface) {
            view.add(surface);
        }

        @Override
        public boolean remove(Surface surface) {
            return view.remove(surface);
        }
    }

    public TabPane() {
        this(new GridContent());
    }
    public TabPane(AbstractContent content) {
        super();

        unsplit();

        this.model = content;

        tabs = new Gridding();

        T(tabs);
        B(model.view());


    }

    public TabPane addToggles(Map<String, Supplier<Surface>> builder) {
        addToggles(builder, CheckBox::new);
        return this;
    }

    public void addToggles(Map<String, Supplier<Surface>> builder, Function<String, ToggleButton> buttonBuilder) {
        builder.entrySet().stream().map(x -> {
            return addToggle(buttonBuilder, x.getKey(), x.getValue());
        }).forEach(tabs::add);
    }

    public Surface addToggle(String label, Supplier<Surface> creator) {
        return addToggle(CheckBox::new, label, creator);
    }

    public Surface addToggle(Function<String, ToggleButton> buttonBuilder, String label, Supplier<Surface> creator) {
        final Surface[] created = {null};
        ObjectBooleanProcedure<ToggleButton> toggleInside = (cb, onOrOff) -> {
            toggle(creator, onOrOff, created, true);
        };

        Runnable toggleOutside = () -> {
            //Exe.invokeLater(()->{
                toggle(creator, true, created, false);
            //});
        };

        ToggleButton bb = buttonBuilder.apply(label).on(toggleInside);
        PushButton cc = PushButton.awesome("external-link").click(toggleOutside);

        return Splitting.row(bb, 0.75f, new AspectAlign(cc, AspectAlign.Align.RightTop,1, 0.75f));
    }

    void toggle(Supplier<Surface> creator, boolean onOrOff, Surface[] created, boolean inside) {
        Surface cx;
        if (onOrOff) {
            try {
                cx = creator.get();
            } catch (Throwable t) {
                String msg = t.getMessage();
                if (msg == null)
                    msg = t.toString();
                cx = new VectorLabel(msg);
            }
            cx = wrapper.apply(cx);
        } else {
            cx = null;
        }
        synchronized(TabPane.this) {

            if (onOrOff) {

                if (inside) {
                    model.add(created[0] = cx);
                    split();
                } else {
                    window(created[0] = cx, 800, 800);
                }



            } else {

                if (created[0] != null) {
                    model.remove(created[0]);
                    created[0] = null;
                }
                if (model.isEmpty()) {
                    unsplit();
                }

            }

        }
    }


    public TabPane setWrapper(Function<Surface,Surface> wrapper) {
        synchronized (this) {
            this.wrapper = wrapper;
            return this;
        }
    }

    protected void split() {
        split(CONTENT_VISIBLE_SPLIT);
    }

    protected void unsplit() {
        split(0f);
    }


//    public static class TabWall extends TabPane {
//
//        public TabWall() {
//            super();
//            setContent(new GraphEdit());
//            setWrapper(x -> new Windo(new MetaFrame(x)).size(w()/2, h()/2));
//        }
//    }

}
