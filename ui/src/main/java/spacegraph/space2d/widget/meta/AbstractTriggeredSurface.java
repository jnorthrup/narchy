package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import jcog.event.Off;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.unit.UnitContainer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

abstract public class AbstractTriggeredSurface<X extends Surface> extends UnitContainer<X> {

    private Off on;

    final AtomicBoolean invalid = new AtomicBoolean(false);
    final List<BiConsumer<GL2,SurfaceRender>> render = new FasterList();

    protected AbstractTriggeredSurface(X the) {
        super(the);
    }

    abstract public Off on();

    abstract protected void update();

    @Override
    protected void doLayout(int dtMS) {
        invalid.set(true);
        super.doLayout(dtMS);
    }

    protected final void updateIfShowing() {
        if (showing()) {
            update();
            invalid.set(true);
        }
    }

    /** TODO double buffer to prevent 'tearing' */
    @Override protected void compileChildren(SurfaceRender r) {
        if (invalid.compareAndSet(true, false))
            r.record(the(), render);
        else
            r.play(render);
    }

    @Override
    protected void starting() {
        super.starting();

        assert(on == null);
        on = on();
        invalid.set(true);
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
