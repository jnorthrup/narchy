package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.function.BiFunction;

abstract public class AbstractBiFunctionChip<X, Y, Z> extends Gridding {
    protected final Port xIn, yIn;
    protected final Port out;

    //buffers
    volatile X x;
    volatile Y y;

    protected AbstractBiFunctionChip() {
        super();
        out = new Port();
        xIn = new Port((Object a) -> {
            AbstractBiFunctionChip.this.x = (X) a;
            if (y != null)
                commit(x, y);
        });
        yIn = new Port((Object b) -> {
            AbstractBiFunctionChip.this.y = (Y) b;
            if (x != null)
                commit(x, y);
        });

        set(new Gridding(new LabeledPane("x in", xIn), new LabeledPane("y in", yIn)),
                new LabeledPane("f(x,y)", out));

    }

    abstract protected BiFunction<X, Y, Z> f();

    private void commit(X x, Y y) {
        Z z = f().apply(x, y);
        if (z != null)
            out.out(z);
    }
}
