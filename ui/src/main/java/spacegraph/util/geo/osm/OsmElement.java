package spacegraph.util.geo.osm;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmElement {
    public final long id;

    //CompactStringObjectMap ?
    public Map<String, String> tags;



    OsmElement(long id, Map<String, String> tags) {
        this.id = id;

        if (tags == null || tags.isEmpty())
            tags = Collections.emptyMap();
        this.tags = tags;
    }

    public void forEach(Consumer<OsmElement> eachChild) {
        //leaf
    }

    public void tag(String k, String v) {
        if (tags.isEmpty()) {
            tags = new UnifiedMap(1);
        }
        tags.put(k, v);
    }
}
