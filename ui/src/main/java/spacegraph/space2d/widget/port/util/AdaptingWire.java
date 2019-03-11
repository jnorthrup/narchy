package spacegraph.space2d.widget.port.util;

import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.Wire;

import java.util.List;
import java.util.function.Function;

/** holds a pair of function lists */
public class AdaptingWire extends Wire {

    private final List<Function> abAdapters, baAdapters;

    /** current enabled strategy (selection index) */
    volatile int whichAB = -1, whichBA = -1;

    public AdaptingWire(Wire w, List<Function> ab, List<Function> ba) {
        super(w);
        if (!ab.isEmpty()) {
            whichAB = 0;
            this.abAdapters = ab;
        } else { this.abAdapters = null; }
        if (!ba.isEmpty()) {
            whichBA = 0;
            this.baAdapters = ba;
        }else { this.baAdapters = null; }
    }

    @Override
    protected Object transfer(Surface sender, Object x) {
        if (sender == a && whichAB >= 0) {
            x = (abAdapters.get(whichAB)).apply(x);
        } else if (sender == b && whichBA >= 0) {
            x = (baAdapters.get(whichBA)).apply(x);
        }
        return x;
    }
}
