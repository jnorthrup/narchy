package jcog.tree.rtree;

import org.jetbrains.annotations.Nullable;

public class RInsertion<X> {

    public final Spatialization<X> model;
    public final X x;

    final HyperRegion bounds;

    private boolean addOrMerge;

    private boolean added, merged;

    /** if merged, whether the merge resulted in stretch (recurse bounds update necessary on return) */
    public boolean stretched = false;

//    private Spatialization<X> space;

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

    public boolean merged() {
        return merged;
    }

    void setAdded() {
        added = true;
    }

    //TODO setMerged() { ...

    public void setAdd(boolean addOrMerge) {
        this.addOrMerge = addOrMerge;
    }

    @Nullable
    public final X merge(X y) {
        X z = model.merge(y, x, this);
        if (z!=null)
            merged = true;
        return z;
    }

    public void mergeEqual(X x) {
        merged = true;
    }

    final boolean maybeContainedBy(HyperRegion c) {
        return model.canMergeStretch() ? c.intersects(bounds) : c.contains(bounds);
    }



//    public final void start(Spatialization<X> t) {
//        this.space = t;
//    }
//    public final void end(Spatialization<X> xrTree) {
//        this.space = null;
//    }
//
//    public final Spatialization<X> space() {
//        return space;
//    }
}
