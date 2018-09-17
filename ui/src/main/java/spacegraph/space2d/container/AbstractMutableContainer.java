package spacegraph.space2d.container;

import spacegraph.space2d.Surface;

public abstract class AbstractMutableContainer extends Container {

    @Override
    protected void starting() {
        synchronized (this) {
            forEach(c -> {
                assert (c.parent == null || c.parent == AbstractMutableContainer.this) : c + " has parent " + c.parent + " when trying to add to " + AbstractMutableContainer.this;
                c.start(this);
            });
        }
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
