package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;

import java.util.function.Function;

public class Spatialization<T> {

    public final Split<T> split;
    public final Function<T, HyperRegion> bounds;
    public final short max;       // max entries per node
    public final short min;       // least number of entries per node

    public Spatialization(@Deprecated final Function<T, HyperRegion> bounds, DefaultSplits split, final int min, final int max) {
        this(bounds, split.get(), min, max);
    }

    public Spatialization(@Deprecated final Function<T, HyperRegion> bounds, final Split<T> split, final int min, final int max) {
        this.max = (short) max;
        this.min = (short) min;
        this.bounds = bounds;
        this.split = split;
    }

    public HyperRegion bounds(/*@NotNull*/ T t) {
        return bounds.apply(t);
    }

    public Leaf<T> newLeaf() {
        return new Leaf<>(max);
    }

    public Branch<T> newBranch() {
        return new Branch<>(max);
    }

    public Branch<T> newBranch(Leaf<T> a, Leaf<T> b) {
        return new Branch<>(max, a, b);
    }

    public Node<T, ?> split(T t, Leaf<T> leaf) {
        return split.split(t, leaf, this);
    }

    public double perimeter(T c) {
        return bounds(c).perimeter();
    }

    /** called when add encounters an equivalent (but different) instance */
    protected void merge(T existing, T incoming) {

    }

    public final Leaf<T> transfer(Leaf<T> leaf, HyperRegion[] sortedMbr, int from, int to) {
        Leaf<T> l = newLeaf();
        transfer(leaf, sortedMbr, l, from, to);
        return l;
    }

    public void transfer(Leaf<T> leaf, HyperRegion[] sortedSrc, Node<T, ?> target, int from, int to) {

        boolean[] dummy = new boolean[1];

        for (int j = 0; j < leaf.size; j++) {
            T jd = leaf.data[j];
            HyperRegion jr = bounds(jd);

            for (int i = from; i < to; i++) {
                HyperRegion si = sortedSrc[i];

                if (si!=null && jr.equals(si)) {

                    dummy[0] = false;
                    target.add(jd, leaf, this, dummy);
                    sortedSrc[i] = null;
                    break;
                }
            }
        }
    }


    /**
     * Different methods for splitting nodes in an RTree.
     */
    @Deprecated public enum DefaultSplits {
        AXIAL {
            @Override
            public <T> Split<T> get() {
                return new AxialSplitLeaf<>();
            }
        },
        LINEAR {
            @Override
            public <T> Split<T> get() {
                return new LinearSplitLeaf<>();
            }
        },
        QUADRATIC {
            @Override
            public <T> Split<T> get() {
                return new QuadraticSplitLeaf<>();
            }
        };

        abstract public <T> Split<T> get();

    }
}
