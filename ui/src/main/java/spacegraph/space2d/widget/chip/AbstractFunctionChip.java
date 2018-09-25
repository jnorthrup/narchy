package spacegraph.space2d.widget.chip;

import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.port.Port;

import java.util.function.Function;

abstract public class AbstractFunctionChip<X,Y> extends Gridding {
    protected final Port in;
    protected final Port out;

    protected AbstractFunctionChip() {
        super();
        out = new Port();
        in = new Port((X x)->{
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
    abstract protected Function<X,Y> f();
}
