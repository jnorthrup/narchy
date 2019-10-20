package spacegraph.util;

import jcog.data.map.MRUMap;
import spacegraph.space3d.SpaceGraph3D;
import spacegraph.space3d.Spatial;

import java.util.Map;
import java.util.function.Function;

class SpatialCache<X, Y extends Spatial<X>> {

    private final MRUMap<X, Y> cache;
    private final SpaceGraph3D<X> space;

    public SpatialCache(SpaceGraph3D<X> space, int capacity) {
        this.space = space;
        cache = new MRUMap<>(capacity) {
            @Override
            protected void onEvict(Map.Entry<X, Y> entry) {
                space.remove(entry.getValue());
            }
        };















    }

    public Y getOrAdd(X x, Function<X, Y> materializer) {
        var y = cache.computeIfAbsent(x, materializer);
        y.activate();
        return y;
    }

    public Y get(Object x) {
        Spatial y = cache.get(x);
        if (y != null)
            y.activate();
        return (Y) y;
    }


    public void remove(X x) {
        var y = cache.remove(x);
        if (y != null) {
            space.remove(y);
        }
    }

    public void clear() {
        cache.clear();
    }
}
