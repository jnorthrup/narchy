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

    public final MutableUnitContainer set(S _next) {
        Surface next = _next;
        if (next == null) {
            next = new EmptySurface();
        }
        synchronized(this) {
            if (this.the==next)
                return this;

            _set(next);
        }
        layout();
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
