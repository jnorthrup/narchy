package spacegraph.space2d.widget.menu;

import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.menu.view.GridMenuView;
import spacegraph.space2d.widget.meta.LazySurface;
import spacegraph.space2d.widget.meta.MetaHover;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class TabMenu extends Menu {

    private final Splitting wrap;

    private static final float MenuContentRatio = 0.9f;

    private final Map<String,ToggleButton> items = new LinkedHashMap();

    public TabMenu(Map<String, Supplier<Surface>> options) {
        this(options, new GridMenuView());
    }

    public TabMenu(Map<String, Supplier<Surface>> options, MenuView view) {
        this(options, view, CheckBox::new);
    }

    public TabMenu(Map<String, Supplier<Surface>> options, MenuView view, Function<String, ToggleButton> buttonBuilder) {
        super(options, view);
        items.clear();
        Gridding tabs = new Gridding();
        tabs.set(options.entrySet().stream()
                .map(x -> toggle(buttonBuilder, x.getKey(), x.getValue()))
                .collect(toList()));

        wrap = new Splitting(tabs, 0, content.view());

        set(wrap);
    }

    public final TabMenu set(String item, boolean enable) {
        ToggleButton b = items.get(item);
        b.on(enable);
        return this;
    }

    void toggle(ToggleButton button, Supplier<Surface> creator, boolean onOrOff, Surface[] created, boolean inside) {

        //wrap/decorate

        //VectorLabel label = ((ToggleButton) button).label;

//        if (label!=null)
//            cx = LabeledPane.the(button.term() /*buttonBuilder.apply(label.text())*/, cx);

            if (onOrOff) {
                Surface cx = new LazySurface(creator);
                cx = wrapper.apply(cx); assert(cx!=null);

                if (inside) {
                    content.active(created[0] = cx);
                    split();
                } else {
                    SpaceGraph.window(created[0] = cx, 800, 800);
                }

            } else {

                if (created[0] != null) {
                    boolean removed = content.inactive(created[0]);
                    assert(removed);
                    created[0] = null;
                }
                if (content.isEmpty()) {
                    unsplit();
                }

            }

    }


//    public void addToggle(String label, Supplier<Surface> creator) {
//        toggle(CheckBox::new, label, creator);
//    }

    public Surface toggle(Function<String, ToggleButton> buttonBuilder, String label, Supplier<Surface> creator) {
        final Surface[] created = {null};
        ObjectBooleanProcedure<ToggleButton> toggleInside = (button, onOrOff) -> {
//            Exe.invoke(()->{
                toggle(button, creator, onOrOff, created, true);
//            });
        };

        Runnable spawnOutside = () -> {
//            Exe.invoke(()->{
                toggle(null, creator, true, created, false);
//            });
        };

        ToggleButton bb = buttonBuilder.apply(label).on(toggleInside);
        items.put(label, bb);
        PushButton cc = PushButton.awesome("external-link").clicked(spawnOutside);

        //return Splitting.row(bb, 0.75f, new AspectAlign(cc, AspectAlign.Align.RightTop,1, 0.75f));

        AspectAlign ccc = new AspectAlign(cc, 1, AspectAlign.Align.TopRight, 0.25f, 0.25f);
        return new MetaHover(bb, ()->ccc);
        //return new Stacking(cc, ccc);

    }

    protected void split() {
        wrap.split(MenuContentRatio);
    }

    protected void unsplit() {
        wrap.split(0f);
    }


}
