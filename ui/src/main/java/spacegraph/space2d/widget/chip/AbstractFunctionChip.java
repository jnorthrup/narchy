package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.function.BiFunction;
import java.util.function.Function;

abstract public class AbstractFunctionChip<X,Y> extends Gridding {
    protected final Port in;
    protected final Port out;

    protected AbstractFunctionChip() {
        super();
        out = new Port();
        in = new Port((X x) -> {
            try {
                Y y = f().apply(x);
                if (y != null)
                    out.out(y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        set(out, in);
    }

    abstract protected Function<X, Y> f();

    abstract public static class AbstractBiFunctionChip<X, Y, Z> extends Gridding {
        protected final Port xIn, yIn;
        protected final Port out;

        //buffers
        volatile X x;
        volatile Y y;

        protected AbstractBiFunctionChip() {
            super();
            out = new Port();
            xIn = new Port((X a) -> {
                AbstractBiFunctionChip.this.x = a;
                if (y != null)
                    commit(x, y);
            });
            yIn = new Port((Y b) -> {
                AbstractBiFunctionChip.this.y = b;
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
}
