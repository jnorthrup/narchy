package spacegraph.space2d.container;

import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class UnitContainer extends Container {

    public final Surface the;

    protected UnitContainer(Surface the) {
        this.the = the;
    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            the.start(this);
            layout();
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            the.stop();
            return true;
        }
        return false;
    }

    /** default behavior: inherit bounds directly */
    @Override
    protected void doLayout(int dtMS) {
        the.pos(bounds);
    }

    @Override
    public final int childrenCount() {
        return 1;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        o.accept(the);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return o.test(the);
    }

    @Override
    public final boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }
}
