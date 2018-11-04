package spacegraph.space2d.container.layout;

import jcog.data.list.FasterList;
import jcog.data.pool.MetalPool;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.util.MutableFloatRect;

public abstract class DynamicLayout2D<X, M extends MutableFloatRect> implements Graph2D.Graph2DUpdater<X> {
    protected final FasterList<M> nodes = new FasterList();
    private final MetalPool<M> nodesPool = new MetalPool<>() {
        @Override
        public M create() {
            return newContainer();
        }

        @Override
        public void put(M i) {
            i.clear();
            super.put(i);
        }
    };

    abstract protected M newContainer();

    @Override
    public void update(Graph2D<X> g, int dtMS) {
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

        nodesPool.steal(nodes);
    }

    /** apply to node after layout
     * default impl: copy directly
     * */
    protected void put(M mover, Graph2D.NodeVis node) {
        node.posxyWH(mover.x, mover.y, mover.w, mover.h);
    }

}
