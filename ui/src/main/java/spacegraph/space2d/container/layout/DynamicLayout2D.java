package spacegraph.space2d.container.layout;

import jcog.data.list.FasterList;
import jcog.data.pool.MetalPool;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.util.MutableRectFloat;

public abstract class DynamicLayout2D<X> implements Graph2D.Graph2DUpdater<X> {

    protected final FasterList<MutableRectFloat<X>> nodes = new FasterList<>();

    private final MetalPool<MutableRectFloat<X>> nodesPool = new MetalPool<>() {

        @Override
        public MutableRectFloat<X> create() {
            return newContainer();
        }

        @Override
        public void put(MutableRectFloat<X> i) {
            i.clear();
            super.put(i);
        }
    };

    protected MutableRectFloat<X> newContainer() {
        return new MutableRectFloat<X>();
    }

    @Override
    public void update(Graph2D<X> g, float dtS) {
        if (!get(g))
            return;

        layout(g, dtS);

        put();
    }

    protected abstract void layout(Graph2D<X> g, float dtS);

    private boolean get(Graph2D<X> g) {
        g.forEachValue(v -> {
            if (v.visible() && !v.pinned()) {
                MutableRectFloat<X> m = nodesPool.get();
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
    protected void put(MutableRectFloat<X> mover, Graph2D.NodeVis node) {
        node.posXYWH(mover.x, mover.y, mover.w, mover.h);
    }

}
