package spacegraph.util.geo.osm;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by unkei on 2017/04/26.
 */
public class OsmElement {
    public final long id;

    //CompactStringObjectMap ?
    public Map<String, String> tags;

    /** TODO make immutable */
    List<? extends OsmElement> children;


    OsmElement(long id, List<? extends OsmElement> children, Map<String, String> tags) {
        this.id = id;

        this.children = children;
        if (tags == null || tags.isEmpty())
            tags = Collections.emptyMap();
        this.tags = tags;
    }

//    public List<? extends OsmElement> children()
//        return this.children;
//    }

    public void forEach(Consumer<OsmElement> eachChild) {
        //this class impl no children
    }

    public void tag(String k, String v) {
        if (tags.isEmpty()) {
            tags = new UnifiedMap(1);
        }
        tags.put(k, v);
    }
}
