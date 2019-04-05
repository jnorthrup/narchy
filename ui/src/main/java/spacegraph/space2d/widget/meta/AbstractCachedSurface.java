package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import jcog.event.Off;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.UnitContainer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

abstract public class AbstractCachedSurface<X extends Surface> extends UnitContainer<X> {

    private Off on;

    final AtomicBoolean invalid = new AtomicBoolean(false);
    final List<BiConsumer<GL2, ReSurface>> render = new FasterList();

    /** set to false to disable the caching */
    protected boolean cache = true;

    protected AbstractCachedSurface(X the) {
        super(the);
    }

    abstract public Off on();

    abstract protected void update();

    @Override
    protected void doLayout(float dtS) {
        invalid.set(true);
        super.doLayout(dtS);
    }

    protected final void updateIfShowing() {
        if (showing()) {
            update();
            invalid.set(true);
        }
    }


    public AbstractCachedSurface<X> cache(boolean cache) {
        this.cache = cache;
        return this;
    }

    /** TODO double buffer to prevent 'tearing' */
    @Override protected void renderChildren(ReSurface r) {
        if (cache) {
            if (!invalid.compareAndSet(true, false)) {
                r.play(render);
            } else {
                r.record(the(), render);
            }
        } else {
            render.clear();
            invalid.set(false);
            super.renderChildren(r);
        }
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
        on.pause();
        on = null;
        super.stopping();
    }



}
