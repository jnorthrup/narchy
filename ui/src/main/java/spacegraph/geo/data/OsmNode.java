package spacegraph.geo.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmNode extends OsmElement {
    final GeoCoordinate geoCoordinate;

    public OsmNode(String id, GeoCoordinate geoCoordinate, Map<String, String> tags) {
        super(id, null, tags);
        this.geoCoordinate = geoCoordinate;
    }

    public GeoCoordinate getGeoCoordinate() {
        return this.geoCoordinate;
    }

    public static OsmNode getOsmNodeById(List<OsmNode> nodes, String id) {
        return (OsmNode) elemById(nodes, id);
    }
}
