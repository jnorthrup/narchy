package spacegraph.space2d.container.layout;

import jcog.data.list.FasterList;
import jcog.data.pool.MetalPool;
import org.eclipse.collections.api.block.procedure.Procedure;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.graph.NodeVis;
import spacegraph.util.MutableRectFloat;

import java.util.function.Consumer;

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
        return new MutableRectFloat<>();
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
        g.forEachValue(new Consumer<NodeVis<X>>() {
            @Override
            public void accept(NodeVis<X> v) {
                if (v.visible() && !NodeVis.pinned()) {
                    MutableRectFloat<X> m = nodesPool.get();
                    m.set(v);
                    nodes.add(m);
                }
            }
        });

        return !nodes.isEmpty();
    }

    private void put() {
        nodes.forEach(new Procedure<MutableRectFloat<X>>() {
            @Override
            public void value(MutableRectFloat<X> m) {
                DynamicLayout2D.this.put(m, m.node);
            }
        });

        nodesPool.steal(nodes);
    }

    /** apply to node after layout
     * default impl: copy directly
     * */
    protected void put(MutableRectFloat<X> mover, NodeVis node) {
        node.posXYWH(mover.x, mover.y, mover.w, mover.h);
    }

}
