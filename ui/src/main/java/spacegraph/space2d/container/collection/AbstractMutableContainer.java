package spacegraph.space2d.container.collection;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;

public abstract class AbstractMutableContainer extends Container {

    @Override
    protected void starting() {
        //synchronized (this) {
            forEach(c -> c.start(this));
        //}
        layout();
    }


//    @Override
//    protected void stopping() {
//        clear();
//    }


    public boolean attachChild(Surface s) {
        return false;  //by default dont support external add
    }

    public boolean detachChild(Surface s) {
        return false; //by default dont support external removal
    }

    public final boolean remove(Surface s) {
        boolean removed = detachChild(s);
        if (removed) {
            if (s.stop()) {
                clear();
                return true;
            }
        }
        return false;
    }

    protected abstract void clear();
}
