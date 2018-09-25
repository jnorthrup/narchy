package spacegraph.space2d.widget.port;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Gridding;

import java.util.function.Function;

public class PortVector extends Gridding {
    public final Port[] out;

    public PortVector(int size) {
        this(size, (x)->x);
    }

    public PortVector(int size, Function<Port, Surface> portRenderer) {
        super();
        Surface[] outs = new Surface[size];

        out = new Port[size];
        for (int i = 0; i < size; i++) {
            out[i] = new Port();
            outs[i] = portRenderer.apply(out[i]);
        }

        set(outs);
    }

    public void out(int x, Object value) {
        out[x].out(value);
    }
}
