package spacegraph.space2d.phys.fracture.poly2Tri.splayTree;

/**
 * Each object which want to be a part of the tree must
 * implement interface SplayTreeItem.
 */
public class BTreeNode {

    SplayTreeItem _data = null;
    BTreeNode _left = null;
    BTreeNode _right = null;
    private boolean _visited = false;

    public BTreeNode() {
    }

    public BTreeNode(SplayTreeItem data, BTreeNode left, BTreeNode right) {
        _data = data;
        _left = left;
        _right = right;
    }

    public SplayTreeItem data() {
        return _data;
    }

    public BTreeNode left() {
        return _left;
    }

    public BTreeNode right() {
        return _right;
    }

    void setVisited(boolean visited) {
        _visited = visited;
    }

    boolean getVisited() {
        return _visited;
    }

    Comparable keyValue() {
        return _data.keyValue();
    }

}
