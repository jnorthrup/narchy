package spacegraph.geo.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmElement {
    public final String id;

    public final Map<String, String> tags;

    /** TODO make immutable */
    public List<? extends OsmElement> children;


    public OsmElement(String id, List<? extends OsmElement> children, Map<String, String> tags) {
        this.id = id;
        this.children = children;
        this.tags = tags;
    }

    public List<? extends OsmElement> children() {
        return this.children;
    }

    public static OsmElement elemById(List<? extends OsmElement> osmElemnts, String id) {
        if (id != null && !id.isEmpty()) {
            for (int i = 0, osmElemntsSize = osmElemnts.size(); i < osmElemntsSize; i++) {
                OsmElement osmElement = osmElemnts.get(i);
                if (id.equals(osmElement.id)) {
                    return osmElement;
                }
            }
        }
        return null;
    }
}
