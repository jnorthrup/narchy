package spacegraph.space2d.container.collection;

import jcog.data.atomic.MetalAtomicReferenceArray;
import spacegraph.space2d.Surface;
import spacegraph.space2d.Surfacelike;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** TODO support resizing */
public abstract class MutableArrayContainer<S extends Surface> extends AbstractMutableContainer<S> {

    /** TODO varhandle */
    protected final MetalAtomicReferenceArray<S> children;
    public final int length;

    public MutableArrayContainer(int size) {
        this.children = new MetalAtomicReferenceArray(size);
        this.length = size;
    }
    @SafeVarargs
    public MutableArrayContainer(S... items) {
        this(items.length);
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            S s = items[i];
            if (s!=null)
                setAt(i, s);
        }
    }

    public final S get(int s) {
        return children.getFast(s);
    }

    public final S remove(int index) {
        return setAt(index, null);
    }




    /** put semantics */
    public final S setAt(int index, S s) {
        if (s != setAt(index, s, true))
            layout();
        return s;
    }

    /** returns the removed element */
    private S setAt(int index, S ss, boolean restart) {
        return restart ?
            children.getAndAccumulate(index, ss, this::updateRestart) :
            children.getAndSet(index, ss);
    }


    private S updateRestart(S r, S s) {
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
        for (int i = 0; i < length; i++)
            setAt(i, null);
        return null;
    }


    @Override
    public int childrenCount() {
        int l = this.length;
        long result = 0L;
        for (int i = 0; i < l; i++) {
            if (children.getFast(i) != null) {
                result++;
            }
        }
        int count = (int) result;
        return count;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        for (int i = 0; i < length; i++) {
            S ii = children.getFast(i);
            if (ii !=null) o.accept(ii);
        }
    }

    @Override
    public <X> void forEachWith(BiConsumer<Surface, X> o, X x) {
        for (int i = 0; i < length; i++) {
            S ii = children.getFast(i);
            if (ii !=null) o.accept(ii, x);
        }
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        int bound = length;
        for (int i = 0; i < bound; i++) {
            S fast = children.getFast(i);
            if (fast != null) {
                if (!o.test(fast)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return IntStream.iterate(length - 1, i -> i >= 0, i -> i - 1).mapToObj(children::getFast).filter(Objects::nonNull).allMatch(o);
    }
    @Override
    public void add(Surface... s) {
        throw new UnsupportedOperationException();
    }
}
