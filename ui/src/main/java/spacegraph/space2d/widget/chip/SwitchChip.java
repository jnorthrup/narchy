package spacegraph.space2d.widget.chip;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.PortVector;

import java.util.function.Function;

public class SwitchChip extends Gridding {

    final PortVector out;

    public SwitchChip(int size) {
        this(size, (p) -> p);
    }

    /**
     * port renderer can be used for example to create enable-button decorated ports
     */
    public SwitchChip(int size, Function<Port, Surface> portRenderer) {
        super();
        Port in = new Port((x) -> {
            if (x instanceof Number) {
                trigger(((Number) x).intValue());
            } else if (x instanceof Boolean) {
                trigger(((Boolean) x) ? 1 : 0);
            } else {
                //nothing
            }
        });

        set(in, out = new PortVector(size, portRenderer));
    }

    /** TODO different modes: adhoc, exclusive, etc */
    public void trigger(int x) {
        out.out(x, Boolean.TRUE);
    }

}
