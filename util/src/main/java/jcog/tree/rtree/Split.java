package jcog.tree.rtree;

@FunctionalInterface public interface Split<X> {
    Node<X> split(X x, Leaf<X> leaf, Spatialization<X> model);

    /** used by linear and quadratic splits */
    default Node<X> newBranch(X x, Leaf<X> leaf, Spatialization<X> model, short size, int r1Max, int r2Max, X[] data) {
        final Leaf<X> l1Node = model.newLeaf();
        final Leaf<X> l2Node = model.newLeaf();

        l1Node.insert(data[r1Max], model);
        l2Node.insert(data[r2Max], model);

        for (int i = 0; i < size; i++) {
            if ((i != r1Max) && (i != r2Max))
                leaf.transfer(l1Node, l2Node, data[i], model);
        }

        leaf.transfer(l1Node, l2Node, x, model);

        return model.newBranch(l1Node, l2Node);
    }
}
