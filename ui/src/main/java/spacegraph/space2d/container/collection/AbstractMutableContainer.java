package spacegraph.space2d.container.collection;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ContainerSurface;

public abstract class AbstractMutableContainer<S extends Surface> extends ContainerSurface {




//    @Override
//    protected void stopping() {
//        clear();
//    }


    public boolean attachChild(S s) {
        return false;  //by default dont support external addAt
    }

    public boolean detachChild(S s) {
        return false; //by default dont support external removal
    }


    public final boolean remove(S s) {
        return detachChild(s) && s.stop();
    }

    public final void addAll(Surface... s) {
        add(s);
    }

    public abstract void add(Surface... s);


    protected abstract AbstractMutableContainer<S> clear();
}
