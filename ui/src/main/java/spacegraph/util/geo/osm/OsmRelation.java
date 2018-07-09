package spacegraph.util.geo.osm;

import java.util.List;
import java.util.Map;

/**
 * Created by unkei on 2017/04/26.
 */
class OsmRelation extends OsmElement {

    public boolean isMultipolygon;

    public OsmRelation(long id, List<? extends OsmElement> children, Map<String, String> tags) {
        super(id, children, tags);
    }

    public void addChildren(List<? extends OsmElement> children) {
        this.children = children;
    }




}
