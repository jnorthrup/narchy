package spacegraph.space2d.container;

import jcog.data.list.FasterList;
import jcog.data.pool.DequePool;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;

public abstract class DynamicLayout2D<X, M extends MovingRectFloat2D> implements Graph2D.Graph2DLayout<X> {
    protected final FasterList<M> nodes = new FasterList();
    private final DequePool<M> nodesPool = new DequePool<>(128) {
        @Override
        public M create() {
            return newContainer();
        }

        @Override
        public void take(M i) {
            i.clear();
            super.take(i);
        }
    };

    abstract protected M newContainer();

    @Override
    public void layout(Graph2D<X> g, int dtMS) {
        if (!get(g))
            return;

        layout(g);

        put();
    }

    protected abstract void layout(Graph2D<X> g);

    private boolean get(Graph2D<X> g) {
        g.forEachValue(v -> {
            if (v.visible() && !v.pinned()) {
                M m = nodesPool.get();
                m.set(v);
                nodes.add(m);
            }
        });

        return !nodes.isEmpty();
    }

    private void put() {
        nodes.forEach(m -> put(m, m.node));

        nodesPool.take(nodes);
    }

    /** apply to node after layout
     * default impl: copy directly
     * */
    protected void put(M mover, Graph2D.NodeVis node) {
        node.posX0Y0WH(mover.x, mover.y, mover.w, mover.h);
    }

}
