package spacegraph.space2d.widget.meta;

import jcog.event.Off;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.UnitContainer;

abstract public class AbstractTriggeredSurface<X extends Surface> extends UnitContainer<X> {

    private Off on;

    protected AbstractTriggeredSurface(X the) {
        super(the);
    }

    abstract public Off on();

    abstract protected void update();

    protected final void updateIfShowing() {
        if (showing()) {
            update();
        }
    }


    @Override
    protected void starting() {
        super.starting();

        assert(on == null);
        on = on();
        assert(on!=null);
    }


    @Override
    protected void stopping() {
        assert(on!=null);
        on.off();
        on = null;
        super.stopping();
    }


}
