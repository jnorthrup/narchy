package spacegraph.space2d.container;

import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

public abstract class AbstractMutableContainer extends Container {

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            synchronized (this) {
                //add pre-added
                forEach(c -> {
                    assert (c.parent == null): c + " has parent " + c.parent + " when trying to add to " + AbstractMutableContainer.this;
                    c.start(this);
                });
            }
            layout();
            return true;
        }
        return false;
    }

    @Override
    protected void doLayout(int dtMS) {
        forEach(Surface::layout);
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            clear();
            return true;
        }
        return false;
    }

    protected abstract void clear();
}
