package spacegraph.space2d.widget.chip;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.button.CheckBox;

import java.util.function.Function;
import java.util.function.Supplier;

import static spacegraph.space2d.container.Bordering.S;

public class ExpandingChip extends MutableUnitContainer {

    public final Function<ExpandingChip, Surface> builder;
    private final CheckBox button;

    public ExpandingChip(String label, Supplier<Surface> builder) {
        this(label, new Function<ExpandingChip, Surface>() {
            @Override
            public Surface apply(ExpandingChip x) {
                return builder.get();
            }
        });
    }

    public ExpandingChip(String label, Function<ExpandingChip,Surface> builder) {
        super();

        this.builder = builder;
        this.button = new CheckBox(label);

        button.on(new BooleanProcedure() {
            @Override
            public void value(boolean state) {
                synchronized (ExpandingChip.this) {
                    if (state) {
                        button.delete();
                        ExpandingChip.this.set(new Bordering(builder.apply(ExpandingChip.this)).set(S, button));
                    } else {
                        button.delete();
                        ExpandingChip.this.set(button);
                    }
                }
            }
        });

        set(button);
    }

}
