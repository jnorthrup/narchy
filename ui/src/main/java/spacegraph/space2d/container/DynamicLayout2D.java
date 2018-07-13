package spacegraph.space2d.container;

import jcog.data.pool.DequePool;
import jcog.data.list.FasterList;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;

import java.util.List;

public abstract class DynamicLayout2D<X, M extends MovingRectFloat2D> implements Graph2D.Graph2DLayout<X> {
    final List<Graph2D.NodeVis<X>> nodes = new FasterList();
    protected final List<M> bounds = new FasterList();
    private final DequePool<M> boundsPool = new DequePool<>(128) {
        @Override
        public M create() {
            return newContainer();
        }
    };


    abstract protected M newContainer();

    float recenterX;
    float recenterY;
    float tx;
    float ty;

    /**
     * override this to control the final position the layout specifies
     */
    private void apply(Graph2D.NodeVis<X> n, RectFloat2D target) {
        n.pos(target);
    }

    @Override
    public void layout(Graph2D<X> g, int dtMS) {

        if (!get(g))
            return;

        layoutDynamic(g);

        put();
    }

    protected abstract void layoutDynamic(Graph2D<X> g);

    private boolean get(Graph2D<X> g) {
        nodes.clear();

        float ox = g.bounds.x;
        float oy = g.bounds.y;
        recenterX = ox + g.bounds.w / 2;
        recenterY = oy + g.bounds.h / 2;
        tx = ty = 0;
        
        

        g.forEachValue(v -> {
            if (v.visible() && !v.pinned()) {
                nodes.add(v);
                M m = boundsPool.get();
                m.set(v.bounds);
                m.move(-recenterX, -recenterY, 1f); 
                bounds.add(m);
            }
        });
        int n = nodes.size();
        return n != 0;
    }

    private void put() {
        int n = bounds.size();
        for (int i = 0; i < n; i++) {
            apply(nodes.get(i), bounds.get(i).get(tx, ty));
        }

        boundsPool.take(bounds);
    }
}
