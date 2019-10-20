package toxi.geom;

import jcog.tree.rtree.RTree;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import toxi.physics2d.VerletParticle2D;

import java.util.function.Consumer;

/** removal has bug */
public class RTreeQuadTree<X extends VerletParticle2D> extends RTree<X> implements SpatialIndex<X> {

    public RTreeQuadTree() {
        super(RTreeQuadTree::b, 3, Spatialization.DefaultSplits.AXIAL.get());
    }

    static RectFloat b(Vec2D v) {
        float DEFAULT_RADIUS = 0.5f;
        return b(v, DEFAULT_RADIUS);
    }

    private static RectFloat b(Vec2D v, float radius) {
        return RectFloat.XYWH(v.x, v.y, radius* 2.0F, radius* 2.0F);
    }


    @Override
    public boolean index(X p) {
        synchronized (this) {
            return add(p);
        }
    }

    @Override
    public boolean isIndexed(X item) {
        return contains(item);
    }

    @Override
    public void itemsWithinRadius(Vec2D p, float radius, Consumer<X> results) {
        synchronized (this) {
            intersectsWhile(b(p, radius), (t) -> {
                results.accept(t);
                return true;
            });
        }
    }

    @Override
    public boolean reindex(X p, Consumer<X> each) {
        synchronized (this) {
            if (unindex(p)) {
                each.accept(p);

                return index(p);

            }
            return false;
        }
    }

    @Override
    public boolean unindex(X p) {
        synchronized (this) {
            return remove(p);
        }
    }

}
