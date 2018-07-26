package spacegraph.space2d.widget.tab;

import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Clipped;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.windo.Wall;
import spacegraph.space2d.widget.windo.Windo;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by me on 12/2/16.
 */
public class TabPane extends Splitting {


    private static final float CONTENT_VISIBLE_SPLIT = 0.9f;
    private final Gridding tabs;
    private MutableContainer content;
    private Function<Surface, Surface> wrapper = x->x;





    public TabPane(Map<String, Supplier<Surface>> builder, Function<String, ToggleButton> buttonBuilder) {
        this();
        addToggles(builder, buttonBuilder);
    }

    public TabPane() {
        super();

        unsplit();

        content = new Gridding();

        tabs = new Gridding();

        set(tabs, content);


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

    public ToggleButton addToggle(String label, Supplier<Surface> creator) {
        return addToggle(CheckBox::new, label, creator);
    }

    public ToggleButton addToggle(Function<String, ToggleButton> buttonBuilder, String label, Supplier<Surface> creator) {
        final Surface[] created = {null};
        ObjectBooleanProcedure<ToggleButton> toggle = (cb, a) -> {
            synchronized(TabPane.this) {

                if (a) {
                    Surface cx;
                    try {
                        cx = creator.get();
                    } catch (Throwable t) {
                        String msg = t.getMessage();
                        if (msg == null)
                            msg = t.toString();
                        cx = new Label(msg);
                    }


                    content.add(created[0] = wrapper.apply(cx));
                    split();

                } else {

                    if (created[0] != null) {
                        content.remove(created[0]);
                        created[0] = null;
                    }
                    if (content.isEmpty()) {
                        unsplit();
                    }

                }

            }
        };

        ToggleButton bb = buttonBuilder.apply(label);
        return bb.on(toggle);
    }

    public TabPane setContent(MutableContainer next) {
        synchronized (this) {
            content = next;
            set(1, new Clipped(next));
            next.addAll(content.children());
            return this;
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


    public static class TabWall extends TabPane {

        public TabWall() {
            super();
            setContent(new Wall());
            setWrapper(x -> new Windo(new MetaFrame(x)).size(w()/2, h()/2));
        }
    }

}
