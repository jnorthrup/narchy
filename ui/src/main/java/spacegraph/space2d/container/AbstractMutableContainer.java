package spacegraph.space2d.container;

public abstract class AbstractMutableContainer extends Container {

    @Override
    protected void starting() {
        forEach(c -> {
            assert (c.parent == null || c.parent == AbstractMutableContainer.this) : c + " has parent " + c.parent + " when trying to add to " + AbstractMutableContainer.this;
            c.start(this);
        });
    }

    @Override
    protected void stopping() {
        clear();
    }

    protected abstract void clear();
}
