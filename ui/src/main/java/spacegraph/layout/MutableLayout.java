package spacegraph.layout;

import jcog.list.FastCoWList;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class MutableLayout extends Layout {


    public final FastCoWList<Surface> children = new Children(0);


    public MutableLayout(Surface... children) {
        super();
        children(children);
    }

    public MutableLayout(List<Surface> children) {
        children(children);
    }

    @Override
    public int childrenCount() {
        return children.size();
    }

    @Override
    public void start(@Nullable Surface parent) {
        synchronized (children) {
            super.start(parent);
            children.forEach(c -> {
                if (c.parent != this)
                    c.start(this);
            });
        }
    }

    @Override
    public void doLayout(int dtMS) {
        children.forEach(Surface::layout);
    }

    public Layout children(Surface... next) {
        synchronized (children) {
            if (!equals(this.children, next)) {
                children.clear();
                for (Surface c : next) {
                    if (c != null)
                        children.add(c);
                }
                layout();
            }
        }
        return this;
    }

    public Layout children(List<Surface> next) {
        synchronized (children) {
            if (!equals(this.children, next)) {
                children.clear();
                children.addAll(next);
                layout();
            }
        }
        return this;
    }

    @Override
    public synchronized void stop() {
        synchronized (children) {
            super.stop();
            children.clear();
        }
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        children.forEach(o);
    }


    final static Surface[] EMPTY_SURFACE_ARRAY = new Surface[0];

    static final IntFunction<Surface[]> NEW_SURFACE_ARRAY = (i) -> {
        return i == 0 ? EMPTY_SURFACE_ARRAY : new Surface[i];
    };

    private class Children extends FastCoWList<Surface> {

        public Children(int capacity) {
            super(capacity, NEW_SURFACE_ARRAY);
        }

        @Override
        public boolean add(Surface surface) {
            synchronized (children) {
                if (!super.add(surface)) {
                    return false;
                }
                if (parent != null) {
                    surface.start(MutableLayout.this);
                    layout();
                }
            }
            return true;
        }

        @Override
        public Surface set(int index, Surface neww) {
            Surface old;
            synchronized (children) {
                while (size() <= index) {
                    add(null);
                }
                old = super.set(index, neww);
                if (old == neww)
                    return neww;
                else {
                    if (old != null) {
                        old.stop();
                    }
                    if (neww != null && parent != null) {
                        neww.start(MutableLayout.this);
                    }
                }
            }
            layout();
            return old;
        }

        @Override
        public boolean addAll(Collection<? extends Surface> c) {
            synchronized (children) {
                for (Surface s : c)
                    add(s);
            }
            layout();
            return true;
        }

        @Override
        public Surface remove(int index) {
            Surface x;
            synchronized (children) {
                x = super.remove(index);
                if (x == null)
                    return null;
                x.stop();
            }
            layout();
            return x;
        }

        @Override
        public boolean remove(Object o) {
            synchronized (children) {
                if (!super.remove(o))
                    return false;
                ((Surface) o).stop();
            }
            layout();
            return true;
        }


        @Override
        public void add(int index, Surface element) {
            synchronized (children) {
                super.add(index, element);
                element.start(MutableLayout.this);
            }
            layout();
        }

        @Override
        public void clear() {
            synchronized (children) {
                this.removeIf(x -> {
                    x.stop();
                    return true;
                });
            }
            layout();
        }
    }

}
