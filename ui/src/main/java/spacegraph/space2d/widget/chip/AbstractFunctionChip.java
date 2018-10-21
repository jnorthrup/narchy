package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.function.Function;

abstract public class AbstractFunctionChip<X,Y> extends Gridding {
    protected final Port in;
    protected final Port out;

    protected AbstractFunctionChip() {
        super();
        out = new Port();
        in = new Port((Object x) -> {
            try {
                Y y = f().apply((X) x);
                if (y != null)
                    out.out(y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        set(LabeledPane.awesome(out, "angle-right"), LabeledPane.awesome(in, "question-circle"));
    }

    abstract protected Function<X, Y> f();

}
