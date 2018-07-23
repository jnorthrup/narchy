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
    private final ButtonSet tabs;
    private MutableContainer content;
    private Function<Surface, Surface> wrapper = x->x;


    public TabPane(Map<String, Supplier<Surface>> builder) {
        this(ButtonSet.Mode.Multi, builder);
    }

    private TabPane(ButtonSet.Mode mode, Map<String, Supplier<Surface>> builder) {
        this(mode, builder, CheckBox::new);
    }

    public TabPane(ButtonSet.Mode mode, Map<String, Supplier<Surface>> builder, Function<String, ToggleButton> buttonBuilder) {
        super();

        unsplit();

        content = new Gridding();

        tabs = new ButtonSet<>(mode, builder.entrySet().stream().map(x -> {
            final Surface[] created = {null};
            Supplier<Surface> creator = x.getValue();
            String label = x.getKey();
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
        }).toArray(ToggleButton[]::new));

        set(tabs, content);

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

        public TabWall(Map<String, Supplier<Surface>> builder) {
            super(builder);
            setContent(new Wall());
            setWrapper(x -> new Windo(new MetaFrame(x)).size(w()/2, h()/2));
        }
    }

}
