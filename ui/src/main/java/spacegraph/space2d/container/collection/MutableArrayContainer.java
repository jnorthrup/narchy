package spacegraph.space2d.container.collection;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AbstractMutableContainer;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MutableArrayContainer<S extends Surface> extends AbstractMutableContainer {
    protected final AtomicReferenceArray<S> children;
    public final int length;

    public MutableArrayContainer(int size) {
        this.children = new AtomicReferenceArray(size);
        this.length = size;
    }

    @Override
    protected void clear() {
        for (int i= 0; i < length; i++)
            children.set(i, null);
    }


    @Override
    protected void doLayout(int dtMS) {

    }

    @Override
    protected int childrenCount() {
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (children.get(i)!=null)
                count++;
        }
        return count;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        for (int i = 0; i < length; i++) {
            S ii = children.get(i);
            if (ii !=null)
                o.accept(ii);
        }
    }

    @Override
    protected boolean whileEach(Predicate<Surface> o) {
        for (int i = 0; i < length; i++) {
            S ii = children.get(i);
            if (ii !=null)
                if (!o.test(ii))
                    return false;
        }
        return true;
    }

    @Override
    protected boolean whileEachReverse(Predicate<Surface> o) {
        for (int i = length - 1; i >= 0; i--) {
            S ii = children.get(i);
            if (ii !=null)
                if (!o.test(ii))
                    return false;
        }
        return true;
    }

}
