package jcog.tree.rtree;

@FunctionalInterface public interface Split<X> {
    Node<X> split(X x, Leaf<X> leaf, Spatialization<X> model);

    /** used by linear and quadratic splits */
    default Node<X> newBranch(X x, Leaf<X> leaf, Spatialization<X> model, short size, int r1Max, int r2Max, X[] data) {
        final Leaf<X> l1Node = model.newLeaf();
        final Leaf<X> l2Node = model.newLeaf();

        boolean[] dummy = new boolean[1];
        l1Node.add(data[r1Max], true, model, dummy);

        dummy[0] = false;
        l2Node.add(data[r2Max], true, model, dummy);

        for (int i = 0; i < size; i++) {
            if ((i != r1Max) && (i != r2Max))
                leaf.transfer(l1Node, l2Node, data[i], model);
        }

        leaf.transfer(l1Node, l2Node, x, model);

        return model.newBranch(l1Node, l2Node);
    }
}
