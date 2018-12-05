package spacegraph.util.geo.osm;

import java.util.Map;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmNode extends OsmElement {
    public final GeoVec3 pos;

    public OsmNode(long id, GeoVec3 pos, Map<String, String> tags) {
        super(id, null, tags);
        this.pos = pos;
    }

    


}
