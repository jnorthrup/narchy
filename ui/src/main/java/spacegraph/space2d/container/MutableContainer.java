package spacegraph.space2d.container;

import com.google.common.collect.Sets;
import jcog.list.BufferedCoWList;
import jcog.list.FastCoWList;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class MutableContainer extends Container {


    final static Surface[] EMPTY_SURFACE_ARRAY = new Surface[0];
    static final IntFunction<Surface[]> NEW_SURFACE_ARRAY = (i) -> {
        return i == 0 ? EMPTY_SURFACE_ARRAY : new Surface[i];
    };
    private final FastCoWList<Surface> children;



    public MutableContainer(Surface... children) {
        this(false, children);
    }

    public MutableContainer(boolean buffered, Surface... children) {
        this(buffered ? new BufferedCoWList(children.length + 1, NEW_SURFACE_ARRAY)
                :
                new FastCoWList(children.length + 1, NEW_SURFACE_ARRAY),
            children
        );
    }
    public MutableContainer(FastCoWList<Surface> childrenModel, Surface... children) {
        super();

        this.children = childrenModel;
        if (children.length > 0)
            set(children);
    }

    public Surface[] children() {
        return children.copy;
    }

    @Override
    public int childrenCount() {
        return children.size();
    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {
            synchronized (this) {
                //add pre-added
                children.forEach(c -> {
                    assert (c.parent == null): c + " has parent " + c.parent + " when trying to add to " + MutableContainer.this;
                    c.start(this);
                });
            }
            layout();
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

    @Override
    protected void doLayout(int dtMS) {

        if (children instanceof BufferedCoWList)
            ((BufferedCoWList)children).commit();

        children.forEach(Surface::layout);
    }

    public Surface get(int index) {
        return children.copy[index];
    }

    @Nullable
    public Surface getSafe(int index) {
        @Nullable Surface[] c = children.copy;
        if (index >= 0 && index < c.length)
            return c[index];
        else
            return null;
    }

    /**
     * returns the existing value that was replaced
     */
    public Surface set(int index, Surface next) {
        Surface existing;
        synchronized (this) {
            if (parent == null) {
                return children.set(index, next);
            } else {
                if (children.size() - 1 < index)
                    throw new RuntimeException("index out of bounds");

                existing = get(index);
                if (existing != next) {
                    existing.stop();

                    children.set(index, next);

                    if (this.parent != null)
                        next.start(this);
                }
            }
        }
        layout();
        return existing;
    }

    //TODO: addIfNotPresent(x) that tests for existence first

    public void addAll(Surface... s) {
        if (s.length == 0) return;
        
        for (Surface x : s)
            _add(x);
        layout();
    }

    public void add(Surface s) {
        _add(s);
        layout();
    }

    private void _add(Surface s) {
        synchronized (this) {
            if (parent == null) {
                children.add(s);
            } else {
                if (s.start(this)) {
                    children.add(s); //assume it was added to the list
                }
            }
        }
    }

    public boolean remove(Surface s) {
        synchronized (this) {
            if (parent == null) {
                return children.remove(s);
            } else {
                if (children.remove(s)) {
                    assert (s.parent == this);
                    s.stop();
                    layout();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public final Container set(Surface... next) {

        synchronized (this) {

            if (parent == null) {
                children.set(next);
                return this;
            } else {

                int numExisting = size();
                if (numExisting == 0) {

                    //currently empty, just add all
                    addAll(next);

                } else if (next.length == 0) {

                    clear();

                } else {
                    //possibly some remaining, so use Set intersection to invoke start/stop only as necessary

                    Sets.SetView unchanged = Sets.intersection(
                            Set.of(children.copy), Set.of(next)
                    );
                    if (unchanged.isEmpty()) unchanged = null;

                    for (Surface existing : children.copy) {
                        if (unchanged == null || !unchanged.contains(existing))
                            remove(existing);
                    }

                    for (Surface n : next) {
                        if (unchanged == null || !unchanged.contains(n))
                            add(n);
                    }

                }
            }

        }

        return this;
    }

    public final Container set(List<? extends Surface> next) {
        set(next.toArray(new Surface[0]));
        return this;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        for (Surface x : children.copy) {
            o.accept(x);
        }
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        for (Surface x : children.copy) {
            if (!o.test(x))
                return false;
        }
        return true;
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        @Nullable Surface[] copy = children.copy;
        for (int i = copy.length - 1; i >= 0; i--) {
            Surface x = copy[i];
            if (!o.test(x))
                return false;
        }
        return true;
    }

    public int size() {
        return children.size();
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public void clear() {
        synchronized (this) {
            if (parent == null) {
                children.clear();
            } else {
                if (!children.isEmpty()) {
                    children.forEach(Surface::stop);
                    children.clear();
                    layout();
                }
            }
        }
    }

    /**
     * this can be accelerated by storing children as a Map
     */
    public void replace(Surface child, Surface replacement) {
        synchronized (this) {
            if (!remove(child))
                throw new RuntimeException("could not replace missing " + child + " with " + replacement);

            add(replacement);
        }
    }

//    private class Children extends FastCoWList<Surface> {
//
//        public Children(int capacity) {
//            super(capacity, NEW_SURFACE_ARRAY);
//        }
//
//        @Override
//        public boolean add(Surface surface) {
//            synchronized (this) {
//                if (!super.add(surface)) {
//                    return false;
//                }
//                if (parent != null) {
//                    layout();
//                }
//            }
//            return true;
//        }
//
//        @Override
//        public Surface set(int index, Surface neww) {
//            Surface old;
//            synchronized (this) {
//                while (size() <= index) {
//                    add(null);
//                }
//                old = super.set(index, neww);
//                if (old == neww)
//                    return neww;
//                else {
//                    if (old != null) {
//                        old.stop();
//                    }
//                    if (neww != null && parent != null) {
//                        neww.start(MutableContainer.this);
//                    }
//                }
//            }
//            layout();
//            return old;
//        }
//
//        @Override
//        public boolean addAll(Collection<? extends Surface> c) {
//            synchronized (this) {
//                for (Surface s : c)
//                    add(s);
//            }
//            layout();
//            return true;
//        }
//
//        @Override
//        public Surface remove(int index) {
//            Surface x;
//            synchronized (this) {
//                x = super.remove(index);
//                if (x == null)
//                    return null;
//                x.stop();
//            }
//            layout();
//            return x;
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            synchronized (this) {
//                if (!super.remove(o))
//                    return false;
//                ((Surface) o).stop();
//            }
//            layout();
//            return true;
//        }
//
//
//        @Override
//        public void add(int index, Surface element) {
//            synchronized (this) {
//                super.add(index, element);
//            }
//            layout();
//        }
//
//        @Override
//        public void clear() {
//            synchronized (this) {
//                this.removeIf(x -> {
//                    x.stop();
//                    return true;
//                });
//            }
//            layout();
//        }
//    }

}
