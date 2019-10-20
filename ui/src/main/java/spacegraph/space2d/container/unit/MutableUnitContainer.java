package spacegraph.space2d.container.unit;

import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;

import java.util.concurrent.atomic.AtomicReference;

/** TODO make more bulletproof w/ locking. this is not complete */
public class MutableUnitContainer<S extends Surface> extends AbstractUnitContainer<S> {

    private final AtomicReference<Surface> the = new AtomicReference<>();

    public MutableUnitContainer() {
        this(null);
    }

    public MutableUnitContainer(S the) {
        super();
        set(the);
    }

    @Override protected void renderContent(ReSurface r) {
        var t = the.getOpaque();
        if (t!=null)
            t.renderIfVisible(r);
    }

    public final MutableUnitContainer<S> set(S next) {

        var prev = this.the.getAndSet(next);
        if (prev == next)
            return this; //same instance

        if (prev !=null)
            prev.delete();

        if (next!=null) {
            if (parent != null) {
                next.start(this);
                layout();
            }

        }

        return this;
    }



    @Override
    public S the() {
        return (S) the.get();
    }
}
