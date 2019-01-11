package spacegraph.util.geo.osm;

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.rect.HyperRectFloat;

import java.util.List;
import java.util.Map;

abstract public class OsmGroup extends OsmElement {
    //    public boolean isMultipolygon;

    protected List<OsmElement> children;

    transient private HyperRegion bounds = null;

    public OsmGroup(long id, Map<String, String> tags) {
        super(id, tags);
    }

    public void invalidate() {
        bounds = null;
    }

    protected void validate() {
        HyperRegion bounds;
        int n;
        if (children == null)
            n = 0;
        else
            n = children.size();

        switch (n) {
            case 0: bounds = HyperRectFloat.unbounded3; break;
            case 1:
                bounds = children.get(0);
                break;
            default:
                bounds = children.get(0);
                for (int i = 1; i < n; i++) {
                    bounds = bounds.mbr(children.get(i));
                }
                break;
        }
        this.bounds = bounds;
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        HyperRegion bounds = this.bounds;
        if (bounds == null)
            validate();
        return bounds.mbr(r);
    }

    @Override
    public double coord(int dimension, boolean maxOrMin) {
        HyperRegion b = bounds();

        return b.coord(dimension, maxOrMin);
    }

    public HyperRegion bounds() {
        HyperRegion b = bounds;
        if (b == null) {
            validate();
            b = bounds;
        }
        return b;
    }

}
