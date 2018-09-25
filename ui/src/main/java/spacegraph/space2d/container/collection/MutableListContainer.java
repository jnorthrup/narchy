package spacegraph.space2d.container.collection;

import com.google.common.collect.Sets;
import jcog.data.list.FastCoWList;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.AbstractMutableContainer;
import spacegraph.space2d.container.Container;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class MutableListContainer extends AbstractMutableContainer {


    private final static Surface[] EMPTY_SURFACE_ARRAY = new Surface[0];
    private static final IntFunction<Surface[]> NEW_SURFACE_ARRAY = (i) -> i == 0 ? EMPTY_SURFACE_ARRAY : new Surface[i];
    private final FastCoWList<Surface> children;

    public MutableListContainer(Surface... children) {
        super();
        this.children = new FastCoWList(NEW_SURFACE_ARRAY) {
            @Override
            public void commit() {
                super.commit();
                layout();
            }
        };

        if (children.length > 0)
            set(children);
    }

    public Surface[] children() {
        return children.array();
    }

    @Override
    public int childrenCount() {
        return children.size();
    }


    protected Surface get(int index) {
        return children.get(index);
    }

//    @Nullable
//    public Surface getSafe(int index) {
//        @Nullable Surface[] c = children.copy;
//        if (index >= 0 && index < c.length)
//            return c[index];
//        else
//            return null;
//    }

    /**
     * returns the existing value that was replaced
     */
    protected Surface set(int index, Surface next) {
        synchronized (this) {
            SurfaceBase p = this.parent;
            if (p == null) {
                return children.set(index, next);
            } else {
                if (children.size() - 1 < index)
                    throw new RuntimeException("index out of bounds");

                Surface existing = children.set(index, next);
                if (existing != next) {
                    if (existing != null)
                        existing.stop();

                    next.start(this);


                }
                return existing;
            }
        }
    }


    public final void addAll(Surface... s) {
        add(s);
    }

    public void add(Surface... s) {

        if (s.length == 0) return;

        synchronized (this) {
            for (Surface x : s) {
                if (x != null) {
                    children.add(x);
                }
            }
            //children.commit();

            if (parent!=null) {
                for (Surface x : s) {
                    if (x != null) {
                        x.start(this);
                    }
                }
            } //else: wait until attached
        }



    }

    public boolean removeChild(Surface s) {
        boolean removed = children.removeFirstInstance(s);
        if (removed) {
            if (s.stop()) {
//                if (!(children instanceof BufferedCoWList))
                return true;
            }
        }
        return false;
    }

    public final Container set(Surface... next) {

        synchronized (this) {

            if (parent == null) {
                children.set(next);
                return this;
            } else {

                int numExisting = size();
                if (numExisting == 0) {


                    addAll(next);

                } else if (next.length == 0) {

                    clear();

                } else {


                    Surface[] cc = children.array();
                    Sets.SetView unchanged = Sets.intersection(
                            Set.of(cc), Set.of(next)
                    );
                    if (unchanged.isEmpty()) unchanged = null;

                    for (Surface existing : cc) {
                        if (unchanged == null || !unchanged.contains(existing))
                            removeChild(existing);
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
        set(next.toArray(Surface.EmptySurfaceArray));
        return this;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        children.forEach((c) -> {
            if (c != null) {
                SurfaceBase cp = c.parent; assert (cp == null || cp == MutableListContainer.this) : c + " has parent " + cp + " when trying to add to " + MutableListContainer.this;
                o.accept(c);
            }
        });
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return children.whileEach(o);
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return children.whileEachReverse(o);
    }

    protected int size() {
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
                }
            }
        }
    }

    /**
     * this can be accelerated by storing children as a Map
     */
    public void replace(Surface child, Surface replacement) {
        if (!removeChild(child))
            throw new RuntimeException("could not replace missing " + child + " with " + replacement);

        add(replacement);
    }

    @Override
    protected void doLayout(int dtMS) {
        //forEach(x -> x.layout());
    }
}
