package spacegraph.space2d.widget.meta;

import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.UnitContainer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

abstract public class AbstractCachedSurface<X extends Surface> extends UnitContainer<X> {



    final AtomicBoolean invalid = new AtomicBoolean(false);
    final FasterList<BiConsumer<GL2, ReSurface>> render = new FasterList();

    /** set to false to disable the caching */
    protected boolean cache = true;

    protected AbstractCachedSurface(X the) {
        super(the);
    }



    abstract protected void update();

    @Override
    protected void doLayout(float dtS) {
        invalid.set(true);
        super.doLayout(dtS);
    }

    protected final void updateIfShowing() {
        if (showing()) {
            update();
        }
    }

    public AbstractCachedSurface<X> cache(boolean cache) {
        this.cache = cache;
        return this;
    }

    /** TODO double buffer to prevent 'tearing' */
    @Override protected void renderContent(ReSurface r) {
        if (cache) {
            if (invalid.compareAndSet(true, false)) {
                r.record(the(), render);
                invalid.set(false);
            } else {
                r.play(render);
            }
        } else {
            render.clear();
            super.renderContent(r);
        }
    }

    @Override
    protected void starting() {

        invalid.set(true);

        super.starting();
    }





}
