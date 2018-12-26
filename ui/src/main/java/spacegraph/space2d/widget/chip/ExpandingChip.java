package spacegraph.space2d.widget.chip;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.container.unit.UnitContainer;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.port.Wire;
import spacegraph.space2d.widget.windo.GraphEdit;
import spacegraph.space2d.widget.windo.Windo;

import java.util.Map;
import java.util.function.Supplier;

import static spacegraph.space2d.container.Bordering.S;

public class ExpandingChip extends MutableUnitContainer {

    public final Supplier<Surface> builder;
    private final CheckBox button;

    public ExpandingChip(String label, Supplier<Surface> builder) {
        super();

        this.builder = builder;
        this.button = new CheckBox(label);

        button.on((state)->{
            synchronized (ExpandingChip.this) {
                if (state) {
                    button.remove();
                    set(new Bordering(builder.get()).set(S, button));
                } else {
                    button.remove();
                    set(button);
                }
            }
        });

        set(button);
    }

}
