package spacegraph.geo.data;

import org.eclipse.collections.api.map.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmRelation extends OsmElement {

    public boolean isMultipolygon;

    public OsmRelation(String id, List<? extends OsmElement> children, Map<String, String> tags) {
        super(id, children, tags);
    }

    public void addChildren(List<? extends OsmElement> children) {
        this.children = children;
    }

    public static OsmRelation getOsmRelationById(List<OsmRelation> relations, String id) {
        return (OsmRelation) elemById(relations, id);
    }
}
