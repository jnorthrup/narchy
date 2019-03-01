package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.function.BiFunction;

abstract public class AbstractBiFunctionChip<X, Y, Z> extends Gridding {
    protected final TypedPort<X> xIn;
    protected final TypedPort<Y> yIn;
    protected final TypedPort<Z> out;

    //buffers
    volatile X x;
    volatile Y y;

    protected AbstractBiFunctionChip(Class<? super X> xClass, Class<? super Y> yClass, Class<? super Z> zClass) {
        super();
        out = new TypedPort<>(zClass);
        xIn = new TypedPort<>(xClass, (X a) -> {
            AbstractBiFunctionChip.this.x = a;
            if (y != null)
                commit(x, y);
        });
        yIn = new TypedPort<>(yClass, (Y b) -> {
            AbstractBiFunctionChip.this.y = b;
            if (x != null)
                commit(x, y);
        });

        set(new Gridding(
                new Gridding(
                        LabeledPane.the(this.xIn.type + " x (in)", xIn),
                        LabeledPane.the(this.yIn.type + " y (in)", yIn)
                ),
                LabeledPane.the(this.out.type + " out", out))
        );

    }

    abstract protected BiFunction<X, Y, Z> f();

    private void commit(X x, Y y) {
        Z z = f().apply(x, y);
        if (z != null)
            out.out(z);
    }
}
