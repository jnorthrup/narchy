package spacegraph.util.geo.osm;

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.rect.HyperRectFloat;

import java.util.Map;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmNode extends OsmElement {
    public final GeoVec3 pos;

    public OsmNode(long id, GeoVec3 pos, Map<String, String> tags) {
        super(id, tags);
        this.pos = pos;
    }

    @Override
    public String toString() {
        return id + "@" + pos + (tags!=null ? tags.toString() : "");
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        return r.mbr(new HyperRectFloat(pos));
    }

    @Override
    public double coord(int dimension, boolean maxOrMin) {
        switch (dimension) {
            case 0: return (double) pos.x;
            case 1: return (double) pos.y;
            case 2: return (double) pos.z;
        }
        return Double.NaN;
    }
}
