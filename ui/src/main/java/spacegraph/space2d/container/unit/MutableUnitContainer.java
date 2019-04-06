package spacegraph.space2d.container.unit;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;

import java.util.concurrent.atomic.AtomicReference;

public class MutableUnitContainer<S extends Surface> extends AbstractUnitContainer<S> {

    private final AtomicReference<Surface> the = new AtomicReference<Surface>();

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
            next = new EmptySurface(); //HACK TODO just use null if that works
        }
        Surface prev = this.the.getAndSet(next);
        if (prev == next)
            return this; //same instance

        if (prev !=null)
            prev.stop();

        if (parent!=null) {
            next.start(this);
        }

        layout();

        return this;
    }



    @Override
    public S the() {
        return (S) the.get();
    }
}
