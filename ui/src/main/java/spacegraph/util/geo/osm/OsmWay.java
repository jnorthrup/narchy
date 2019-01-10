package spacegraph.util.geo.osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmWay extends OsmElement {




    public OsmWay(long id, List<OsmNode> children, Map<String, String> tags) {
        super(id, children, tags);
    }

    public List<OsmNode> getOsmNodes() {
        return (List<OsmNode>)this.children;
    }

    public void addOsmWay(OsmWay way) {

        if (way == null || way.children == null) return;

        List<? extends OsmElement> newChildren = way.children;
        if (this.children == null) {
            this.children = newChildren;
        } else {
            List<OsmElement> combinedNodes = new ArrayList<>(this.children);
            combinedNodes.addAll(newChildren);
            this.children = combinedNodes;
        }
    }

    public boolean isFollowedBy(OsmWay way) {
        if (this.children != null && way != null && way.children != null &&
                !this.children.isEmpty() && !way.children.isEmpty()) {
            OsmNode node = (OsmNode)way.children.get(0);
            if (node != null) {
                return node.id == this.children.get(this.children.size() - 1).id;
            }
        }
        return false;
    }

    public boolean isLoop() {
        
        int s;
        List<? extends OsmElement> c = this.children;
        if (c != null && (s = c.size()) > 3) {
            OsmElement first = c.get(0), last = c.get(s - 1);
            if (first != null && last != null) {
                return first.id == last.id;
            }
        }
        return false;
    }




}
