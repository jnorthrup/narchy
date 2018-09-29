package spacegraph.input.finger;

import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.port.util.Wire;

import java.util.List;
import java.util.function.Function;

public class AdaptingWire extends Wire {

    private final List<Function> abAdapters, baAdapters;
    /** current default strategy selection index */
    int whichAB = -1, whichBA = -1;

    public AdaptingWire(Wire w, List<Function> ab, List<Function> ba) {
        super(w);
        this.abAdapters = ab.isEmpty() ? null : ab;
        this.baAdapters = ba.isEmpty() ? null : ba;
        whichAB = abAdapters == null ?  -1 : 0;
        whichBA = baAdapters == null ?  -1 : 0;
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
