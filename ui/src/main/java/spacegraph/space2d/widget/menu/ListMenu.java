package spacegraph.space2d.widget.menu;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;

import java.util.Map;
import java.util.function.Supplier;

/** selectable list, views one item at a time */
public class ListMenu extends Menu {

    private final Surface index;
    private final Splitting wrap;

    public ListMenu(Map<String, Supplier<Surface>> options) {
        super(options);
        index = new ButtonSet(ButtonSet.Mode.One, options.entrySet().stream().map(e -> {
            return new CheckBox(e.getKey()).on(()->view(e.getValue()));
        })::iterator);
        ((ButtonSet) index).vertical();
        wrap = Splitting.row(index, 0.2f, new EmptySurface());
        set(wrap);
    }

    private void view(Supplier<Surface> value) {
        wrap.R(value.get());
    }

}
