package spacegraph.space2d.container.collection;

import spacegraph.space2d.Surface;
import spacegraph.space2d.Surfacelike;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** TODO support resizing */
abstract public class MutableArrayContainer<S extends Surface> extends AbstractMutableContainer<S> {

    /** TODO varhandle */
    protected final AtomicReferenceArray<S> children;
    public final int length;

    public MutableArrayContainer(int size) {
        this.children = new AtomicReferenceArray(size);
        this.length = size;
    }
    public MutableArrayContainer(S... items) {
        this(items.length);
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            S s = items[i];
            if (s!=null)
                setAt(i, s);
        }
    }

    public S get(int s) {
        return children.getOpaque(s);
    }

    public final S remove(int index) {
        return setAt(index, null);
    }




    /** put semantics */
    public final S setAt(int index, S s) {
        if (s != setAt(index, s, true)) {
            layout();
        } return s;
    }

    /** returns the removed element */
    private S setAt(int index, S ss, boolean restart) {
        return restart ?
            children.getAndAccumulate(index, ss, this::updateRestart) :
            children.getAndSet(index, ss);
    }


    private <S extends Surface> S updateRestart(S r, S s) {
        if (r != s) {
                if (r != null) {
                    //r.stop();
                    r.delete();
                }

                if (s != null) {
                    Surfacelike sParent = s.parent;
                    assert (sParent == null || sParent == this): this + " confused that " + s + " already has parent " + sParent;


//                        synchronized (this) {
                    if (sParent == null && this.parent != null) {
                        s.start(this);
                    }
                    //otherwise it is started, or this isnt started
//                        }

                }


        }

        return s;
    }

    @Override
    protected TextEdit clear() {
        for (int i= 0; i < length; i++)
            setAt(i, null);
        return null;
    }


    @Override
    protected int childrenCount() {
        int count = 0;
        int l = this.length;
        for (int i = 0; i < l; i++) {
            if (children.getOpaque(i)!=null)
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
    public boolean whileEach(Predicate<Surface> o) {
        for (int i = 0; i < length; i++) {
            S ii = children.get(i);
            if (ii !=null)
                if (!o.test(ii))
                    return false;
        }
        return true;
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        for (int i = length - 1; i >= 0; i--) {
            S ii = children.get(i);
            if (ii !=null)
                if (!o.test(ii))
                    return false;
        }
        return true;
    }
    @Override
    public void add(Surface... s) {
        throw new UnsupportedOperationException();
    }
}
