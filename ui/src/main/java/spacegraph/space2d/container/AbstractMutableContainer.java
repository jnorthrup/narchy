package spacegraph.space2d.container;

import spacegraph.space2d.SurfaceBase;

public abstract class AbstractMutableContainer extends Container {

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {

            
            forEach(c -> {



                assert (c.parent == null || c.parent == AbstractMutableContainer.this) : c + " has parent " + c.parent + " when trying to add to " + AbstractMutableContainer.this;
                c.start(this);
            });

            return true;
        }
        return false;
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
