package spacegraph.space2d.container;

import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

public abstract class AbstractMutableContainer extends Container {

    @Override
    protected void starting() {
        synchronized (this) {
            forEach(c -> {
                c.start(this);
            });
        }
        layout();
    }


    @Override
    protected void stopping() {
        clear();
    }

    public boolean removeChild(Surface s) {
        return false; //by default dont support external removal
    }

    protected abstract void clear();
}
