package spacegraph.space2d.container.unit;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;

public class MutableUnitContainer<S extends Surface> extends AbstractUnitContainer<S> {

    private Surface the;

    public MutableUnitContainer() {
        this(null);
    }

    public MutableUnitContainer(S the) {
        super();
        set(the);
    }

    public final MutableUnitContainer set(S next) {
        synchronized(this) {
            if (next == null) {
                _set(new EmptySurface());
            } else if (this.the!=next) {
                _set(next);
            }
        }
        return this;
    }

    private void _set(Surface next) {
        if (the==next)
            return;

        if (the!=null)
            the.stop();

        the = next;

        if (parent!=null) {
            next.start(this);
            layout();
        }
    }

    @Override
    public S the() {
        return (S)the;
    }
}
