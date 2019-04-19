package jcog.tree.rtree;

import org.jetbrains.annotations.Nullable;

public class RInsertion<X> {

    public final Spatialization<X> model;
    public final X x;

    final HyperRegion bounds;

    private boolean addOrMerge;


    private boolean added;

    /** TODO */
//    public enum State {
//        Scanning,
//        Added,
//        Merged
//    }

    /**
     * @param x      - value to add to index
     * @param parent - the callee which is the parent of this instance.
 *                  if parent is null, indicates it is in the 'merge attempt' stage
 *                  if parent is non-null, in the 'insertion attempt' stage
     * @param model
     * @param added
*              so when a callee Branch receives "bounds not changed"
*              it will know it doesnt need to update bounds
*
     */
    public RInsertion(X x, boolean addOrMerge, Spatialization<X> model) {
        this.x = x;
        this.bounds = model.bounds(x);
        this.addOrMerge = addOrMerge;
        this.model = model;
        this.added = false;
    }

    public boolean isAddOrMerge() {
        return addOrMerge;
    }

    public boolean added() {
        return added;
    }

    void setAdded() {
        added = true;
    }

    public void setAdd(boolean addOrMerge) {
        this.addOrMerge = addOrMerge;
    }

    @Nullable
    public X merge(X y) {
        return model.merge(y, x);
    }

    public void mergeIdentity() {

    }

    public boolean maybeContainedBy(HyperRegion c) {
        return model.mergeCanStretch() ? c.intersects(bounds) : c.contains(bounds);
    }
}
