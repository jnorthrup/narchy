package spacegraph.geo.data;

import com.jogamp.opengl.GL2;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by unkei on 2017/04/25.
 */
public class OsmWay extends OsmElement {




    public OsmWay(String id, List<OsmNode> children, Map<String, String> tags) {
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
            if (node != null && node.id != null) {
                return node.id.equals(this.children.get(this.children.size() - 1).id);
            }
        }
        return false;
    }

    public boolean isClosed() {
        // TODO: consider the edge case, where way is attached to the edges and it is closed with the edges.
        int s;
        List<? extends OsmElement> c = this.children;
        if (c != null && (s = c.size()) > 3) {
            OsmElement first = c.get(0);
            OsmElement last = c.get(s - 1);
            if (first != null && last != null) {
                String firstId = first.id;
                String lastId = last.id;
                return firstId != null && lastId != null && firstId.equals(lastId);
            }
        }
        return false;
    }

    public static OsmWay getOsmWayById(List<OsmWay> ways, String id) {
        return (OsmWay) elemById(ways, id);
    }
}
