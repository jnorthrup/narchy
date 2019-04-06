package spacegraph.space2d.container.collection;

import jcog.Util;
import jcog.data.list.FastCoWList;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import spacegraph.space2d.Surface;
import spacegraph.space2d.Surfacelike;
import spacegraph.space2d.container.ContainerSurface;
import spacegraph.space2d.widget.textedit.TextEdit;

import java.util.Collection;
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


    public Surface get(int index) {
        return children.get(index);
    }
    public Surface remove(int index) {
        return children.remove(index);
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
    protected synchronized Surface set(int index, Surface next) {
        Surfacelike p = this.parent;
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

    public void add(Surface... s) {

        if (s.length == 0) return;

        synchronized (this) {
            for (Surface x : s) {
                if (x != null) {
                    _add(x);
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

        layout();
    }

    protected void _add(Surface x) {
        children.add(x);
    }

    @Override public boolean attachChild(Surface s) {
        return children.add(s);
    }

    @Override public boolean detachChild(Surface s) {
        return children.removeFirstInstance(s);
    }

    public final ContainerSurface set(Surface... next) {

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


                    Surface[] ee = children.array();
                    if (ee!=next) {
                        IntSet pi = Util.intSet(x -> x.id, ee);
                        IntSet ni = Util.intSet(x -> x.id, next);
                        IntHashSet unchanged = new IntHashSet().withAll(pi.select(ni::contains));

//                    Sets.SetView unchanged = Sets.intersection(
//                            Set.of(cc), Set.of(next)
//                    );
                        if (unchanged.isEmpty()) unchanged = null;

                        for (Surface existing : ee) {
                            if (unchanged == null || !unchanged.contains(existing.id))
                                remove(existing);
                        }

                        for (Surface n : next) {
                            if (unchanged == null || !unchanged.contains(n.id))
                                add(n);
                        }
                    }

                }
            }

        }

        return this;
    }

    public final ContainerSurface set(Collection<? extends Surface> next) {
        //set(next.toArray(Surface.EmptySurfaceArray));
        children.set(next);

        return this;
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        children.forEachWith((c, oo) -> {
            if (c != null)
                oo.accept(c);
        }, o);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return children.whileEach(o);
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return children.whileEachReverse(o);
    }

    public int size() {
        return children.size();
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public TextEdit clear() {
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
        return null;
    }


    @Override
    protected void doLayout(float dtS) {
        //forEach(x -> x.layout());
    }
}
