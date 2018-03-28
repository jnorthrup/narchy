package spacegraph.space2d.phys.fracture.poly2Tri.splayTree;

/**
 * If you want to traverse the tree and do something with
 * it's nodes...
 */
public interface SplayTreeAction {

    void action(BTreeNode node, double y);

}
