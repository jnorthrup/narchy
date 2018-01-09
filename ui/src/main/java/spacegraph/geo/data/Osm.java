package spacegraph.geo.data;

import jcog.list.FasterList;

import java.util.List;

/**
 * Created by unkei on 2017/04/25.
 */
public class Osm {
    public OsmBounds bounds;
    public final List<OsmNode> nodes;
    public final List<OsmWay> ways;
    public final List<OsmRelation> relations;

    public Osm() {
        nodes = new FasterList<>(128*1024);
        ways = new FasterList<>(32*1024);
        relations = new FasterList<>(32*1024);
    }

    public void clear() {
        nodes.clear();
        ways.clear();
        relations.clear();
    }
}
